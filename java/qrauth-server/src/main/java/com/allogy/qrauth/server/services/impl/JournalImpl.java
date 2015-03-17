package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.entities.*;
import com.allogy.qrauth.server.services.AuthSession;
import com.allogy.qrauth.server.services.Journal;
import com.allogy.qrauth.server.services.Network;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.Session;

import java.util.Collection;
import java.util.Date;

/**
 * Created by robert on 2/18/15.
 */
public
class JournalImpl implements Journal
{
	/**
	 * @TODO: make this deferred (off-thread), rather than blocking
	 * @TODO: consider some sort of automatic escalation given (class,#attempts)
	 * @param a - that which was attempted (usually implies a lack of success)
	 */
	@Override
	public
	void noticeAttempt(Attemptable a)
	{
		a.attempts++;
		a.lastAttempt=new Date();

		hibernateSessionManager.getSession().save(a);
		hibernateSessionManager.commit();
	}

	/**
	 * @TODO: make this deferred (off-thread), rather than blocking
	 * @param a - that which was successfully attempted
	 */
	@Override
	public
	void noticeSuccess(Attemptable a)
	{
		a.attempts++;
		a.successes++;
		a.lastAttempt=a.lastSuccess=new Date();

		hibernateSessionManager.getSession().save(a);
		hibernateSessionManager.commit();
	}

	@Override
	public
	void incrementSuccess(Attemptable attemptable)
	{
		attemptable.successes++;
		attemptable.lastSuccess=new Date();

		hibernateSessionManager.getSession().save(attemptable);
		hibernateSessionManager.commit();
	}

	@Override
	public
	void noticeAttempt(Collection<? extends Attemptable> attemptables)
	{
		final
		Date date=new Date();

		final
		Session session=hibernateSessionManager.getSession();

		for (Attemptable a : attemptables)
		{
			a.attempts++;
			a.lastAttempt=new Date();
			session.save(a);
		}

		hibernateSessionManager.commit();
	}

	@Override
	public
	void incrementSuccess(Collection<? extends Attemptable> attemptables)
	{
		final
		Date date=new Date();

		final
		Session session=hibernateSessionManager.getSession();

		for (Attemptable a : attemptables)
		{
			a.successes++;
			a.lastAttempt=a.lastSuccess=new Date();
			session.save(a);
		}

		hibernateSessionManager.commit();
	}

	@Override
	public
	void createdUserAccount(
							   DBUserAuth userAuth, Username username, TenantSession tenantSession
	)
	{
		final
		LogEntry logEntry = new LogEntry();

		logEntry.time = new Date();
		logEntry.actionKey = "user-create";
		logEntry.message = "Created account using "+humanReadable(userAuth.authMethod);
		logEntry.user=userAuth.user;
		logEntry.username=username;
		logEntry.userAuth=userAuth;
		logEntry.tenant=getTenant(tenantSession);
		logEntry.tenantIP=getTenantIP(logEntry.tenant);
		logEntry.tenantSession=tenantSession;
		logEntry.deadline=null;

		hibernateSessionManager.getSession().save(logEntry);
	}

	@Override
	public
	void authenticatedUser(
							  DBUserAuth userAuth, Username username, TenantSession tenantSession, Date deadline
	)
	{
		final
		LogEntry logEntry = new LogEntry();

		logEntry.time = new Date();
		logEntry.actionKey = "user-login";
		logEntry.message = "Authenticated using "+humanReadable(userAuth.authMethod);
		logEntry.user=userAuth.user;
		logEntry.username=username;
		logEntry.userAuth=userAuth;
		logEntry.tenant=getTenant(tenantSession);
		logEntry.tenantIP=getTenantIP(logEntry.tenant);
		logEntry.tenantSession=tenantSession;
		logEntry.deadline=deadline;

		hibernateSessionManager.getSession().save(logEntry);
	}

	@Override
	public
	void updatedUserAuth(DBUserAuth userAuth)
	{
		final
		LogEntry logEntry = new LogEntry();

		logEntry.time = new Date();
		logEntry.actionKey = "userauth-update";
		logEntry.message = "Updated "+humanReadable(userAuth.authMethod);
		logEntry.user=userAuth.user;
		logEntry.username=null;
		logEntry.userAuth=userAuth;
		logEntry.tenant=null;
		logEntry.tenantIP=network.needIPForThisRequest(null);
		logEntry.tenantSession=null;
		logEntry.deadline=null;

		hibernateSessionManager.getSession().save(logEntry);
		hibernateSessionManager.commit();
	}

	@Override
	public
	void revokedUserAuth(DBUserAuth userAuth)
	{
		final
		LogEntry logEntry = new LogEntry();

		logEntry.time = new Date();
		logEntry.actionKey = "userauth-revoked";
		logEntry.message = "Revoked "+humanReadable(userAuth.authMethod);
		logEntry.user=userAuth.user;
		logEntry.username=null;
		logEntry.userAuth=userAuth;
		logEntry.tenant=null;
		logEntry.tenantIP=network.needIPForThisRequest(null);
		logEntry.tenantSession=null;
		logEntry.deadline=null;

		hibernateSessionManager.getSession().save(logEntry);
		hibernateSessionManager.commit();
	}

