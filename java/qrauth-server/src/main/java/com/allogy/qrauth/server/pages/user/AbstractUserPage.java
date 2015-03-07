package com.allogy.qrauth.server.pages.user;

import com.allogy.qrauth.server.entities.DBUser;
import com.allogy.qrauth.server.services.AuthSession;
import com.allogy.qrauth.server.services.Network;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.Session;

/**
 * Most pages under '/user/' require the user to be logged in, and (if not) we direct them to the login page,
 * which is one of the exceptions that is still under '/user/'.
 */
public
class AbstractUserPage
{
	@Property
	protected
	DBUser user;

	@Inject
	protected
	AuthSession authSession;

	@Inject
	protected
	Network network;

	@Inject
	protected
	Session session;

	protected
	String myIpAddress;

	private
	Object onActivate()
	{
		myIpAddress = network.getIpAddress();
		user = authSession.getDBUser();

		if (user == null)
		{
			return LoginUser.class;
		}
		else
		{
			return null;
		}
	}
}
