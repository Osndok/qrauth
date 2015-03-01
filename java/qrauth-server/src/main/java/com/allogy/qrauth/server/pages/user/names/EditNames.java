package com.allogy.qrauth.server.pages.user.names;

import com.allogy.qrauth.server.entities.Username;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.pages.user.AbstractUserPage;
import com.allogy.qrauth.server.pages.user.NamesUser;
import com.allogy.qrauth.server.services.Policy;
import org.apache.tapestry5.annotations.PageActivationContext;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;

/**
 * Created by robert on 2/28/15.
 */
public
class EditNames extends AbstractNamesPage
{
	@PageActivationContext
	private
	Username username;

	@Property
	private
	String displayValue;

	Object onActivate()
	{
		if (username==null)
		{
			return new ErrorResponse(404, "missing username id number");
		}

		if (!username.user.id.equals(user.id))
		{
			return new ErrorResponse(403, "user/name mismatch");
		}

		this.displayValue=username.displayValue;

		return null;
	}

	@Inject
	private
	Policy policy;

	@CommitAfter
	Object onSuccess()
	{
		final
		String newMatch=policy.usernameMatchFilter(displayValue);

		if (newMatch.equals(username.matchValue))
		{
			username.displayValue=displayValue.trim();
			session.save(username);
			return NamesUser.class;
		}
		else
		{
			return new ErrorResponse("you can only change capitalization, punctuation, and spacing.");
		}
	}
}