	@Override
	public
	void transferredUserAuth(DBUserAuth userAuth, DBUser toWhom)
	{
		final
		LogEntry logEntry = new LogEntry();

		logEntry.time = new Date();
		logEntry.actionKey = "userauth-transferred";
		logEntry.message = "Transferred "+humanReadable(userAuth.authMethod)+" to "+toWhom;
		logEntry.user=userAuth.user;
		logEntry.username=null;
		logEntry.userAuth=userAuth;
		logEntry.tenant=null;
		logEntry.tenantIP=network.needIPForThisRequest(null);
		logEntry.tenantSession=null;
		logEntry.deadline=null;

		hibernateSessionManager.getSession().save(logEntry);
		hibernateSessionManager.commit();
	}

	@Override
	public
	void addedUserAuthCredential(DBUserAuth userAuth)
	{
		final
		LogEntry logEntry = new LogEntry();

		logEntry.time = new Date();
		logEntry.actionKey = "userauth-added";
		logEntry.message = "Added "+humanReadable(userAuth.authMethod)+" authentication method";
		logEntry.user=userAuth.user;
		logEntry.username=null;
		logEntry.userAuth=userAuth;
		logEntry.tenant=null;
		logEntry.tenantIP=network.needIPForThisRequest(null);
		logEntry.tenantSession=null;
		logEntry.deadline=null;
		logEntry.important=true; //Adding an auth method is *always* important.

		hibernateSessionManager.getSession().save(logEntry);
		hibernateSessionManager.commit();
	}

	@Inject
	private
	AuthSession authSession;

	@Override
	public
	void allocatedUsername(Username username)
	{
		final
		LogEntry logEntry = new LogEntry();

		logEntry.time = new Date();
		logEntry.actionKey = "username-allocated";
		logEntry.message = "Allocated username: '"+username.displayValue+"'";
		logEntry.user=username.user;
		logEntry.username=username;
		logEntry.userAuth=authSession.getDBUserAuth();
		logEntry.tenant=null;
		logEntry.tenantIP=network.needIPForThisRequest(null);
		logEntry.tenantSession=null;
		logEntry.deadline=null;

		hibernateSessionManager.getSession().save(logEntry);
		hibernateSessionManager.commit();
	}

	@Override
	public
	void revokedUsername(Username username)
	{
		final
		DBUserAuth userAuth=authSession.getDBUserAuth();

		final
		LogEntry logEntry = new LogEntry();

		logEntry.time = new Date();
		logEntry.actionKey = "username-revoked";
		logEntry.message = "Revoked username '"+username.displayValue+"'";
		logEntry.user=userAuth.user;
		logEntry.username=username;
		logEntry.userAuth=userAuth;
		logEntry.tenant=null;
		logEntry.tenantIP=network.needIPForThisRequest(null);
		logEntry.tenantSession=null;
		logEntry.deadline=null;

		hibernateSessionManager.getSession().save(logEntry);
		hibernateSessionManager.commit();

	}

	@Override
	public
	void newTenantAccountCreated(TenantUser tenantUser)
	{
		final
		LogEntry logEntry = new LogEntry();

		logEntry.time = new Date();
		logEntry.actionKey = "tenant-created";
		logEntry.message = "Tenant Account Created";
		logEntry.user=tenantUser.user;
		logEntry.username=authSession.getUsername();
		logEntry.userAuth=authSession.getDBUserAuth();
		logEntry.tenant=tenantUser.tenant;
		logEntry.tenantIP=network.needIPForThisRequest(null);
		logEntry.tenantSession=null;
		logEntry.deadline=null;
		logEntry.important=true;

		hibernateSessionManager.getSession().save(logEntry);
	}

	@Override
	public
	void newTenantUser(TenantUser tenantUser, TenantSession tenantSession, TenantIP tenantIP)
	{
		final
		LogEntry logEntry = new LogEntry();

		logEntry.time = new Date();
		logEntry.actionKey = "tenantuser-created";
		logEntry.message = "First user authentication to "+Config.get().presentableTenantIdentification(tenantUser.tenant);
		logEntry.user=tenantUser.user;
		logEntry.username=tenantSession.username;
		logEntry.userAuth=tenantSession.userAuth;
		logEntry.tenant=tenantUser.tenant;
		logEntry.tenantIP=tenantIP;
		logEntry.tenantSession=tenantSession;
		logEntry.deadline=null;
		logEntry.important=false;

		hibernateSessionManager.getSession().save(logEntry);
	}

	private
	TenantIP getTenantIP(Tenant tenant)
	{
		return network.needIPForThisRequest(tenant);
	}

	@Inject
	private
	Network network;

	private
	String humanReadable(AuthMethod authMethod)
	{
		switch (authMethod)
		{
			case SQRL:
				return "SQRL public key";
			case RSA:
				return "RSA public key";
			case YUBIKEY_CUSTOM:
				return "custom Yubikey token";
			case HMAC_OTP:
				return "H-OTP proof";
			case TIME_OTP:
				return "T-OTP proof";
			case PAPER_PASSWORDS:
				return "PPP password";
			case YUBIKEY_PUBLIC:
				return "public Yubikey token";
			case OPEN_ID:
				return "3rd party OpenID server";
			case STATIC_OTP:
				return "provisioned one-time-password";
			case EMAILED_SECRET:
				return "email reception proof";
			case ROLLING_PASSWORD:
				return "rolling password";
			case STATIC_PASSWORD:
				return "static password";

			default:
				return "unknown";
		}
	}

	private
	Tenant getTenant(TenantSession tenantSession)
	{
		if (tenantSession == null)
		{
			return null;
		}
		else
		{
			return tenantSession.tenant;
		}
	}

	@Inject
	private
	HibernateSessionManager hibernateSessionManager;
}
