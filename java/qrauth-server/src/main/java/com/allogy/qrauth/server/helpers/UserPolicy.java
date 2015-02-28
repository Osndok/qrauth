package com.allogy.qrauth.server.helpers;

import com.allogy.qrauth.server.entities.DBUser;
import com.allogy.qrauth.server.entities.DBUserAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by robert on 2/27/15.
 */
public
class UserPolicy
{
	private static final Logger log = LoggerFactory.getLogger(UserPolicy.class);

	/**
	 * Prevents the implicit allocation of close/mis-typed usernames at login time.
	 *
	 * Helps prevent "have the password guess the username" attacks from working... for some mechanisms.
     *
	 * @param user - the user in question
	 * @return true if the user has specifically opted to require a username as a 'something you know' 2nd-factor
	 */
	public static
	boolean requiresUsername(DBUser user)
	{
		log.error("unimplemented: requiresUsername()?");
		//NB: the default must *ALWAYS* be false, or this *WILL* interfere with login methods that do not require a username
		return false;
	}

	/**
	 * Helps prevent "have the password guess the username" attacks from working... for some mechanisms.
	 *
	 * @param user - the user in question
	 * @return true if the user has specifically opted to require a blank username as a 'something you know' 2nd-factor
	 */
	public static
	boolean requiresEmptyUsername(DBUser user)
	{
		return false;
	}

	public static
	boolean requiresUsername(DBUserAuth userAuth)
	{
		return requiresUsername(userAuth.user);
	}

	public static
	boolean requiresEmptyUsername(DBUserAuth userAuth)
	{
		return requiresEmptyUsername(userAuth.user);
	}
}
