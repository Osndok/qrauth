package com.allogy.qrauth.server.pages.user;

import com.allogy.qrauth.server.entities.TenantUser;
import com.allogy.qrauth.server.services.Policy;
import com.allogy.qrauth.server.services.impl.Config;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Created by robert on 2/25/15.
 */
public
class SitesUser extends AbstractUserPage
{
	@Property
	private
	List<TenantUser> tenantUsers;

	private
	void onActivate()
	{
		tenantUsers=session.createCriteria(TenantUser.class)
			.add(Restrictions.eq("user", user))
			.list()
			;

		final
		Config config=Config.get();

		if (config.hasSupervisor())
		{
			for (TenantUser tenantUser : tenantUsers)
			{
				if (tenantUser.authAdmin && config.isSupervisor(tenantUser.tenant))
				{
					canEnterSupervisorMode=true;
				}
			}
		}
	}

	@Property
	private
	TenantUser tenantUser;

	@Inject
	private
	Policy policy;

	public
	boolean getCanRegisterTenancy()
	{
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

	@Property
	private
	boolean canEnterSupervisorMode;
}
