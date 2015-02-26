package com.allogy.qrauth.server.pages.user;

import com.allogy.qrauth.server.entities.DBUser;
import com.allogy.qrauth.server.entities.LogEntry;
import com.allogy.qrauth.server.entities.TenantIP;
import com.allogy.qrauth.server.services.AuthSession;
import com.allogy.qrauth.server.services.Network;
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
class ActivityUser extends AbstractUserPage
{
	@Inject
	private
	Session session;

	public
	GridDataSource getDataSource()
	{
		return new HibernateGridDataSource(session, LogEntry.class)
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
	LogEntry logEntry;

	public
	boolean isFromMyIp()
	{
		final
		TenantIP tenantIP=logEntry.tenantIP;

		if (tenantIP==null)
		{
			return false;
		}
		else
		{
			//TODO: do we need to consider subnet records here?!?!
			return myIpAddress.equals(tenantIP.ipAddress);
		}
	}

}
