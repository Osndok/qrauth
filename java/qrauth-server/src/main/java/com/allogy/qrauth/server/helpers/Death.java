package com.allogy.qrauth.server.helpers;

import com.allogy.qrauth.server.entities.Mortal;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;

import java.util.Date;

/**
 * Created by robert on 2/18/15.
 */
public
class Death
{
	/**
	 * This is the preferred (and unified) way to check for the death of an object.
	 *
	 * For most objects, death is instant, as they will have their deadline set to
	 * now(), which also serves as a note for *when* the object was terminated.
	 *
	 * However... for some object (such as passwords), this deadline is intentionally
	 * set in the future.
	 *
	 * NB: it is certainly possible that a skewed clock (intentional or not) may
	 * errantly re-activate once-dead objects or prematurely kill objects that have
	 * not yet reached their time-limited end-of-life. This is accepted as a design
	 * trade-off to combine and simplify checking of both present & future object
	 * obsolescence.
	 *
	 * @param mortal - the object whose present relevance is to be determined
	 * @return true if (and only if) the time for the given mortal's death has come
	 * @throws NullPointerException if given argument is null
	 */
	public static
	boolean hathVisited(Mortal mortal) throws NullPointerException
	{
		final
		Date deadline=mortal.getDeadline();

		return deadline!=null && deadline.getTime() <= System.currentTimeMillis();
	}

	public static
	String noteMightSay(Mortal mortal, String _default)
	{
		if (mortal==null)
		{
			return _default;
		}

		final
		String message=mortal.getDeathMessage();

		if (message==null)
		{
			return _default;
		}
		else
		{
			return message;
		}
	}

	private Death() {}
}
