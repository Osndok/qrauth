package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.entities.*;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.services.*;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.*;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.Date;

/**
 * Created by robert on 2/25/15.
 */
public
class AuthSessionImpl implements AuthSession
{
	private static final String QRAUTH_COOKIE_NAME = Config.get().getCookieName();
	private static final String COOKIE_DOMAIN      = Config.get().getCookieDomain();
	private static final String COOKIE_PATH        = Config.get().getCookiePath();

	private static final Logger log = LoggerFactory.getLogger(AuthSession.class);

	public
	boolean isLoggedIn()
	{
		return environment.peek(AuthSessionMemo.class) != null;
	}

	@Override
	public
	DBUser getDBUser()
	{
		final
		DBUserAuth dbUserAuth = getDBUserAuth();

		if (dbUserAuth == null)
		{
			return null;
		}
		else
		{
			return dbUserAuth.user;
		}
	}

	@Override
	public
	DBUserAuth getDBUserAuth()
	{
		final
		AuthSessionMemo authSessionMemo = environment.peek(AuthSessionMemo.class);

		if (authSessionMemo == null)
		{
			return null;
		}

		if (authSessionMemo.dbUserAuth == null)
		{
			final
			Session session = hibernateSessionManager.getSession();

			authSessionMemo.dbUserAuth = (DBUserAuth) session.get(DBUserAuth.class, authSessionMemo.userAuthId);
		}

		return authSessionMemo.dbUserAuth;
	}

	@Override
	public
	long timeLeft()
	{
		final
		AuthSessionMemo authSessionMemo=environment.peek(AuthSessionMemo.class);

		if (authSessionMemo==null)
		{
			return -1;
		}

		final
		long deadline=authSessionMemo.deadline;

		final
		long now=System.currentTimeMillis();

		return (deadline-now);
	}

	@Override
	public
	void discardAuthenticationCookie()
	{
		final
		AuthSessionMemo authSessionMemo=environment.peek(AuthSessionMemo.class);

		if (authSessionMemo==null)
		{
			//Well... we don't know who is even requesting the logout.
			cookies.removeCookieValue(QRAUTH_COOKIE_NAME);
			return;
		}

		final
		DBUserAuth userAuth;
		{
			userAuth = getDBUserAuth();

			if (userAuth==null)
			{
				//Well... we don't know who is even requesting the logout.
				cookies.removeCookieValue(QRAUTH_COOKIE_NAME);
				log.warn("authSessionMemo {} without userAuth?", authSessionMemo);
				return;
			}
		}

		final
		Session session = hibernateSessionManager.getSession();

		final
		TenantSession tenantSession;
		{
			if (authSessionMemo.tenantSessionId==null)
			{
				tenantSession=null;
			}
			else
			{
				tenantSession = (TenantSession) session.get(TenantSession.class, authSessionMemo.tenantSessionId);
			}
		}

		final
		Tenant tenant;
		{
			if (tenantSession==null)
			{
				tenant=null;
			}
			else
			{
				tenant=tenantSession.tenant;
			}
		}

		final
		LogEntry logEntry = new LogEntry();

		logEntry.time = new Date();
		logEntry.actionKey = "logout";
		logEntry.message = "Logout requested";
		logEntry.username=getUsername(session, authSessionMemo);
		logEntry.userAuth=userAuth;
		logEntry.user=userAuth.user;
		logEntry.tenant=tenant;
		logEntry.tenantIP=network.needIPForThisRequest(tenant);
		logEntry.tenantSession=tenantSession;
		logEntry.deadline=new Date(authSessionMemo.deadline);
		session.save(logEntry);

		hibernateSessionManager.commit();

		cookies.removeCookieValue(QRAUTH_COOKIE_NAME);
	}

	private
	Username getUsername(Session session, AuthSessionMemo authSessionMemo)
	{
		final
		Long id=authSessionMemo.userNameId;

		if (id==null)
		{
			return null;
		}
		else
		{
			return (Username) session.get(Username.class, id);
		}
	}

	@Inject
	private
	Policy policy;

	@Inject
	private
	Network network;

