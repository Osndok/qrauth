package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.entities.Attemptable;
import com.allogy.qrauth.server.services.Journal;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;

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

	@Inject
	private
	HibernateSessionManager hibernateSessionManager;
}
