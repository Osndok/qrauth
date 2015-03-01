package com.allogy.qrauth.server.pages.user.names;

import com.allogy.qrauth.server.entities.Username;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.pages.user.AbstractUserPage;
import com.allogy.qrauth.server.pages.user.NamesUser;
import com.allogy.qrauth.server.services.Journal;
import com.allogy.qrauth.server.services.Policy;
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

	@Inject
	private
	Logger log;

	@Inject
	private
	Journal journal;

	@InjectPage
	private
	EditNames editNamesPage;

	@CommitAfter
	Object onSuccess()
	{
		log.debug("onSuccess()");

		if (policy.wouldAllowAdditionalUsernames(user, false) && policy.wouldAllowUsernameToBeRegistered(displayName))
		{
			final
			String matchValue = policy.usernameMatchFilter(displayName);

			Username username = getUsername(matchValue);

			if (username == null)
			{
				username = new Username();
				username.user = user;
				username.displayValue = displayName.trim();
				username.matchValue = matchValue;
				session.save(username);

				journal.allocatedUsername(username);

				//return editNamesPage.with(username);
				return NamesUser.class;
			}

			return this;
		}
		else
		{
			return new ErrorResponse(400,
										"please wait a moment, and retry... maybe you have too many active usernames?");
		}
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
}
