package com.allogy.qrauth.server.pages.api;

import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.services.DBTiming;
import com.allogy.qrauth.server.services.Network;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by robert on 2/18/15.
 */
public abstract
class AbstractAPICall
{
	@Inject
	protected
	Request request;

	@Inject
	protected
	Response response;

	@Inject
	protected
	Session session;

	@Inject
	protected
	DBTiming dbTiming;

	protected
	boolean isPostRequest()
	{
		return request.getMethod().equals("POST");
	}

	protected
	ErrorResponse mustBePostRequest()
	{
		response.setHeader("Allow", "POST");
		return new ErrorResponse(405, "This API call requires a POST request method.\n");
	}

	protected
	Object mustBePostOrPreflightCheck()
	{
		if (request.getMethod().equals("OPTIONS"))
		{
			return new StreamResponse()
			{
				@Override
				public
				String getContentType()
				{
					return "none";
				}

				@Override
				public
				InputStream getStream() throws IOException
				{
					return null;
				}

				@Override
				public
				void prepareResponse(Response response)
				{
					response.addHeader("Access-Control-Allow-Origin", "*");
					response.addHeader("Access-Control-Allow-Methods", "POST");
				}
			};
		}
		else
		{
			return mustBePostRequest();
		}
	}

	protected
	ErrorResponse missingParameter(String key)
	{
		return new ErrorResponse(400, String.format("parameter named '%s' was expected, but not received\n", key));
	}

	protected
	ErrorResponse missingOrInvalidParameter(String key)
	{
		return new ErrorResponse(400, String.format("parameter named '%s' was missing, or not a valid value\n", key));
	}

	protected
	ErrorResponse invalidParameter(String key)
	{
		return new ErrorResponse(400, String.format("the value provided for the '%s' parameter was invalid\n", key));
	}

	protected
	ErrorResponse ipAddressIsBlacklisted()
	{
		return new ErrorResponse(403, network.bestEffortBanMessage());
	}

	protected final
	Logger log;

	protected
	AbstractAPICall()
	{
		log = LoggerFactory.getLogger(getClass());
	}

	@Inject
	protected
	Network network;

}
