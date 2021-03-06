package com.allogy.qrauth.server.pages.internal.auth;

import com.allogy.qrauth.server.entities.*;
import com.allogy.qrauth.server.helpers.*;
import com.allogy.qrauth.server.pages.api.AbstractAPICall;
import com.allogy.qrauth.server.pages.user.ActivityUser;
import com.allogy.qrauth.server.pages.user.ContinueUser;
import com.allogy.qrauth.server.pages.user.names.AddNames;
import com.allogy.qrauth.server.services.AuthSession;
import com.allogy.qrauth.server.services.Hashing;
import com.allogy.qrauth.server.services.Journal;
import com.allogy.qrauth.server.services.Policy;
import com.allogy.qrauth.server.services.impl.Config;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;
import com.yubico.client.v2.exceptions.YubicoVerificationException;
import org.apache.commons.codec.binary.Base64;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.criterion.Restrictions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by robert on 2/20/15.
 */
public
class DispatchAuth extends AbstractAPICall
{
	private
	TenantSession tenantSession;

	@Inject
	private
	Journal journal;

	private
	Collection<TenantIP> allMatchingTenantIPs;

	Object onActivate() throws IOException
	{
		if (!isPostRequest())
		{
			return mustBePostOrPreflightCheck();
		}

		if (log.isDebugEnabled())
		{
			for (String key : request.getParameterNames())
			{
				String value = request.getParameter(key);
				log.debug("parameter: {} -> {}", key, value);
			}
		}

		final
		Tenant tenant;

		final
		String tenantSessionId = request.getParameter("tenantSession");
		{
			if (tenantSessionId == null || tenantSessionId.isEmpty())
			{
				log.debug("no tenant session id");
				tenantSession = null;
				tenant = null;
			}
			else
			{
				tenantSession = (TenantSession)
									session.createCriteria(TenantSession.class)
										.add(Restrictions.eq("session_id", tenantSessionId))
										.uniqueResult()
				;

				/**
				 * !!!: What to do on a failure... if we terminate here, then someone might be able to brute
				 * force (and therefore hijack) a tenant session (if, for example, they have weak session ids).
				 * By simply allowing the tenantSession to be null, we are probably *safer*, as they will
				 * simply fall into the local user maintenance workflows (rather than being authenticated to
				 * the 3rd party client).
				 *
				 * TODO: these cases need more thought!
				 */
				if (tenantSession==null)
				{
					log.warn("attempting to login with invalid (previously-unseen) tenant session_id");
					tenant=null;
				}
				else
				if (Death.hathVisited(tenantSession))
				{
					log.warn("attempting to login with invalid (previously-killed) tenant session_id");
					tenantSession=null;
					tenant=null;
				}
				else
				{
					tenant=tenantSession.tenant;

					final
					String referrer=request.getHeader("Referer");

					if (tenantSession.return_url==null)
					{
						if (referrer==null)
						{
							log.debug("{} return_url={}", tenantSession, referrer);
							tenantSession.return_url=referrer;
							session.save(tenantSession);
						}
						else
						{
							log.debug("{}, referer is null (and without a return_url)", tenantSession);
						}
					}
					else
					{
						log.debug("{} already has a return_url", tenantSession);
					}
				}
			}
		}

		allMatchingTenantIPs = network.getExistingTenantIPsForThisOriginator(tenant);

		//TODO: !!!: we need to check per-tenant ip bans here

		journal.noticeAttempt(allMatchingTenantIPs);

		String username=request.getParameter("otp_username");
		{
			if (username == null || username.isEmpty())
			{
				username = request.getParameter("ppp_username");
			}
		}

		String password=request.getParameter("otp_password");
		{
			if (password==null || password.isEmpty())
			{
				password = request.getParameter("ppp_response");
			}
		}

		if (request.getParameter("do_something") != null)
		{
			log.debug("user probably hit enter within an one-line input field");
			return do_otp_attempt(username, password);
		}
		else
		if (request.getParameter("do_sqrl") != null)
		{
			return do_sqrl();
		}
		else if (request.getParameter("do_otp") != null || request.getParameter("do_ppp_2")!=null)
		{
			return do_otp_attempt(username, password);
		}
		else if (request.getParameter("do_ppp_1")!=null)
		{
			//only relevant for noscript support (which also means provider has this as the default?)
			return new ErrorResponse(500, "noscript ppp is unimplemented");
		}
		else
		if (request.getParameter("do_rsa")!=null)
		{
			return do_rsa_attempt();
		}
		else
		{
			return new ErrorResponse(500, "dispatacher does not implement the requested auth method");
		}
	}

