package com.allogy.qrauth.server.pages.supervisor.tenant;

import com.allogy.qrauth.server.entities.LogEntry;
import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.entities.TenantUser;
import com.allogy.qrauth.server.pages.supervisor.AbstractSupervisorPage;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.hibernate.HibernateGridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 * Created by robert on 3/20/15.
 */
public
class ApproveTenant extends AbstractSupervisorPage
{
	public
	GridDataSource getDataSource()
	{
		//TODO: how to best set the *initial* sorting on such an otherwise-nice high level widget?
		return new HibernateGridDataSource(session, Tenant.class)
		{
			@Override
			protected
			void applyAdditionalConstraints(Criteria criteria)
			{
				criteria.add(Restrictions.eq("needsReview", true));
				//criteria.addOrder(Order.asc("updated"));
			}
		};
	}

	@Property
	private
	Tenant tenant;

	public
	String getRowClass()
	{
		//TODO: non-conflicting name changes should not require approval
		//TODO: color should indicate task (new name, changed name)
		//TODO: color should indicate probability of conflict?
		//TODO: run tenant.requestedName through redux filter, and check for match?
		return "todo";
	}
}
