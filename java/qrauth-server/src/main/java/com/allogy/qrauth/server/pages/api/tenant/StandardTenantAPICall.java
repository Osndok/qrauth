package com.allogy.qrauth.server.pages.api.tenant;

import com.allogy.qrauth.server.entities.Tenant;
import org.apache.tapestry5.annotations.Property;

/**
 * Created by robert on 3/16/15.
 */
public
class StandardTenantAPICall extends AbstractTenantAPICall
{
	public static final String PARAMETER_API_KEY = "api_key";

	@Property
	protected
	Tenant tenant;

	private
	Object onActivate()
	{
		log.debug("lookup provided api_key");

		final
		String apiKey = request.getParameter(PARAMETER_API_KEY);
		{
			if (apiKey == null || apiKey.isEmpty())
			{
				return missingParameter(PARAMETER_API_KEY);
			}
		}

		tenant=fromUnhashedAPIKey(apiKey);

		if (tenant==null)
		{
			return invalidParameter(PARAMETER_API_KEY);
		}

		log.debug("standard api call from {}", tenant);
		return null;
	}
}