	private
	Object do_otp_attempt(String username, String password)
	{
		final
		ErrorResponse notEnoughInfo=new ErrorResponse(400, "missing username and/or password");

		if (username==null || username.isEmpty())
		{
			if (password==null || password.isEmpty())
			{
				return notEnoughInfo;
			}
			else
			if (mightContainHardwareOTPIdentity(password))
			{
				return do_otp_self_id(password);
			}
			else
			{
				return notEnoughInfo;
			}
		}
		else
		{
			if (password==null || password.isEmpty())
			{
				if (mightContainHardwareOTPIdentity(username))
				{
					return do_otp_self_id(username);
				}
				else
				{
					return notEnoughInfo;
				}
			}
			else
			{
				return do_otp_attempt2(username, password);
			}
		}
	}

	/**
	 * The same as above, except now we know that neither of our arguments are null or empty.
	 *
	 * @param usernameString
	 * @param password
	 * @return
	 */
	@CommitAfter
	private
	Object do_otp_attempt2(String usernameString, String password)
	{
		final
		long startTime=System.currentTimeMillis();

		Username username=getUsername(usernameString);

		if (username==null)
		{
			//Maybe allow the implicit allocation of usernames if the password contains mechanically verifiable identification.
			if (yubikeyOtpValidatesAgainstPublicServer(password))
			{
				final
				String pubKey = "yubico:"+YubicoClient.getPublicId(password);

				DBUserAuth userAuth=locateUserAuthByPublicKey(pubKey);

				if (userAuth==null)
				{
					//It's not a yubikey that we have seen before. Create an account, therefor...
					userAuth=new DBUserAuth();

					userAuth.authMethod=AuthMethod.YUBIKEY_PUBLIC;
					userAuth.pubKey=pubKey;

					createUserWithNewStipulation(userAuth);
				}
				else
				if (UserPolicy.requiresUsername(userAuth) || UserPolicy.requiresEmptyUsername(userAuth))
				{
					//The yubikey is 'taken' by another account, which requires a username, that the caller did not locate.
					//This is roughly equivalent to a classic 'invalid username' situation...
					//But if we were *sure* they were a different user, then we would de-auth the yubikey, right?
					journal.noticeAttempt(userAuth);

					dbTiming.concerning("otp-auth").shorterPath(startTime);

					//TODO: should we obscure the fact that they got the yubikey right?
					return new ErrorResponse(403, "invalid username, and you must submit a fresh OTP too...");
				}
				else
				{
					journal.noticeAttempt(userAuth);
				}

				/*
				(!!!) At this point, we have a valid userAuth/method (but we have have just created it).
				We would authenticate the user here, but we may still be able to do more regarding the
				supplied username.
				 */

				/*
				Facts:
				(1) If they had not supplied a username, they would have authenticated (just via a different logic branch), and
				(2) They *did* supply a username, and
				(3) it is 'available'.

				So it sounds to me like we ought to allocate it for them, at best, it might be a mnemonically-close or
				easily confusable username... it which case, it should be their own.
				 */

				if (policy.wouldAllowUsernameToBeRegistered(usernameString))
				{
					if (policy.wouldAllowAdditionalUsernames(userAuth.user, true))
					{
						//TODO: BUG: username is created, even if user account is disabled (low priority b/c atm there are no disabled accounts?).
						username=new Username();
						username.type = UsernameType.IMPLICIT;
						username.user=userAuth.user;
						username.displayValue=usernameString.trim();
						username.unixValue=policy.usernameUnixFilter(username.type, usernameString);
						username.matchValue=policy.usernameMatchFilter(usernameString);
						session.save(username);

						//TODO: BUG: authentication method is not noted, because authSession[Memo] has not been set
						journal.allocatedUsername(username);
					}
					else
					{
						//TODO: should we place the username in the database without a user to prevent it from being used?
						log.info("too many usernames?");
					}

					return maybeAuthenticateUser(userAuth, username);
				}
				else
				{
					//Create an account (based solely on the yubikey), log them in, and send them to the username selection screen with the provided username as the hint...
					Object standardEgress=maybeAuthenticateUser(userAuth, null);

					if (authSession.isLoggedIn())
					{
						log.info("login using username & yubikey almost allocated username, ignoring '{}' in favor of allocating a username", standardEgress);
						return addNamesPage.withDisplayName(usernameString);
						//TODO: we need to be sure that (if there is a tenantSession) that they eventually make it back to the tenant's page.
					}
					else
					{
						log.debug("valid yubikey login to invalid account");
						return standardEgress;
					}
				}
			}
			else
			{
				/*
				They supplied a username and password, but the username has no existing record... and the
				password is not mechanically verifiable (like a yubikey). So we *COULD* create a new user/pass
				combo (yuck) like a conventional registration page, but that might make someone who simply
				mistyped their username create a bunch of spurious accounts (that they are *very-likely* to
				fall into).
				 */
				return authFailure("bad username/password combo");
			}
		}
		else
		if (username.user==null)
		{
			return authFailure("that username was used by a former member, or is reserved for future use");
		}
		else
		{
			final
			DBUser user=username.user;

			final
			long now = System.currentTimeMillis();

			final
			List<Attemptable> attemptables=new ArrayList<Attemptable>(20);

			//The username matches a real user record, run through all the user's possible otp methods, maybe find one that works.
			for (DBUserAuth userAuth : user.authMethods)
			{
				/*
				NOTICE: we do not check for 'dead' auth methods, so that:
				(1) the actual death message will surface, and
				(2) the user cannot reuse old passwords.
				 */
				final
				AuthMethod authMethod = userAuth.authMethod;

				if (authMethod.usesPasswordEntry())
				{
					attemptables.add(userAuth);

					if (passwordSatisfiesAuthMethod(authMethod, userAuth, password))
					{
						log.debug("provided password satisfies {} {}", authMethod, userAuth);
						return maybeAuthenticateUser(userAuth, username);
					}
				}
			}

			attemptables.add(user);

			//If we did not find a match, count each method (and the user) as having an attempt.
			journal.noticeAttempt(attemptables);

			return authFailure("provided otp does not match any configured auth methods for the given username");
		}
	}

