package com.allogy.qrauth.server.services.filters;

import com.allogy.qrauth.server.services.Network;
import org.apache.tapestry5.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by robert on 2/19/15.
 */
public
class HighLevelBanFilter implements Dispatcher
{
	private static final
	Logger log = LoggerFactory.getLogger(HighLevelBanFilter.class);

	private final
	Network network;

	public
	HighLevelBanFilter(Network network)
	{
		this.network = network;
	}

	@Override
	public
	boolean dispatch(Request request, Response response) throws IOException
	{
		if (network.addressIsGenerallyBlocked())
		{
			log.trace("HIT");

			response.setStatus(403);

			OutputStream outputStream = response.getOutputStream("text/plain");
			outputStream.write(network.bestEffortBanMessage().getBytes());
			outputStream.write('\n');
			//???: close/flush?

			return true;
		}
		else
		{
			return false;
		}
	}

	/*
	public
	void handleComponentEvent(
								 ComponentEventRequestParameters componentEventRequestParameters,
								 ComponentRequestHandler componentRequestHandler
	) throws IOException
	{
		log.debug("component event");
		componentRequestHandler.handleComponentEvent(componentEventRequestParameters);
	}

	@Override
	public
	void handlePageRender(
							 PageRenderRequestParameters pageRenderRequestParameters,
							 ComponentRequestHandler componentRequestHandler
	) throws IOException
	{
		log.debug("page render");
		componentRequestHandler.handlePageRender(pageRenderRequestParameters);
	}
	*/
}
