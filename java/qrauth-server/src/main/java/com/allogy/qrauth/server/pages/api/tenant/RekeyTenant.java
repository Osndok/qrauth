package com.allogy.qrauth.server.pages.api.tenant;

import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.util.TextStreamResponse;

/**
 * Created by robert on 2/18/15.
 */
public
class RekeyTenant extends AbstractTenantAPICall
{
	private static final String PARAMETER_NEXT_KEY="next_key";

	Object onActivate()
	{
		if (!isPostRequest())
		{
			return mustBePostRequest();
		}

		final
		String hashedSoonToBePrimaryKey;
		{
			final
			String unhashedSecondaryKey=request.getParameter(PARAMETER_API_KEY);

			if (unhashedSecondaryKey==null)
			{
				return missingParameter(PARAMETER_API_KEY);
			}
			else
			{
				hashedSoonToBePrimaryKey=hashing.forDatabaseLookupKey(unhashedSecondaryKey);
			}
		}

		final
		String hashedSoonToBeSecondaryKey;
		{
			final
			String unhashedPrimaryKey=request.getParameter(PARAMETER_NEXT_KEY);

			if (unhashedPrimaryKey==null)
			{
				return missingParameter(PARAMETER_NEXT_KEY);
			}
			else
			{
				hashedSoonToBeSecondaryKey=hashing.forDatabaseLookupKey(unhashedPrimaryKey);
			}
		}

		final
		Tenant tenant=fromHashedAPIKey(hashedSoonToBePrimaryKey);
		{
			if (tenant == null)
			{
				return missingParameter(PARAMETER_API_KEY);
			}
			else
			if (!tenant.hashedApiKeySecondary.equals(hashedSoonToBePrimaryKey))
			{
				//TODO: improve (or at least vet) this error message
				return new ErrorResponse(302, "The provided 'api_key' is valid, but the rekey was not successful because you supplied the primary key. "
											+ "If you have submitted this query more than once, then this may indicate that the previous rekey attempt was successful, shifting the secondary key to the new primary. "
											+ "Otherwise, please be sure to use the *secondary* api key when requesting a rekey.\n"
				);
			}
		}

		//These are low priorities, and might actually be dropped altogether...
		//It is, after all, the Tenant's responsibility to ensure the randomness of their api key.
		//TODO: consider catching the case when rekeying to a different tenant's primary-key
		//TODO: better handle the failed-to-update because someone else is using my secondary key as their primary
		//TODO: better handle the failed-to-update because someone else has that secondary key case
		updateTenantKeys(tenant, hashedSoonToBePrimaryKey, hashedSoonToBeSecondaryKey);

		response.setStatus(202);
		return new TextStreamResponse("text/plain", "accepted\n");
	}

	@CommitAfter
	private
	void updateTenantKeys(Tenant tenant, String newPrimaryKeyHash, String newSecondaryKeyHash)
	{
		tenant.hashedApiKeyPrimary=newPrimaryKeyHash;
		tenant.hashedApiKeySecondary=newSecondaryKeyHash;
		session.save(tenant);
	}
}