	@Inject
	private
	Hashing hashing;

	/**
	 * @param authMethod
	 * @param userAuth
	 * @param password
	 * @return
	 */
	private
	boolean passwordSatisfiesAuthMethod(AuthMethod authMethod, DBUserAuth userAuth, String password)
	{
		/**
		 * BE VARY CAREFUL WITH THIS METHOD !!!
		 * There is a lot of code and simply returning 'true' results in an authentication!
		 */
		switch (authMethod)
		{
			case STATIC_OTP:
			case STATIC_PASSWORD:
			case ROLLING_PASSWORD:
			{
				try
				{
					return hashing.digestMatch(password, userAuth.secret);
				}
				catch (UnimplementedHashFunctionException e)
				{
					log.error("old version can't verify new digest?", e);
					return false;
				}
			}

			case YUBIKEY_PUBLIC:
			{
				//Try to avoid calling out to Yubico unless (at least) the prefix matches.
				if (yubicoPrefixedPublicIdMatchesSuppliedOtp(userAuth.pubKey, password))
				{
					return yubikeyOtpValidatesAgainstPublicServer(password);
				}
				else
				{
					log.debug("otp does not seem to match prefix: {}", userAuth.pubKey);
					return false;
				}
			}

			case HMAC_OTP:
			{
				/*TODO: HMAC is probably more expensive than the others, so we can incur a performance tradeoff for
				if (HEAVY_LOAD && Death.hathVisited(userAuth))
				{
					return false;
				}
				*/
				//NB: fall through to TIME_OTP !
			}
			case TIME_OTP:
			{
				final
				long now=System.currentTimeMillis();

				return OTPHelper.matchesUserInput(userAuth, now, password, policy.hotpAdvanceMatch());
			}

			case PAPER_PASSWORDS:
			{
				if (password.length()==PPP_Engine.PASSCODE_LENGTH)
				{
					return new PPP_Helper(userAuth).testAndIncrement(password);
				}
				else
				{
					log.debug("wrong password length for PPP");
					return false;
				}
			}

			case YUBIKEY_CUSTOM:
			case EMAILED_SECRET:
				log.error("unimplemented password-based auth method: {}", authMethod);
				return false;

			case SQRL:
			case RSA:
			case OPEN_ID:
				log.error("not a password-based auth method: {}", authMethod);
				return false;

			default:
				log.error("unknown password-based auth method: {}", authMethod);
				return false;
		}
	}

