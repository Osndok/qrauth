package com.allogy.qrauth.server.pages.internal;

import com.allogy.qrauth.server.helpers.ErrorResponse;

/**
 * Created by robert on 2/20/15.
 */
public
class IndexInternal
{
	Object onActivate()
	{
		return new ErrorResponse(404, "unknown internal page, api, or function group\n");
	}
}
