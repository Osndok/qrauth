package com.allogy.qrauth.server.pages.user.names;

import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.entities.Username;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.pages.Index;
import com.allogy.qrauth.server.services.AuthSession;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.Session;

/**
 * Created by robert on 2/28/15.
 */
public
class AbstractNamesPage
{
	@Property
	protected
	Username username;

	@Property
	protected
	DBUserAuth myAuth;

	Object onActivate()
	{
		if (username == null)
		{
			return new ErrorResponse(404, "missing credential identity number");
		}
		else
		{
			return null;
		}
	}

	@Inject
	protected
	Session session;

	@Inject
	protected
	AuthSession authSession;

	Object onActivate(Username username)
	{
		this.username = username;
		this.myAuth = authSession.getDBUserAuth();

		if (myAuth == null) return Index.class;

		if (!theSameUser(username, myAuth))
		{
			return new ErrorResponse(403, "user/credential mismatch");
		}

		/*
		TODO: enable this check ???

		if (Death.hathVisited(username))
		{
			return new ErrorResponse(403, "user/credential mismatch");
		}
		*/

		return null;
	}

	private static
	boolean theSameUser(Username a, DBUserAuth b)
	{
		return (a.user.id.equals(b.user.id));
	}

	Object onPassivate()
	{
		return username.id;
	}

	public
	AbstractNamesPage with(Username username)
	{
		this.username = username;
		return this;
	}
}