	private
	boolean yubicoPrefixedPublicIdMatchesSuppliedOtp(String publicIdWithPrefix, String withOtpSuffix)
	{
		final
		String prefix="yubico:";

		final
		String publicId=publicIdWithPrefix.substring(prefix.length());

		log.debug("strip prefix: '{}' -> '{}'", publicIdWithPrefix, publicId);
		return withOtpSuffix.startsWith(publicId);
	}

	@InjectPage
	private
	AddNames addNamesPage;

	private
	boolean mightContainHardwareOTPIdentity(String s)
	{
		//Yubikeys tend to start with several c's... until they make a certain number of them, I suppose.
		//return s.startsWith("cc");
		//How about... the are much longer than most people's passwords (and are always the same size)?
		return s.length()==44;
	}

	private
	Object do_otp_self_id(String otp)
	{
		log.debug("do_otp_self_id: {}", otp);

		final
		String publicId = YubicoClient.getPublicId(otp);

		final
		String pubKey = "yubico:" + publicId;

		//NB: we *always* send the request to yubico, as (success or failure) it is important to invalidate the OTP.
		//TODO: If we had asynchronous db fetching, then we could be searching for the key in our database at the same time...
		if (yubikeyOtpValidatesAgainstPublicServer(otp))
		{
			DBUserAuth userAuth = locateUserAuthByPublicKey(pubKey);

			if (userAuth == null)
			{
				//Greetings! Someone is trying to log in using a yubikey that we have never seen. Create an account, therefor.
				userAuth = new DBUserAuth();

				userAuth.authMethod = AuthMethod.YUBIKEY_PUBLIC;
				userAuth.pubKey = pubKey;

				createUserWithNewStipulation(userAuth);
				return maybeAuthenticateUser(userAuth, null);
			}
			else
			{
				return maybeAuthenticateUser(userAuth, null);
			}
		}
		else
		{
			return new ErrorResponse(400, "unable to validate yubikey");
		}
	}

	private
	boolean yubikeyOtpValidatesAgainstPublicServer(String otp)
	{
		final
		YubicoClient yubicoClient = Config.get().getYubicoClient();

		if (yubicoClient == null)
		{
			log.warn("refused yubikey user, because system is missing yubico api key");
			return false;
		}
		else
		{
			try
			{
				return yubicoClient.verify(otp).isOk();
			}
			catch (YubicoVerificationException e)
			{
				e.printStackTrace();
				return false;
			}
			catch (YubicoValidationFailure yubicoValidationFailure)
			{
				yubicoValidationFailure.printStackTrace();
				return false;
			}
		}
	}

