package com.allogy.qrauth.server.pages.user;

import com.allogy.qrauth.server.entities.LogEntry;
import com.allogy.qrauth.server.entities.TenantUser;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.hibernate.HibernateGridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 * Created by robert on 2/25/15.
 */
public
class SitesUser extends AbstractUserPage
{
	public
	GridDataSource getDataSource()
	{
		return new HibernateGridDataSource(session, TenantUser.class)
		{
			@Override
			protected
			void applyAdditionalConstraints(Criteria criteria)
			{
				criteria.add(Restrictions.eq("user", user));
			}
		};
	}

	@Property
	private
	TenantUser tenantUser;

}
