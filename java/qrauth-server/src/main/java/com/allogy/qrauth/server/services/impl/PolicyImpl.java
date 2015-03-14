package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.entities.DBUser;
import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.entities.Username;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.PasswordHelper;
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
		return usernameMatchFilter(username).length() > 4;
	}

	@Override
	public
	String usernameMatchFilter(String userInput)
	{
		final
		int l=userInput.length();

		final
		StringBuilder sb=new StringBuilder();

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