	private
	Object do_rsa_attempt() throws IOException
	{
		final
		String pubKeyOrUsername = request.getParameter("rsa_pubkey");
		{
			if (pubKeyOrUsername == null || pubKeyOrUsername.isEmpty())
			{
				return missingParameter("rsa_pubkey");
			}
		}

		final
		String base64Response = stripNonBase64Lines(request.getParameter("rsa_response"));
		{
			if (base64Response == null || base64Response.isEmpty())
			{
				return missingParameter("rsa_response");
			}
		}

		final
		Nut nut = getNut();
		{
			if (nut == null || Death.hathVisited(nut))
			{
				return authFailure(Death.noteMightSay(nut, "nut is expired, consumed, or missing"));
			}
		}

		//TODO: detect if the user transmits a private key in either text box, and (if there is an *existing* account/key tuple) kill that auth method.

		if (pubKeyOrUsername.indexOf(' ') < 0)
		{
			final
			Username username = getUsername(pubKeyOrUsername);

			if (username == null)
			{
				//TODO: !!!: disguise (in both response and timing) revealation of valid/invalid username (in production mode?)
				return new ErrorResponse(400, "invalid username");
			}

			journal.noticeAttempt(username);
			journal.noticeAttempt(username.user);

			final
			byte[] binaryResponse = Base64.decodeBase64(base64Response);

			final
			List<DBUserAuth> pubkeys = session.createCriteria(DBUserAuth.class)
										   .add(Restrictions.eq("user", username.user))
										   .add(Restrictions.eq("authMethod", AuthMethod.RSA))
										   .list();

			final
			long now=System.currentTimeMillis();

			//Try "alive" public keys first...
			for (DBUserAuth rsaUserAuth : pubkeys)
			{
				if (!Death.hathVisited(rsaUserAuth, now))
				{
					journal.noticeAttempt(rsaUserAuth);

					final
					RSAHelper rsaHelper = new RSAHelper(rsaUserAuth);

					try
					{
						if (rsaHelper.signatureIsValid(nut.stringValue, binaryResponse))
						{
							log.debug("rsa login with a username: '{}'", username.matchValue);
							return maybeAuthenticateUser(rsaUserAuth, username);
						}
					}
					finally
					{
						rsaHelper.close();
					}
				}
			}

			//Only then, try dead keys; this separation is important in case two users bat a pubKey back-and-forth, as it will still match one of the dead keys.
			for (DBUserAuth rsaUserAuth : pubkeys)
			{
				if (Death.hathVisited(rsaUserAuth, now))
				{
					journal.noticeAttempt(rsaUserAuth);

					final
					RSAHelper rsaHelper = new RSAHelper(rsaUserAuth);

					try
					{
						if (rsaHelper.signatureIsValid(nut.stringValue, binaryResponse))
						{
							log.debug("rsa login with a username: '{}'", username.matchValue);
							return maybeAuthenticateUser(rsaUserAuth, username);
						}
					}
					finally
					{
						rsaHelper.close();
					}
				}
			}

			return new ErrorResponse(400,
										"that username has no public keys on file that match your provided signature");
		}
		else
		{
			final
			RSAHelper rsaHelper = new RSAHelper(pubKeyOrUsername);

			try
			{
				if (rsaHelper.signatureIsValid(nut.stringValue, Base64.decodeBase64(base64Response)))
				{
					DBUserAuth userAuth = locateUserAuthByPublicKey(rsaHelper.getSshKeyBlob());

					if (userAuth == null)
					{
						log.debug("creating account in reaction to valid (yet unknown) rsa login");
						userAuth = rsaHelper.toDBUserAuth();
						createUserWithNewStipulation(userAuth);
						return maybeAuthenticateUser(userAuth, null);
					}
					else
					{
						log.debug("rsa login without a username");
						journal.noticeAttempt(userAuth);
						journal.noticeAttempt(userAuth.user);
						return maybeAuthenticateUser(userAuth, null);
					}
				}
				else
				{
					return new ErrorResponse(403, "signature problem");
				}
			}
			finally
			{
				rsaHelper.close();
			}
		}
	}

