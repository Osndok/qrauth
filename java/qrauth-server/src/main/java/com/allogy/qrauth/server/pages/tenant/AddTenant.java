package com.allogy.qrauth.server.pages.tenant;

import com.allogy.qrauth.server.entities.DBUser;
import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.entities.TenantUser;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.pages.user.AbstractUserPage;
import com.allogy.qrauth.server.pages.user.SitesUser;
import com.allogy.qrauth.server.services.Hashing;
import com.allogy.qrauth.server.services.Policy;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Created by robert on 3/13/15.
 */
public
class AddTenant extends AbstractUserPage
{
	@Inject
	private
	Policy policy;

	@Property
	private
	Tenant tenant;

	@Property
	private
	String primaryApiKey;

	@Property
	private
	String secondaryApiKey;

	private
	void onActivate()
	{
		if (tenant == null)
		{
			tenant = new Tenant();
			tenant.newUsers=true;
		}

		if (primaryApiKey==null)
		{
			primaryApiKey = UUID.randomUUID().toString();
		}

		if (secondaryApiKey==null)
		{
			secondaryApiKey = UUID.randomUUID().toString();
		}
	}

	@Inject
	private
	Logger log;

	private
	Object onSuccess()
	{
		if (!canRegisterTenancy())
		{
			return new ErrorResponse(400, "maximum number of per-user tenants reached");
		}

		{
			String name = tenant.requestedName;

			if (name == null || (name = name.trim()).length() == 0)
			{
				return new ErrorResponse(400, "requested name cannot be empty");
			}
			else
			if (policy.isAcceptableTenantName(name))
			{
				tenant.requestedName = name;
			}
			else
			{
				return new ErrorResponse(400, "requested tenant name is unacceptable");
			}
		}

		log.debug("registering new tenant: {}\n{}\n{}", tenant.requestedName, primaryApiKey, secondaryApiKey);

		tenant.primaryContact=user;
		tenant.hashedApiKeyPrimary=hashing.forDatabaseLookupKey(primaryApiKey);
		tenant.hashedApiKeySecondary=hashing.forDatabaseLookupKey(secondaryApiKey);
		tenant.config=tenant.fieldDescriptionsJson=tenant.permissionsDescriptionsJson="{}";
		session.save(tenant);

		saveAndCommitAdministrator(tenant, user);

		//TODO: go to tenant administration page
		return SitesUser.class;
	}

	@CommitAfter
	private
	void saveAndCommitAdministrator(Tenant tenant, DBUser user)
	{
		final
		TenantUser tenantUser=new TenantUser();

		tenantUser.tenant=tenant;
		tenantUser.user=user;
		tenantUser.authAdmin=true;
		tenantUser.shellAccess=true;
		tenantUser.configJson="{}";
		session.save(tenantUser);
	}

	@Inject
	private
	Hashing hashing;

	public
	boolean canRegisterTenancy()
	{
		final
		List<TenantUser> tenantUsers = session.createCriteria(TenantUser.class)
										   .add(Restrictions.eq("user", user))
										   .list();

		int numAdmin = 0;

		for (TenantUser tenantUser : tenantUsers)
		{
			if (tenantUser.authAdmin)
			{
				numAdmin++;
			}
		}

		return numAdmin < policy.getMaximumTenantsForUser(user);
	}
}
