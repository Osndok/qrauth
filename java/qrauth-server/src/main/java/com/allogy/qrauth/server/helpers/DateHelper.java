package com.allogy.qrauth.server.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by robert on 2/27/15.
 */
public
class DateHelper
{
	public static
	String iso8601()
	{
		return iso8601_formatter.get().format(new Date());
	}

	public static
	String iso8601(Date date)
	{
		return iso8601_formatter.get().format(date);
	}

	public static final TimeZone UTC=TimeZone.getTimeZone("UTC");

	private static final
	ThreadLocal<SimpleDateFormat> iso8601_formatter = new ThreadLocal<SimpleDateFormat>()
	{
		@Override
		protected
		SimpleDateFormat initialValue()
		{
			final
			SimpleDateFormat retval = new SimpleDateFormat("yyyy-MM-dd HH:mm'z'");

			retval.setTimeZone(UTC);

			return retval;
		}
	};
}
