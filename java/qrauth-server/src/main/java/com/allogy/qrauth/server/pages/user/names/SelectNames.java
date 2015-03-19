package com.allogy.qrauth.server.pages.user.names;

import com.allogy.qrauth.server.entities.DBUser;
import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.entities.TenantSession;
import com.allogy.qrauth.server.entities.Username;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.pages.user.AbstractUserPage;
import com.allogy.qrauth.server.pages.user.ContinueUser;
import com.allogy.qrauth.server.services.Policy;
import com.allogy.qrauth.server.services.impl.Config;
import org.apache.tapestry5.Block;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.hibernate.HibernateGridDataSource;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;

import java.util.List;

/**
 * Created by robert on 3/19/15.
 */
public
class SelectNames extends AbstractUserPage
{
	@Property
	private
	Username username;

	@Property
	private
	List<Username> usernames;

	private
	void onActivate()
	{
		usernames=session.createCriteria(Username.class)
			.add(Restrictions.eq("user", user))
			//TODO: null check is not the same as a deadline check
			.add(Restrictions.isNull("deadline"))
			.list()
		;
	}

	@Inject
	private
	Logger log;

	@Inject
	private
	Policy policy;

	public
	String getTenant()
	{
		final
		TenantSession tenantSession = authSession.getTenantSession();

		if (tenantSession == null)
		{
			log.warn("SelectNames page without a tenantSession?");
			return null;
		}
		else
		{
			return Config.get().presentableTenantIdentification(tenantSession.tenant);
		}
	}

	@Inject
	private
	Block onlyOne;

	@Inject
	private
	Block fromMany;

	public
	Block getAppropriateBlock()
	{
		final
		int count=usernames.size();

		if (count==0)
		{
			//TODO: low: this will probably throw a NPE
			return null;
		}
		else
		if (count==1)
		{
			username=usernames.iterator().next();
			return onlyOne;
		}
		else
		{
			return fromMany;
		}
	}

	Object onActionFromDoOne(Username username)
	{
		return verifySaveAndContinue(username);
	}

	Object onActionFromDoMany(Username username)
	{
		return verifySaveAndContinue(username);
	}

	private
	Object verifySaveAndContinue(Username username)
	{
		if (!sameUser(username.user))
		{
			return new ErrorResponse(400, "username/user mismatch");
		}

		final
		TenantSession tenantSession = authSession.getTenantSession();

		final
		Tenant tenant=tenantSession.tenant;

		if (tenant.fixedUsername && tenantSession.username!=null)
		{
			log.debug("ignoring change-username b/c tenant requires fixed usernames");
		}
		else
		{
			saveUsernameSelection(tenantSession, username);
		}

		return continueUser.toNextTenantSessionStep();
	}

	@InjectPage
	private
	ContinueUser continueUser;

	@CommitAfter
	private
	void saveUsernameSelection(TenantSession tenantSession, Username username)
	{
		tenantSession.username=username;
		session.save(tenantSession);
	}

	private
	boolean sameUser(DBUser thatUser)
	{
		return this.user.id.equals(thatUser.id);
	}

}