	@Override
	public
	void authenticateRemoteBrowser(
									  DBUserAuth userAuth,
									  Username username,
									  TenantSession tenantSession,
									  boolean minimalSessionLength
	)
	{
		log.debug("authenticateRemoteBrowser({}, {}, {})", userAuth, username, tenantSession);

		final
		Session session = hibernateSessionManager.getSession();

		final
		DBUser user = userAuth.user;

		//NB: Try and place db fetches above this, to maximize the 'now' time value.

		final
		long now = System.currentTimeMillis();

		final
		Date nowDate = new Date(now);

		if (user.globalLogout.getTime() < now + policy.getShortestUsableSessionLength())
		{
			log.debug("advancing user's globalLogout");
			user.globalLogout = new Date(now + policy.getGlobalLogoutPeriod());
			user.epoch++;
		}

		final
		Date sessionDeadline;
		{
			if (minimalSessionLength)
			{
				sessionDeadline=new Date(now + policy.getShortestUsableSessionLength());
			}
			else
			if (userAuth.millisGranted == null)
			{
				log.debug("new session (from unbounded auth method) hits global logout time");
				sessionDeadline=user.globalLogout;
			}
			else
			{
				sessionDeadline=theSoonerOf(user.globalLogout, new Date(now + userAuth.millisGranted));
			}
		}

		log.debug("session timings:\nnow={}\nnowDate={}\nglobalLogout={}\nmillisGranted={}\nsessionDeadline={}", now, nowDate, user.globalLogout, userAuth.millisGranted, sessionDeadline);

		user.attempts++;
		user.successes++;
		user.lastAttempt = nowDate;
		user.lastSuccess = nowDate;
		session.save(user);

		userAuth.attempts++;
		userAuth.successes++;
		userAuth.lastAttempt = nowDate;
		userAuth.lastSuccess = nowDate;
		session.save(userAuth);

		if (username != null)
		{
			username.attempts++;
			username.successes++;
			username.lastAttempt = nowDate;
			username.lastSuccess = nowDate;
			session.save(username);
		}

		if (tenantSession!=null)
		{
			tenantSession.user=user;
			tenantSession.username=username;
			tenantSession.userAuth=userAuth;
			tenantSession.deadline = sessionDeadline;

			//TODO: BUG: "connected" field seems to be getting the sessionDeadline value?!?!?
			tenantSession.connected = nowDate;

			if (tenantSession.return_url==null)
			{
				log.debug("late notice of tenantSession.return_url");
				tenantSession.return_url=requestGlobals.getRequest().getHeader("Referer");
			}

			session.save(tenantSession);
		}

		journal.authenticatedUser(userAuth, username, tenantSession, sessionDeadline);

		hibernateSessionManager.commit();

		//---------------------------------

		final
		AuthSessionMemo authSessionMemo=new AuthSessionMemo(userAuth, username, tenantSession, sessionDeadline);

		while (environment.peek(AuthSessionMemo.class)!=null)
		{
			environment.pop(AuthSessionMemo.class);
		}

		environment.push(AuthSessionMemo.class, authSessionMemo);

		final
		long cookieDeadline;
		{
			if (productionMode)
			{
				cookieDeadline=sessionDeadline.getTime();
			}
			else
			{
				//In development mode, it might be nice to see the stale cookies that will be edge-cases in production,
				//at least to be sure they are handled smoothly and correctly...
				cookieDeadline=sessionDeadline.getTime()+policy.getGlobalLogoutPeriod();
			}
		}

		//TODO: can we set http-only (no javascript access) by this method?
		cookies.getBuilder(QRAUTH_COOKIE_NAME, hashing.withHmacPrefix(authSessionMemo.toString()))
			.setDomain(COOKIE_DOMAIN)
			.setMaxAge(secondsUntil(now, cookieDeadline))
			.setPath(COOKIE_PATH)
			.setSecure(productionMode)
			.write();
	}

