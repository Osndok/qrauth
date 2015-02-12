package com.allogy.qrauth.server.helpers;

import org.apache.tapestry5.ContentType;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;

/**
 * User: robert
 * Date: 2014/01/20
 * Time: 2:13 PM
 */
public
class ErrorResponse extends TextStreamResponse
{
	public
	ErrorResponse(String text)
	{
		super("text/plain", text);
		this.status=400;
	}

	public
	ErrorResponse(ContentType contentType, String text)
	{
		super(contentType, text);
		this.status=400;
	}

	public
	ErrorResponse(int status, String text)
	{
		super("text/plain", text);
		this.status=status;
	}

	public
	ErrorResponse(int status, ContentType contentType, String text)
	{
		super(contentType, text);
		this.status=status;
	}

	private final
	int status;

	@Override
	public
	void prepareResponse(Response response)
	{
		super.prepareResponse(response);
		response.setStatus(status);
	}
}
