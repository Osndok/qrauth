package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.entities.DBUser;
import com.allogy.qrauth.server.entities.Username;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.PasswordHelper;
import com.allogy.qrauth.server.helpers.Redux;
import com.allogy.qrauth.server.services.Policy;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.cron.IntervalSchedule;
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor;
import org.apache.tapestry5.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by robert on 2/18/15.
 */
public
class PolicyImpl implements Policy, Runnable
{
	private static final Long   SUPREME_TENANT_ID       = Config.get().getSupremeTenantID();
	private static final long   UPDATE_PERIOD_MILLIS    = TimeUnit.MINUTES.toMillis(5);
	private static final Logger log                     = LoggerFactory.getLogger(Policy.class);
	private static final long   GLOBAL_LOGOUT_PERIOD    = TimeUnit.DAYS.toMillis(7);
	private static final long   DEVEL_LOGOUT_PERIOD     = TimeUnit.HOURS.toMillis(1);
	private static final long   SHORTEST_USABLE_SESSION = TimeUnit.MINUTES.toMillis(3);
	private static final long   ADD_CREDENTIAL_TIMEOUT  = TimeUnit.MINUTES.toMillis(30);
	private static final String UNIX_EMAIL_PREFIX       = "email_";
	private static final String UNIX_TELEPHONE_PREFIX   = "tele_";

	private
	JSONObject supremeTenantConfig = new JSONObject();

	@Inject
	@Symbol(SymbolConstants.PRODUCTION_MODE)
	private
	boolean productionMode;

	@PostInjection
	public
	void serviceStarts(PeriodicExecutor periodicExecutor)
	{
		if (SUPREME_TENANT_ID == null)
		{
			log.error("no supreme tenant id is set... using static default policies *only*");
		}
		else
		{
			periodicExecutor.addJob(new IntervalSchedule(UPDATE_PERIOD_MILLIS),
									   "policy-updater",
									   this);
		}
	}

	@Override
	public
	boolean allowsAnonymousCreationOfNewTenants()
	{
		return bool("allowNewTenants", true);
	}

	@Override
	public
	long getGlobalLogoutPeriod()
	{
		if (productionMode)
		{
			return GLOBAL_LOGOUT_PERIOD;
		}
		else
		{
			return DEVEL_LOGOUT_PERIOD;
		}
	}

	@Override
	public
	long getShortestUsableSessionLength()
	{
		return SHORTEST_USABLE_SESSION;
	}

	@Override
	public
	boolean wouldAllowUsernameToBeRegistered(String username)
	{
		//TODO: !!!: prevent common unix-system usernames and group names from being registered as usernames
		//TODO: prevent common names (e.g. "john") from being registered as usernames
		//TODO: prevent dictionary words from being registered as usernames
		//TODO: prevent usernames that contain dirty words from being registered
		//TODO: maybe restrict to usernames to unix-style names
		//TODO: for whole-string username restrictions, use a bloom filter that is created at compile time
		final
		String matchable=usernameMatchFilter(username);

		if (matchable.length() < 4)
		{
			return false;
		}

		//Email addresses (although they use the username mechanism) cannot be registered as usernames.
		if (matchable.indexOf('@')>=0)
		{
			return false;
		}

		if (mightIndicateAuthority(matchable))
		{
			return false;
		}

		/*
		Technically no effect, as '_' is stripped out, but this will prevent people from creating usernames
		testing this vector, and provide a hair of future-proofing in case '_' is allowed in the future.
		*/
		if (matchable.startsWith(UNIX_EMAIL_PREFIX) || matchable.startsWith(UNIX_TELEPHONE_PREFIX))
		{
			return false;
		}

		//NB: must begin with a non-number to not trip the phone number detector.
		return Character.isAlphabetic(matchable.charAt(0));
	}

	private
	boolean mightIndicateAuthority(String matchable)
	{
		return matchable.contains("master"    ) || matchable.contains("admin"    )
			|| matchable.contains("authority" ) || matchable.contains("confirm"  )
			|| matchable.contains("usenet"    ) || matchable.contains("official" )
			;
	}

	@Override
	public
	String usernameMatchFilter(String userInput)
	{
		if (looksLikeEmailAddress(userInput))
		{
			return emailAddressMatchFilter(userInput);
		}
		else
		if (looksLikeRegisterablePhoneNumber(userInput))
		{
			return phoneNumberMatchFilter(userInput);
		}
		else
		{
			return Redux.digest(userInput, null);
		}
	}

