package com.allogy.qrauth.server.services.filters;

import com.allogy.qrauth.server.services.Network;
import org.apache.tapestry5.services.HttpServletRequestFilter;
import org.apache.tapestry5.services.HttpServletRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * An opportunistic ban filter that short-circuits as much of the tapestry stack as possible, even
 * forbidding asset fetching.
 */
public
class LowLevelBanFilter implements HttpServletRequestFilter
{
	private final
	Network network;

	public
	LowLevelBanFilter(Network network)
	{
		this.network = network;
	}

	private static final Logger log = LoggerFactory.getLogger(LowLevelBanFilter.class);

	public
	boolean service(
					   HttpServletRequest httpServletRequest,
					   HttpServletResponse httpServletResponse,
					   HttpServletRequestHandler httpServletRequestHandler
	) throws IOException
	{
		if (network.addressCacheShowsBan(httpServletRequest))
		{
			log.trace("HIT");
			httpServletResponse.setStatus(403);
			httpServletResponse.setContentType("text/plain");

			ServletOutputStream outputStream = httpServletResponse.getOutputStream();
			outputStream.println(network.bestEffortBanMessage(httpServletRequest));
			//???: close/flush?

			return true;
		}
		else
		{
			return httpServletRequestHandler.service(httpServletRequest, httpServletResponse);
		}
	}
}