	public static
	String stripNonBase64Lines(String base64)
	{
		if (base64==null || base64.isEmpty())
		{
			return null;
		}

		final
		String[] lines=base64.split("\n");

		final
		StringBuilder sb=new StringBuilder(base64.length());

		for (String line : lines)
		{
			if (containsOnlyBase64Characters(line))
			{
				sb.append(line);
			}
		}

		return sb.toString();
	}

	private static
	boolean containsOnlyBase64Characters(String s)
	{
		//Basically anything that would indicate a command prompt.
		return (s.indexOf('[')<0 &&
					s.indexOf(']')<0 &&
					s.indexOf('$')<0 &&
					s.indexOf('>')<0
		);
	}

	private
	Username getUsername(String username)
	{
		final
		Username retval = (Username)
						session.createCriteria(Username.class)
							.add(Restrictions.eq("matchValue", policy.usernameMatchFilter(username)))
							.uniqueResult()
							;

		if (retval==null)
		{
			usernameScanDetector.usernameNotFound(username, network.getIpAddress());
		}

		return retval;
	}

	private static final
	UsernameScanDetector usernameScanDetector=UsernameScanDetector.get();

	private
	DBUserAuth locateUserAuthByPublicKey(String pubKeyBlob)
	{
		return (DBUserAuth)
				   session.createCriteria(DBUserAuth.class)
					   .add(Restrictions.eq("pubKey", pubKeyBlob))
					   .uniqueResult();
	}

	@Inject
	private
	Policy policy;

	@CommitAfter
	public
	void createUserWithNewStipulation(DBUserAuth userAuth)
	{
		//TODO: handle this better...
		if (userOption("nocreate"))
		{
			throw new RuntimeException("user has opted not to create a new account");
		}

		final
		DBUser user = new DBUser();

		user.globalLogout = new Date(System.currentTimeMillis() + policy.getGlobalLogoutPeriod());
		user.preferencesJson = "{}";
		user.lastLoginIP = getTenantIP();

		userAuth.millisGranted = (int) userAuth.authMethod.getDefaultLoginLength();
		userAuth.user = user;

		session.save(user);
		session.save(userAuth);

		journal.createdUserAccount(userAuth, null, tenantSession);
	}

	private
	TenantIP getTenantIP()
	{
		final
		Tenant tenant;
		{
			if (tenantSession == null)
			{
				tenant = null;
			}
			else
			{
				tenant = tenantSession.tenant;
			}
		}

		return network.needIPForThisRequest(tenant);
	}

