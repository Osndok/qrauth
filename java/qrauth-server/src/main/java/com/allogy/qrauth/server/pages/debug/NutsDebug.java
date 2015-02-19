package com.allogy.qrauth.server.pages.debug;

import com.allogy.qrauth.server.entities.Nut;
import com.allogy.qrauth.server.services.Network;
import com.allogy.qrauth.server.services.Nuts;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.util.TextStreamResponse;

import java.util.Arrays;

/**
 * TODO: delete this class
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
		if (stringValue.equals("allocate"))
		{
			Nut nut=nuts.allocate(null, network.needIPForThisRequest(null));
			return new TextStreamResponse("text/plain", nut.toString()+" -> "+nut.stringValue+"\n");
		}

		final
		byte[] bytes = nuts.fromStringValue(stringValue);

		return new TextStreamResponse("text/plain", Arrays.toString(bytes));
	}

	@Inject
	private
	Nuts nuts;

	@Inject
	private
	Network network;
}
