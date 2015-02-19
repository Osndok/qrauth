package com.allogy.qrauth.server.pages.api;

import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.services.DBTiming;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	ErrorResponse missingParameter(String key)
	{
		return new ErrorResponse(400, String.format("parameter named '%s' was expected, but not received\n", key));
	}

	protected final
	Logger log;

	protected
	AbstractAPICall()
	{
		log = LoggerFactory.getLogger(getClass());
	}

}
