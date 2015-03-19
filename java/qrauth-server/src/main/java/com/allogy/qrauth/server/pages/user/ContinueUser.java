package com.allogy.qrauth.server.pages.user;

import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.entities.TenantSession;
import com.allogy.qrauth.server.entities.Username;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.pages.user.names.AddNames;
import com.allogy.qrauth.server.pages.user.names.SelectNames;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Answers the question, "where do we send the user now", and can be
 * represented as a static link or called directly.
 *
 * Created by robert on 3/18/15.
 */
public
class ContinueUser extends AbstractUserPage
{
	public
	Object toNextTenantSessionStep()
	{
		//TODO: !!!: this sort of problem is not obvious (page-to-page communication != activated page), but can have security implications... thus we NEED to make sure all such are found!
		if (user==null)
		{
			user=authSession.getDBUser();
			log.debug("restored null user field");
		}

		final
		TenantSession tenantSession=authSession.getTenantSession();

		if (tenantSession==null)
		{
			return new ErrorResponse(500, "sorry, lost track of tenant session");
		}

		final
		Tenant tenant=tenantSession.tenant;

		if (tenant.requireUsername)
		{
			if (tenantSession.username==null)
			{
				if (userDoesNotHaveAnyUsernames())
				{
					log.debug("tenant requires a username, but the user does not have any");
					return addNamesPage.forTenantRequirement();
				}
				else
				{
					return SelectNames.class;
				}
			}
			else
			{
				log.debug("tenant *does* require a username, but the user has used (or selected, or is required to use): {}", tenantSession.username);
			}
		}

		try
		{
			return new URL(tenantSession.return_url);
		}
		catch (MalformedURLException e)
		{
			log.error("bad return-url?: {}", tenantSession.return_url, e);
			return ActivityUser.class;
		}
	}

	private
	boolean userDoesNotHaveAnyUsernames()
	{
		//TODO: bug?: isNull(deadline) is needed, but not the same as a date comparison
		//TODO: this can probably be optimized.
		//See: http://forum.spring.io/forum/spring-projects/data/18371-fastest-way-to-get-a-count-of-objects
		final
		int count=session.createCriteria(Username.class)
			.add(Restrictions.eq("user", user))
			.add(Restrictions.isNull("deadline"))
			.list()
			.size()
			;

		log.debug("user has {} usernames without a deadline", count);
		return (count==0);
	}

	@InjectPage
	private
	AddNames addNamesPage;

	@Inject
	private
	Logger log;

	private
	Object onActivate()
	{
		return toNextTenantSessionStep();
	}
}
