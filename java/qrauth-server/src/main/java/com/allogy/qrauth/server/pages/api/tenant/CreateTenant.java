package com.allogy.qrauth.server.pages.api.tenant;

import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.entities.TenantIP;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.services.Journal;
import com.allogy.qrauth.server.services.Network;
import com.allogy.qrauth.server.services.Policy;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.util.TextStreamResponse;

/**
 * Created by robert on 2/18/15.
 */
public
class CreateTenant extends AbstractTenantAPICall
{
	private static final String PARAMETER_PRIMARY   = "primary";
	private static final String PARAMETER_SECONDARY = "secondary";

	Object onActivate()
	{
		final
		long startTime=System.currentTimeMillis();

		final
		TenantIP ip = network.needIPForThisRequest(null);

		if (ip == null)
		{
			return new ErrorResponse(500, "no ip information");
		}

		if (Death.hathVisited(ip))
		{
			return new ErrorResponse(403, Death.noteMightSay(ip, "not allowed from your computer"));
		}

		if (!isPostRequest())
		{
			return mustBePostRequest();
		}

		if (!policy.allowsAnonymousCreationOfNewTenants())
		{
			return new ErrorResponse(403, "forbidden by current policy");
		}

		final
		String hashedPrimaryAPIKey;
		{
			final
			String primaryApiKey = request.getParameter(PARAMETER_PRIMARY);

			if (primaryApiKey==null)
			{
				return missingParameter(PARAMETER_PRIMARY);
			}

			hashedPrimaryAPIKey=hashing.forDatabaseLookupKey(primaryApiKey);
		}

		final
		String hashedSecondaryAPIKey;
		{
			final
			String secondaryApiKey = request.getParameter(PARAMETER_SECONDARY);

			if (secondaryApiKey == null)
			{
				return missingParameter(PARAMETER_SECONDARY);
			}

			hashedSecondaryAPIKey=hashing.forDatabaseLookupKey(secondaryApiKey);
		}

		Tenant tenant = fromHashedAPIKey(hashedPrimaryAPIKey);

		if (tenant==null)
		{
			tenant= fromHashedAPIKey(hashedSecondaryAPIKey);
		}

		journal.noticeSuccess(ip);

		if (tenant!=null)
		{
			dbTiming.concerning("create-tenant").shorterPath(startTime);
			log.info("possible tenant api-key collision or 'double-post' of api call");

			response.setStatus(201);
			return new TextStreamResponse("text/plain", "already created");
		}

		addNewTenantDatabaseRecord(hashedPrimaryAPIKey, hashedSecondaryAPIKey, ip);

		dbTiming.concerning("create-tenant").longestPath(startTime);

		response.setStatus(201);
		return new TextStreamResponse("text/plain", "new tenant record created");
	}

	@CommitAfter
	private
	Tenant addNewTenantDatabaseRecord(String hashedPrimaryApiKey, String hashedSecondaryApiKey, TenantIP tenantIP)
	{
		final
		Tenant tenant=new Tenant();

		tenant.hashedApiKeyPrimary=hashedPrimaryApiKey;
		tenant.hashedApiKeySecondary=hashedSecondaryApiKey;
		tenant.tenantIP=tenantIP;

		tenant.newUsers=true;

		tenant.config="{}";
		tenant.fieldDescriptionsJson="{}";
		tenant.permissionsDescriptionsJson="{}";

		session.save(tenant);

		log.debug("added {}", tenant);

		return tenant;
	}

	@Inject
	private
	Network network;

	@Inject
	private
	Policy policy;

	@Inject
	private
	Request request;

	@Inject
	private
	Journal journal;
}
