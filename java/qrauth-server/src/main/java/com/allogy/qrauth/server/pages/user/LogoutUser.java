package com.allogy.qrauth.server.pages.user;

import com.allogy.qrauth.server.pages.Index;
import com.allogy.qrauth.server.services.AuthSession;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.util.TextStreamResponse;

/**
 * Created by robert on 2/25/15.
 */
public
class LogoutUser
{
	@Inject
	private
	AuthSession authSession;

	Object onSelectedFromConfirmLogout()
	{
		authSession.discardAuthenticationCookie();
		//return new TextStreamResponse("text/plain", "You should now be logged out");
		return Index.class;
	}
}
