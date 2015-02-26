package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.entities.*;
import com.allogy.qrauth.server.services.Journal;
import com.allogy.qrauth.server.services.Network;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
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
				return "custom Yubikey factors";
			case HMAC_OTP:
				return "H-OTP proof";
			case TIME_OTP:
				return "T-OTP proof";
			case PAPER_PASSWORDS:
				return "PPP password";
			case YUBIKEY_PUBLIC:
				return "public Yubikey verification";
			case OPEN_ID:
				return "3rd party OpenID server";
			case STATIC_OTP:
				return "provisioned one-time-password";
			case EMAILED_SECRET:
				return "email reception proof";
			case SALTED_PASSWORD:
				return "static password";

			default:
				return "unknown method";
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
