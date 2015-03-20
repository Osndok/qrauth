package com.allogy.qrauth.server.pages.supervisor;

import com.allogy.qrauth.server.entities.DBUser;
import com.allogy.qrauth.server.entities.TenantUser;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.pages.user.AbstractUserPage;
import com.allogy.qrauth.server.services.impl.Config;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metamodel.binding.Caching;
import org.slf4j.Logger;

/**
 * Created by robert on 3/20/15.
 */
public abstract
class AbstractSupervisorPage extends AbstractUserPage
{
	//TODO: push this lower... into AbstractUserPage (requires hunting down conflicts)
	@Inject
	protected
	Logger log;

	private
	Object onActivate()
	{
		final
		Long supervisorId=Config.get().getSupervisorId();

		if (supervisorId==null)
		{
			return notSupervisor();
		}

		final
		TenantUser tenantUser=getTenantUser(supervisorId, user);

		if (tenantUser==null)
		{
			log.debug("no association with supervisor tenant");
			return notSupervisor();
		}
		else
		if (!tenantUser.authAdmin)
		{
			log.info("{} is not marked as an 'authAdmin' of the supervisor tenant {} in {}", user, tenantUser.tenant, tenantUser);
			return notSupervisor();
		}

		log.debug("{} is authorized to use supervisor panels due to 'authAdmin' flag of {} concerning {}", user, tenantUser, tenantUser.tenant);

		return null;
	}

	private
	ErrorResponse notSupervisor()
	{
		return new ErrorResponse(403, "supervisor access is required for this page/function");
	}

	private
	TenantUser getTenantUser(Long tenantId, DBUser user)
	{
		return (TenantUser)session.createCriteria(TenantUser.class)
			.add(Restrictions.eq("tenant.id", tenantId))
			.add(Restrictions.eq("user", user))
			.uniqueResult()
			;
	}
}
