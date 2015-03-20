package com.allogy.qrauth.server.pages.user.names;

import com.allogy.qrauth.server.entities.TenantSession;
import com.allogy.qrauth.server.entities.Username;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.pages.user.AbstractUserPage;
import com.allogy.qrauth.server.pages.user.ContinueUser;
import com.allogy.qrauth.server.pages.user.NamesUser;
import com.allogy.qrauth.server.services.Journal;
import com.allogy.qrauth.server.services.Policy;
import com.allogy.qrauth.server.services.impl.Config;
import org.apache.tapestry5.Block;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.PageActivationContext;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;

/**
 * Created by robert on 2/27/15.
 */
public
class AddNames extends AbstractUserPage
{
	@Property
	@PageActivationContext
	private
	String displayName;

	public
	AddNames withDisplayName(String displayName)
	{
		this.displayName = displayName;
		return this;
	}

	private static final String FOR_TENANT_HINT="for_tenant";

	public
	AddNames forTenantRequirement()
	{
		//TODO: BUG: overloading this parameter makes "for_tenant" appear in the entry field; convert to query parameter?
		this.displayName=FOR_TENANT_HINT;
		return this;
	}

	@Inject
	private
	Logger log;

	@Inject
	private
	Journal journal;

	@InjectPage
	private
	EditNames editNamesPage;

	@InjectPage
	private
	ContinueUser continueUser;

	Object onSuccess()
	{
		log.debug("onSuccess()");

		if (!policy.wouldAllowUsernameToBeRegistered(displayName))
		{
			return new ErrorResponse(400, "sorry, that user name does not fit the owner's policy on allowed usernames");
		}
		else
		if (policy.wouldAllowAdditionalUsernames(user, false))
		{
			final
			String matchValue = policy.usernameMatchFilter(displayName);

			Username username = getUsername(matchValue);

			if (username == null)
			{

				username = createNewUsername(matchValue);

				if (authSession.endsWithTenantRedirection() && thatWasTheFirstAndOnlyActiveUsername())
				{
					return continueUser.toNextTenantSessionStep();
				}
				else
				{
					return NamesUser.class;
				}
			}

			return this;
		}
		else
		{
			return new ErrorResponse(400,
										"please wait a moment, and retry... maybe you have too many active usernames?");
		}
	}

	@CommitAfter
	private
	Username createNewUsername(String matchValue)
	{
		final
		Username username=new Username();

		username.user = user;
		username.displayValue = displayName.trim();
		username.unixValue = policy.usernameUnixFilter(displayName);
		username.matchValue = matchValue;
		session.save(username);

		journal.allocatedUsername(username);
		return username;
	}

	private
	boolean thatWasTheFirstAndOnlyActiveUsername()
	{
		final
		int count=session.createCriteria(Username.class)
			.add(Restrictions.eq("user", user))
			.add(Restrictions.isNull("deadline"))
			.list()
			.size()
			;

		log.debug("user now has {} usernames (to select towards tenant usage)", count);
		return (count==1);
	}

	private
	Username getUsername(String matchValue)
	{
		return (Username) session.createCriteria(Username.class)
							  .add(Restrictions.eq("matchValue", matchValue))
							  .uniqueResult();
	}

	@Inject
	private
	Policy policy;

	@Property
	private
	String hint;

	public
	String[] getSuggestions()
	{
		return new String[]{
			"TODO: list some actual, untaken, usernames based on dictionary word pairs and bits of supplied name",
			"TODO: clicking on a suggestion should immediately allocate it, they can revoke it later, if they don't want it."
		};
	}

	@Inject
	private
	Block tryAgain;

	@Inject
	private
	Block firstTry;

	@Inject
	private
	Block forTenant;

	public
	Block getHelpfulBlock()
	{
		if (displayName==null)
		{
			return firstTry;
		}
		else
		if (displayName.equals(FOR_TENANT_HINT))
		{
			return forTenant;
		}
		else
		{
			return tryAgain;
		}
	}

	public
	String getTenantName()
	{
		final
		TenantSession tenantSession=authSession.getTenantSession();

		if (tenantSession==null)
		{
			//???
			return "the website you were trying to log into visiting";
		}
		else
		{
			return Config.get().presentableTenantIdentification(tenantSession.tenant);
		}
	}
}