	private
	Object maybeAuthenticateUser(DBUserAuth userAuth, Username username)
	{
		log.debug("maybeAuthenticateUser({}, {})", userAuth, username);

		//TODO: this check should be made as soon as we know the user (which is in the userAuth), but then there would be a bunch of little checks to remember scattered around this class... :(
		if (Death.hathVisited(userAuth))
		{
			log.debug("dead: {}", userAuth);
			return new ErrorResponse(403, Death.noteMightSay(userAuth,
																"that authentication method is no longer acceptable"));
		}
		else
		if (userAuth.authMethod==AuthMethod.STATIC_OTP)
		{
			userAuth.deadline=new Date();
			if (userAuth.deathMessage==null)
			{
				userAuth.deathMessage="One-time-password has already been used for authentication.";
			}
			session.save(userAuth);
			journal.revokedUserAuth(userAuth);
		}
		else
		if (userAuth.authMethod==AuthMethod.PAPER_PASSWORDS)
		{
			session.save(userAuth);
		}

		if (username!=null && Death.hathVisited(username))
		{
			log.debug("dead: {}", username);
			return new ErrorResponse(403, Death.noteMightSay(userAuth,
																"that username is no longer acceptable"));
		}

		if (username==null)
		{
			username=userAuth.defaultUsername;

			if (username==null)
			{
				log.debug("no username, and auth method has no default username");
			}
			else
			if (Death.hathVisited(username))
			{
				log.debug("default username for {} is dead: {}", username, userAuth);
				username=null;
			}
			else
			{
				log.debug("{} implies default username: {}", userAuth, username);
			}
		}

		final
		DBUser user = userAuth.user;

		if (Death.hathVisited(user))
		{
			log.debug("dead: {}", user);
			return new ErrorResponse(403, Death.noteMightSay(userAuth, "your user account has been suspended"));
		}

		if (username == null && UserPolicy.requiresUsername(userAuth))
		{
			return new ErrorResponse(403, "username required");
		}

		final
		Nut nut = getNut();
		{
			nut.deadline = new Date();
			//???: this would just serve to fill the database with data that would never be seen...
			//nut.deathMessage = "accepted as nonce for " + userAuth;
			session.save(nut);

			//NB: nut consumption commit() will be wrapped up in the authSession transaction.
		}

		authSession.authenticateRemoteBrowser(userAuth, username, tenantSession, userOption("minimal"));

		if (authSession.endsWithTenantRedirection() && !userOption("maintenance"))
		{
			return continueUser.toNextTenantSessionStep();
		}
		else
		{
			return ActivityUser.class;
		}
	}

	private
	boolean userOption(String expectedValue)
	{
		for (String key : request.getParameterNames())
		{
			if (key.startsWith("option"))
			{
				final
				String thisValue=request.getParameter(key);

				if (thisValue.equals(expectedValue))
				{
					log.debug("user *HAS* selected {} option", expectedValue);
					return true;
				}
			}
		}

		log.debug("user has *NOT* selected {} option", expectedValue);
		return false;
	}

	@InjectPage
	private
	ContinueUser continueUser;

	@Inject
	private
	AuthSession authSession;

	private
	Nut getNut()
	{
		final
		String nutStringValue = request.getParameter("nut");

		if (nutStringValue == null)
		{
			return null;
		}

		return (Nut)
				   session.createCriteria(Nut.class)
					   .add(Restrictions.eq("stringValue", nutStringValue))
					   .uniqueResult();
	}

	private
	ErrorResponse authFailure(String message)
	{
		//TODO: this needs to account for both local & with-tenant cases.
		/*
		By the plain reading of the spec, 403 is not technically correct because it implies 'authorization will not help';
		yet 401 (unauthorized) is not correct because we must then provide basic http authentication. Hmm...
		 */
		return new ErrorResponse(403, message);
	}

	@CommitAfter
	private
	Object do_sqrl()
	{
		final
		Nut nut;
		{
			final
			String secret=request.getParameter("nut_secret");

			if (secret==null)
			{
				return missingParameter("nut_secret");
			}

			nut=getNut();

			if (nut==null)
			{
				return missingOrInvalidParameter("nut");
			}

			if (!nut.semiSecretValue.equals(secret))
			{
				return invalidParameter("nut_secret");
			}
		}

		final
		NutState nutState=nut.getState();

		if (nutState==NutState.READY)
		{
			log.debug("finally consuming {}", nut);
			nut.deadline=new Date();
			nut.deathMessage=null;
			session.save(nut);
		}
		else
		if (Death.hathVisited(nut))
		{
			return new ErrorResponse(400, Death.noteMightSay(nut, "nut is no longer valid"));
		}
		else
		{
			nut.deadline=new Date();
			nut.deathMessage="sqrl protocol anachronism, you will probably need to refresh the login page and try again; beware of fake web pages";
			session.save(nut);
			return new ErrorResponse(400, nut.deathMessage);
		}

		return maybeAuthenticateUser(nut.userAuth, null);
	}
}
