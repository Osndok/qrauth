package com.allogy.qrauth.server.pages.debug;

import com.allogy.qrauth.server.services.Nuts;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.util.TextStreamResponse;

import java.util.Arrays;

/**
 * Created by robert on 2/16/15.
 */
public
class NutsDebug
{
	Object onActivate()
	{
		final
		String stringValue=nuts.toStringValue(nuts.generateBytes());

		return new TextStreamResponse("text/plain", stringValue);
	}

	Object onActivate(String stringValue)
	{
		final
		byte[] bytes = nuts.fromStringValue(stringValue);

		return new TextStreamResponse("text/plain", Arrays.toString(bytes));
	}

	@Inject
	private
	Nuts nuts;
}
