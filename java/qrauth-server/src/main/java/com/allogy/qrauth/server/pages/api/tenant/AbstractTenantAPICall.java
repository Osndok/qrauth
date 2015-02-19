package com.allogy.qrauth.server.pages.api.tenant;

import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.pages.api.AbstractAPICall;
import com.allogy.qrauth.server.services.Hashing;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.criterion.Restrictions;

/**
 * Created by robert on 2/18/15.
 */
public abstract
class AbstractTenantAPICall extends AbstractAPICall
{

	protected
	Tenant fromHashedAPIKey(final String hashedApiKey)
	{
		final
		Tenant retval=(Tenant)session.createCriteria(Tenant.class)
			.add(Restrictions.or(
									Restrictions.eq("hashedApiKeyPrimary", hashedApiKey),
									Restrictions.eq("hashedApiKeySecondary", hashedApiKey)
			))
			.uniqueResult()
			;

		log.debug("fromHashedAPIKey({}) -> {}", hashedApiKey, retval);

		return retval;
	}

	protected
	Tenant fromUnhashedAPIKey(final String unhashedApiKey)
	{
		return fromHashedAPIKey(hashing.forDatabaseLookupKey(unhashedApiKey));
	}

	@Inject
	protected
	Hashing hashing;

}
