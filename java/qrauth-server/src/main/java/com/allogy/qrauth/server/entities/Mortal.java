package com.allogy.qrauth.server.entities;

import java.util.Date;

/**
 * Created by robert on 2/13/15.
 */
public
interface Mortal
{
	/**
	 * Gives more information asto why this object is longer active, available, or trusted.
	 *
	 * WARNING: the presence of a death message is *NOT* sufficient to test if an object is, in fact, dead.
	 *
	 * @return a bit more information regarding why this object can no longer be used or trusted, or null if no such information is available
	 */
	String getDeathMessage();

	/**
	 * @return null if the object is still alive (and for the moment, indefinitely so), a past date if the object is now dead, or a future date if it's execution has been scheduled
	 */
	Date getDeadline();
}
