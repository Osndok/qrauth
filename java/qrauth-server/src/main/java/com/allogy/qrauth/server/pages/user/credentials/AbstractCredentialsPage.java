package com.allogy.qrauth.server.pages.user.credentials;

import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.pages.Index;
import com.allogy.qrauth.server.services.AuthSession;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.Session;

/**
 * Created by robert on 2/26/15.
 */
public
class AbstractCredentialsPage
{
	@Property
	protected
	DBUserAuth userAuth;

	@Property
	protected
	DBUserAuth myAuth;

	Object onActivate()
	{
		if (userAuth == null)
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

	Object onActivate(DBUserAuth dbUserAuth)
	{
		this.userAuth = dbUserAuth;
		this.myAuth=authSession.getDBUserAuth();

		if (myAuth==null) return Index.class;

		if (!theSameUser(userAuth, myAuth))
		{
			return new ErrorResponse(403, "user/credential mismatch");
		}

		/*
		TODO: enable this check ???

		if (Death.hathVisited(userAuth))
		{
			return new ErrorResponse(403, "user/credential mismatch");
		}
		*/

		return null;
	}

	private static
	boolean theSameUser(DBUserAuth a, DBUserAuth b)
	{
		return (a.user.id.equals(b.user.id));
	}

	Object onPassivate()
	{
		return userAuth.id;
	}
}
