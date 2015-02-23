package com.allogy.qrauth.server.pages.internal.auth;

import com.allogy.qrauth.server.entities.*;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.helpers.RSAHelper;
import com.allogy.qrauth.server.pages.api.AbstractAPICall;
import com.allogy.qrauth.server.services.Journal;
import com.allogy.qrauth.server.services.Policy;
import org.apache.commons.codec.binary.Base64;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.criterion.Restrictions;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

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
			if (tenantSessionId == null)
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
				}
			}
		}

		allMatchingTenantIPs = network.getExistingTenantIPsForThisOriginator(tenant);

		journal.noticeAttempt(allMatchingTenantIPs);

		if (request.getParameter("do_sqrl") != null)
		{
			//only relevant for noscript support (a button appears for noscript), we only need to check to see if the
			//session is connected, and issue a redirect.
			return new ErrorResponse(500, "noscript sqrl is unimplemented");
		}
		else if (request.getParameter("do_otp") != null)
		{
			return do_otp_attempt();
		}
		else if (request.getParameter("do_ppp_1")!=null)
		{
			//only relevant for noscript support (which also means provider has this as the default?)
			return new ErrorResponse(500, "noscript ppp is unimplemented");
		}
		else
		if (request.getParameter("do_ppp_2")!=null)
		{
			return do_ppp_attempt();
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
	Object do_otp_attempt()
	{
		return new ErrorResponse(500, "otp unimplemented");
	}

	private
	Object do_ppp_attempt()
	{
		return new ErrorResponse(500, "ppp unimplemented");
	}

	private
	Object do_rsa_attempt() throws IOException
	{
		final
		String pubKeyOrUsername=request.getParameter("rsa_pubkey");
		{
			if (pubKeyOrUsername == null)
			{
				return missingParameter("rsa_pubkey");
			}
		}

		final
		String base64Response=request.getParameter("rsa_response");
		{
			if (base64Response==null)
			{
				return missingParameter("rsa_response");
			}

			//TODO: detect (and strip) common command prompts (or... lines with non-base64 characters?).
		}

		final
		Nut nut=getNut();
		{
			if (nut==null || Death.hathVisited(nut))
			{
				return authFailure(Death.noteMightSay(nut, "nut is expired, consumed, or missing"));
			}
		}

		//TODO: detect if the user transmits a private key in either text box, and (if there is an *existing* account/key tuple) kill that auth method.

		if (pubKeyOrUsername.indexOf(' ')<0)
		{
			final
			String username=pubKeyOrUsername;

			//Lookup (and test against) all the user's *active* rsa keys

			//return do_rsa_any_pubkey(user, pubkeys);
			return new ErrorResponse(500, "unimplemented; rsa to existing username");
		}
		else
		{
			final
			RSAHelper rsaHelper=new RSAHelper(pubKeyOrUsername);

			try
			{
				if (rsaHelper.signatureIsValid(nut.stringValue, Base64.decodeBase64(base64Response)))
				{
					DBUserAuth userAuth=locateUserAuthByPublicKey(rsaHelper.getSshKeyBlob());

					if (userAuth==null)
					{
						log.debug("creating account in reaction to valid (yet unknown) rsa login");
						userAuth=rsaHelper.toDBUserAuth();
						createUserWithNewStipulation(userAuth);
						return maybeAuthenticateUser(userAuth);
					}
					else
					{
						//TODO: MULTI-FACTOR: give the user the option of *requiring* a username, in this path they only used a keypair here.
						log.debug("rsa login without a username");
						return maybeAuthenticateUser(userAuth);
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
	private
	void createUserWithNewStipulation(DBUserAuth userAuth)
	{
		final
		DBUser user = new DBUser();

		user.globalLogout = new Date(System.currentTimeMillis() + policy.getGlobalLogoutPeriod());
		user.preferencesJson = "{}";
		user.lastLoginIP = getTenantIP();

		userAuth.millisGranted = (int) userAuth.authMethod.getDefaultLoginLength();
		userAuth.user = user;

		session.save(user);
		session.save(userAuth);
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
	Object maybeAuthenticateUser(DBUserAuth userAuth)
	{
		if (Death.hathVisited(userAuth))
		{
			return new ErrorResponse(403, Death.noteMightSay(userAuth,
																"that authentication method is no longer allowed"));
		}

		journal.incrementSuccess(allMatchingTenantIPs);

		final
		DBUser user = userAuth.user;

		if (Death.hathVisited(user))
		{
			return new ErrorResponse(403, Death.noteMightSay(userAuth, "your user account has been suspended"));
		}

		//TODO: !!!: consume the nut (maybe combine with creation transaction, above? or update below?)
		//TODO: !!!: set cookie
		//TODO: !!!: where do we send them?!?!
		//TODO: if globalLogout deadline is passed, set it to now plus the policy duration

		return new ErrorResponse(500, "trying; authenticate as an existing user");
	}

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
}