	//TODO: if made 'public' for other uses, this should basically be a repeat of the looksLikeEmailAddress() logic
	private
	String emailAddressMatchFilter(String userInput)
	{
		return userInput.trim().toLowerCase();
	}

	/**
	 * NB: must begin with a number to jive with username matching.
	 *
	 * @param userInput
	 * @return
	 */
	private
	String phoneNumberMatchFilter(String userInput)
	{
		final
		StringBuilder sb=new StringBuilder();

		final
		int l=userInput.length();

		for (int i=0; i<l; i++)
		{
			final
			char c = userInput.charAt(i);

			if (Character.isDigit(c))
			{
				sb.append(c);
			}
		}

		return sb.toString();
	}

	private
	boolean looksLikeRegisterablePhoneNumber(String userInput)
	{
		final
		int l = userInput.length();

		//Don't match 911, et al.
		if ( l < 5 || l > 50 )
		{
			return false;
		}

		final
		StringBuilder digits=new StringBuilder();

		int numDigits = 0;

		for (int i=0; i<l; i++)
		{
			final
			char c = userInput.charAt(i);

			if (Character.isDigit(c))
			{
				numDigits++;
				digits.append(c);
			}
		}

		//Don't match special service numbers (311,411,911)
		if (numDigits < 7 || numDigits > 31 )
		{
			return false;
		}

		final
		String digitsString=digits.toString();

		/*
		Special case, "long" emergency phone numbers
		Simplified by the above <7 check...
		//@url: http://en.wikipedia.org/wiki/Emergency_telephone_number
		if (numDigits==5 && ( digitsString.equals("10111") || digitsString.equals("10177") ) )
		{
			return false;
		}
		if (digitsString.equals("1122") || digitsString.equals("1669"))
		{
			return false;
		}
		*/
		if (digitsString.equals("9555555"))
		{
			return false;
		}

		//TODO: detect (or reserve in advance) "our own" phone number (used to send and receive text messages)
		//TODO: detect reserved fictitious phone number blocks?
		//@url: http://en.wikipedia.org/wiki/Fictitious_telephone_number
		return true;
	}

	/**
	 * This is not intended to actually test the  validity of an email addresses, but only
	 * to judge the likelihood of a user-provided string as *trying* to be an email address
	 * in a linear execution time.
	 *
	 * Examples of email addresses that are valid but not matched by this algorithm can be
	 * seen here:
	 * http://en.wikipedia.org/wiki/Email_address#Valid_email_addresses
	 *
	 * @param userInput
	 * @return
	 */
	public static
	boolean looksLikeEmailAddress(String userInput)
	{
		final
		int l = userInput.length();

		//"a@b.ly".length()==6
		if (l<6 || l>254)
		{
			return false;
		}

		boolean foundAtSign=false;

		char lastChar=0;

		for (int i=0; i<l; i++)
		{
			final
			char c = userInput.charAt(i);

			if (c=='@')
			{
				if (foundAtSign || i==0)
				{
					return false;
				}
				else
				{
					foundAtSign=true;
				}
			}
			else
			if (foundAtSign)
			{
				if (!commonEmailDomainCharacter(c, lastChar))
				{
					log.debug("not a common email-domain character: {}", c);
					return false;
				}
			}
			else
			{
				if (!commonEmailUsernameCharacter(c, lastChar))
				{
					log.debug("not a common email-domain character: {}", c);
					return false;
				}
			}

			lastChar=c;
		}

		return foundAtSign && lastChar!='@';
	}

	private static
	boolean commonEmailUsernameCharacter(char c, char lastChar)
	{
		return Character.isAlphabetic(c) || Character.isDigit(c) || c=='.' || c=='-' || c=='+';
	}

	private static
	boolean commonEmailDomainCharacter(char c, char lastChar)
	{
		return Character.isAlphabetic(c) || Character.isDigit(c) || c=='.' || c=='-';
	}

