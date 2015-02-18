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
	public static
	boolean hathVisited(Mortal mortal)
	{
		final
		Date deadline=mortal.getDeadline();

		return deadline!=null && deadline.getTime() <= System.currentTimeMillis();
	}

	public static
	String noteMightSay(Mortal mortal, String _default)
	{
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
