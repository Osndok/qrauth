package com.allogy.qrauth.server.services;

import com.allogy.qrauth.server.entities.DBUser;
import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.entities.TenantSession;
import com.allogy.qrauth.server.entities.Username;
import org.apache.tapestry5.services.Dispatcher;

/**
 *
 * Used to access and maintain the session to the qrauth app itself.
 * This is implemented using a cookie an
 *
 * NB: there are many other 'sessions' hereabout. Including:
 * (1) hibernate Session
 * (2) TenantSession (connecting 'our' session to 'their' session)
 */
public
interface AuthSession extends Dispatcher
{
	/**
	 * @return true if (and only if) the user has presented a currently-valid login cookie with this request
	 */
	boolean isLoggedIn();

	/**
	 * @return the currently-logged-in DBUser, or null if this request is not authenticated.
	 */
	DBUser getDBUser();

	/**
	 * @return the authentication method used to login, or null if this request is either not-authenticated or is somehow implicitly authenticated.
	 */
	DBUserAuth getDBUserAuth();

	/**
	 * @return the number of milliseconds until the current session expires (either by virtue of a short login-method or a global logout epoch)
	 */
	long timeLeft();

	/**
	 * Asks the browser to forget the previously-provided cookie, which (if remembered) would still allow
	 * that machine access to this system.
	 *
	 * NB: this will cause a log entry to be written in the user-accessible authentication history.
	 *
	 */
	void discardAuthenticationCookie();

	/**
	 * Asks the remote browser to remember a cookie which will server to authenticate them between page requests.
	 *
	 * NB: this will cause a log entry to be written in the user-accessible authentication history.
	 *
	 * @param dbUserAuth - the user *and* authentication method that should be activated
	 * @param username - is the username that the user used to authenticate, or null if not applicable
	 */
	void authenticateRemoteBrowser(DBUserAuth dbUserAuth, Username username, TenantSession tenantSession);

	Username getUsername();
}
