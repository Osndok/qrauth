package com.allogy.qrauth.server.pages.user.names;

import com.allogy.qrauth.server.entities.Username;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.pages.user.NamesUser;
import com.allogy.qrauth.server.services.Journal;
import org.apache.tapestry5.annotations.PageActivationContext;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;

import java.util.Date;

/**
 * Created by robert on 2/28/15.
 */
public
class RevokeNames extends AbstractNamesPage
{
	@Property
	private
	String memo;

	@CommitAfter
	Object onSuccess()
	{
		if (!Death.hathVisited(username))
		{
			username.deadline=new Date();
			username.deathMessage=memo;
			session.save(username);

			journal.revokedUsername(username);
		}
		return NamesUser.class;
	}

	@Inject
	private
	Journal journal;
}