	@Override
	public
	String usernameUnixFilter(String userInput)
	{
		if (userInput==null)
		{
			return null;
		}

		final
		int l=userInput.length();

		if (l==0)
		{
			return "empty_string";
		}

		final
		StringBuilder sb=new StringBuilder();

		if (looksLikeEmailAddress(userInput))
		{
			sb.append(UNIX_EMAIL_PREFIX);
		}
		else
		if (looksLikeRegisterablePhoneNumber(userInput))
		{
			sb.append(UNIX_TELEPHONE_PREFIX);
		}

		for (int i=0; i<l; i++)
		{
			final
			char c=userInput.charAt(i);

			if ((c>='a' && c<='z') || (c>='0' && c<='9'))
			{
				sb.append(c);
			}
			else
			if (c>='A' && c<='Z')
			{
				sb.append(Character.toLowerCase(c));
			}
		}

		return sb.toString();
	}

	/**
	 * In testing, this is the amount of time it took for a user to read the 'not now' text and refresh the page.
	 */
	private static final long ADDITIONAL_USERNAME_COOLDOWN = TimeUnit.SECONDS.toMillis(6);

	@Override
	public
	boolean wouldAllowAdditionalUsernames(DBUser user, boolean extraEffort)
	{
		if (user.usernames == null)
		{
			//a primitive hibernate object (new user).
			return true;
		}

		int numAlive = 0;
		int numDead = 0;
		Username mostRecent = null;

		for (Username username : user.usernames)
		{
			if (Death.hathVisited(username))
			{
				numDead++;
			}
			else
			{
				numAlive++;
			}

			if (mostRecent == null || username.created.before(mostRecent.created))
			{
				mostRecent = username;
			}
		}

		if (mostRecent != null && System.currentTimeMillis() < mostRecent.created.getTime() + ADDITIONAL_USERNAME_COOLDOWN)
		{
			log.debug("in username-creation cooldown period: {} alive, {} dead, recent={}", numAlive, numDead, mostRecent);
			return false;
		}

		final
		int total=numAlive+numDead;

		//TODO: count usernames based on ip match too?

		if (extraEffort)
		{
			return numAlive < 15;
		}
		else
		{
			return total < 15;
		}
	}

	private static final long MS_PER_DAY=TimeUnit.DAYS.toMillis(1);

	/**
	 * @param strength - a double to prevent integer division and make the function more readable.
	 * @return
	 */
	public
	Date passwordDeadlineGivenComplexity(double strength)
	{
		double days = strength / PasswordHelper.DICTIONARY_WORD_STRENGTH;

		if (days<1)
		{
			return new Date(System.currentTimeMillis()+MS_PER_DAY);
		}
		else
		if (days>30)
		{
			return new Date(System.currentTimeMillis()+30*MS_PER_DAY);
		}
		else
		{
			return new Date(System.currentTimeMillis()+(long)(days*MS_PER_DAY));
		}
	}

	@Override
	public
	long longestReasonableAddCredentialTaskLength()
	{
		return ADD_CREDENTIAL_TIMEOUT;
	}

	/**
	 * @return the number of times an OTP token can be independently activated and still work
	 */
	@Override
	public
	int hotpAdvanceMatch()
	{
		return 25;
	}

	/**
	 * We certainly want to impose some contemporanious check regarding the sqrl handoff, because there is
	 * a window of time where someone else (in theory) might grab the session (less likely now with the nut's
	 * semi-secret value).
	 *
	 * Also, since SQRL is still vulnerable to MITM/phishing attacks, and the vast majority of SQRL handoffs
	 * are less than two seconds, this *might* be an indication of a MITM... because the intermediate processing
	 * (plus possibly going into another country, having a poor server setup, or having to re-dispatch the
	 * session takeover to a botnet with covert transmission delays to hide their ip address) would show up
	 * as semi-usable signal here as handoff lag time.
	 *
	 * The only complicating fact is that some users might not be using javascript, in which case the handoff
	 * can be as long as it takes them to click the submit button.
	 */
	@Override
	public
	long getMaximumSqrlHandoffPeriod()
	{
		return 30000;
	}

	@Override
	public
	int getMaximumTenantsForUser(DBUser user)
	{
		return 20;
	}

	@Override
	public
	boolean isAcceptableTenantName(String name)
	{
		return wouldAllowUsernameToBeRegistered(name);
	}

	private
	boolean bool(String key, boolean _default)
	{
		final
		Object o = supremeTenantConfig.opt(key);

		if (o instanceof Boolean)
		{
			return ((Boolean) o).booleanValue();
		}
		else
		{
			return _default;
		}
	}

	@Override
	public
	void run()
	{
		//TODO: fetch and decode the supreme tenant's config field
		log.warn("unimplemented");
	}
}
