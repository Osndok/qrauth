package com.allogy.qrauth.server.pages.api.tenant;

import com.allogy.qrauth.server.entities.TenantIP;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.services.Network;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.util.TextStreamResponse;

/**
 * Created by robert on 2/18/15.
 */
public
class CreateTenant
{
	Object onActivate()
	{
		final
		TenantIP ip = network.needIPForThisRequest(null);

		if (ip==null)
		{
			return new ErrorResponse(500, "no ip information");
		}

		if (Death.hathVisited(ip))
		{
			return new ErrorResponse(403, Death.noteMightSay(ip, "not allowed from your computer"));
		}

		if (!request.getMethod().equals("POST"))
		{
			return new ErrorResponse(400, "POST required");
		}

		return new TextStreamResponse("text/plain", ip.toString());
	}

	@Inject
	private
	Network network;

	@Inject
	private
	Request request;
}