	@Override
	public
	Username getUsername()
	{
		final
		AuthSessionMemo authSessionMemo=environment.peek(AuthSessionMemo.class);

		if (authSessionMemo==null)
		{
			log.debug("no AuthSessionMemo, so no username");
			return null;
		}

		final
		Session session = hibernateSessionManager.getSession();

		return getUsername(session, authSessionMemo);
	}

	@Override
	public
	boolean endsWithTenantRedirection()
	{
		final
		AuthSessionMemo authSessionMemo=environment.peek(AuthSessionMemo.class);

		if (authSessionMemo==null)
		{
			log.debug("no AuthSessionMemo, so no tenant redirect");
			return false;
		}
		else
		{
			return (authSessionMemo.tenantSessionId!=null);
		}
	}

	@Override
	public
	TenantSession getTenantSession()
	{
		final
		AuthSessionMemo authSessionMemo=environment.peek(AuthSessionMemo.class);

		if (authSessionMemo==null)
		{
			log.debug("no AuthSessionMemo, so no TenantSession");
			return null;
		}
		else
		if (authSessionMemo.tenantSessionId==null)
		{
			log.debug("tenantSessionId is null");
			return null;
		}
		else
		{
			return fetchTenantSession(authSessionMemo.tenantSessionId);
		}
	}

	private
	TenantSession fetchTenantSession(Long tenantSessionId)
	{
		return (TenantSession)
			hibernateSessionManager.getSession()
			.get(TenantSession.class, tenantSessionId)
			;
	}

	@Inject
	private
	Journal journal;

	private
	int secondsUntil(long now, long then)
	{
		final
		long delta = then - now;

		return (int) (delta / 1000);
	}

	@Inject
	@Symbol(SymbolConstants.PRODUCTION_MODE)
	private
	boolean productionMode;

	@Inject
	private
	Cookies cookies;

	private
	Date theSoonerOf(Date a, Date b)
	{
		if (a.before(b))
		{
			return a;
		}
		else
		{
			return b;
		}
	}

	@Inject
	private
	RequestGlobals requestGlobals;

	@Override
	public
	boolean dispatch(Request request, Response response) throws IOException
	{
		try
		{
			for (Cookie cookie : requestGlobals.getHTTPServletRequest().getCookies())
			{
				if (QRAUTH_COOKIE_NAME.equals(cookie.getName()))
				{
					loadAuthenticationCookie(cookie);
					break;
				}
			}
		}
		catch (UnimplementedHashFunctionException e)
		{
			log.error("mismatched cluster versions or bad user-provided cookie input", e);
			response.sendError(500, "unsupported hash function");
			return true;
		}

		//TODO: this might be a good spot to check for protected page tags, nicer redirection, or whatnot.

		return false;
	}

	@Inject
	private
	Hashing hashing;

	@Inject
	private
	HibernateSessionManager hibernateSessionManager;

	@Inject
	private
	Environment environment;

	/**
	 * @param cookie must be treated as *USER-INPUT*... because it *IS*.
	 */
	private
	void loadAuthenticationCookie(Cookie cookie) throws UnimplementedHashFunctionException
	{
		final
		String withHmacPrefix = cookie.getValue();

		final
		String withoutHmacPrefix = hashing.fromHmacPrefixed(withHmacPrefix);

		if (withoutHmacPrefix == null)
		{
			log.debug("failed hmac test: {}", withHmacPrefix);
		}
		else
		{
			final
			AuthSessionMemo authSessionMemo = new AuthSessionMemo(withoutHmacPrefix);

			if (Death.hathVisited(authSessionMemo))
			{
				log.debug("ignoring expired session cookie-token");
			}
			else
			{
				log.debug("valid session cookie-token: {}", authSessionMemo);
				environment.push(AuthSessionMemo.class, authSessionMemo);

				final
				Response response=requestGlobals.getResponse();

				//TODO: while meant for logging, this might expose the user::id to tenants, is that okay?
				response.setHeader("AI-User-ID", "u"+authSessionMemo.userId);

				if (authSessionMemo.tenantId!=null)
				{
					response.setHeader("AI-Tenant-ID", "t"+authSessionMemo.tenantId);
				}
			}
		}
	}
}
