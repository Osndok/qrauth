package com.allogy.qrauth.server.entities;

/**
 * Created by robert on 3/7/15.
 */
public
enum NutState
{
	/**
	 * There is no indication that this nut has been seen, or is being considered for authentication.
	 */
	INIT,

	/**
	 * A SQRL client has made an inquiry regarding this nut, and has therefor 'locked' it to a particular
	 * identity (as indicated by the mutex field). Any observable reference to the nut's stringValue
	 * (i.e. the QR code) should be immediately hidden.
	 */
	LIMBO,

	/**
	 * The nut has been consumed via an 'ident' command, and the web page should advance to the follow-through
	 * page to finalize the process.
	 */
	READY,

	/**
	 * The nut has been consumed and an authentication ticket has been granted via the follow-through page.
	 */
	COMPLETE,

	/**
	 * The nut has been consumed for non-SQRL reasons; usually because the nut was attached to a login
	 * attempt that succeeded using a method other than SQRL.
	 */
	FAILED,

	;
}
