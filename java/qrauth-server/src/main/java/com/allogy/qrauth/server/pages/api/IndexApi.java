package com.allogy.qrauth.server.pages.api;

import com.allogy.qrauth.server.helpers.ErrorResponse;

/**
 * Created by robert on 2/12/15.
 */
public
class IndexApi
{
	Object onActivate()
	{
		return new ErrorResponse(404, "unknown api group or function");
	}
}
