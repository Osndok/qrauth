package com.allogy.qrauth.server.pages.api.nut;

import com.allogy.qrauth.server.entities.Nut;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;

/**
 * Created by robert on 3/10/15.
 */
public
class StateNut
{
	Object onActivate()
	{
		return new ErrorResponse(404, "missing nut number or secret");
	}

	private
	Long nutId;

	private
	String secret;

	Object onPassivate()
	{
		//WARNING: the second parameter *CANNOT* read "nut.semiSecretValue"! or else basic programming errors might leak the secret!
		return new Object[] {
			nutId,
			secret
		};
	}

	public
	StateNut with(Nut nut)
	{
		this.nutId=nut.id;
		this.secret=nut.semiSecretValue;
		return this;
	}

	@Inject
	private
	Response response;

	Object onActivate(Nut nut, String providedSecret)
	{
		if (nut == null || !nut.semiSecretValue.equals(providedSecret))
		{
			return new ErrorResponse(404, "invalid nut id/secret combination");
		}

		this.nutId = nut.id;
		this.secret = providedSecret;

		response.setHeader("Access-Control-Allow-Origin", "*");

		return new TextStreamResponse("text/plain", nut.getState().toString());
	}

}
