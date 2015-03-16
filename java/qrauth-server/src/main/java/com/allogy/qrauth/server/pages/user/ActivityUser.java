package com.allogy.qrauth.server.pages.user;

import com.allogy.qrauth.server.entities.*;
import com.allogy.qrauth.server.services.AuthSession;
import com.allogy.qrauth.server.services.Network;
import org.apache.tapestry5.annotations.PageActivationContext;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.Retain;
import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.hibernate.HibernateGridDataSource;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BeanModelSource;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 * Created by robert on 2/25/15.
 */
public
class ActivityUser extends AbstractUserPage
{
	@PageActivationContext
	private
	Tenant tenant;

	public
	GridDataSource getDataSource()
	{
		//TODO: how to best set the *initial* sorting on such an otherwise-nice high level widget?
		return new HibernateGridDataSource(session, LogEntry.class)
		{
			@Override
			protected
			void applyAdditionalConstraints(Criteria criteria)
			{
				if (tenant==null)
				{
					//Usually, a user is interested (and allowed) to see all their activity (regardless of tenant).
					criteria.add(Restrictions.eq("user", user));
				}
				else
				{
					TenantUser tenantUser = getTenantUser();

					if (tenantUser!=null && tenantUser.authAdmin)
					{
						//Tenant admins can see all tenant activity, not just their own (i.e. regardless of user).
						criteria.add(Restrictions.eq("tenant", tenant));
					}
					else
					{
						//When a normal user specifies a tenant, they usually mean to restrict the daunting log to
						//a single tenant.
						criteria.add(Restrictions.eq("user", user));
						criteria.add(Restrictions.eq("tenant", tenant));
					}
				}

				//dnw: criteria.addOrder(Order.desc("time"));
			}
		};
	}

	public
	BeanModel getBeanModel()
	{
		if (tenant==null)
		{
			return beanModelWithSite;
		}
		else
		{
			return beanModelWithoutSite;
		}
	}

	@Retain
	private
	BeanModel beanModelWithSite;

	@Retain
	private
	BeanModel beanModelWithoutSite;

	@Inject
	private
	BeanModelSource beanModelSource;

	void pageLoaded()
	{
		beanModelWithSite = beanModelSource.createDisplayModel(LogEntry.class, messages);

		beanModelWithSite.addEmpty("site");
		beanModelWithSite.addEmpty("ipAddress");
		beanModelWithSite.addEmpty("username");
		beanModelWithSite.addEmpty("authMethod");
		beanModelWithSite.reorder("time", "ipAddress", "message", "site", "username", "authMethod");

		beanModelWithoutSite = beanModelSource.createDisplayModel(LogEntry.class, messages);

		beanModelWithoutSite.addEmpty("ipAddress");
		beanModelWithoutSite.addEmpty("username");
		beanModelWithoutSite.addEmpty("authMethod");
		beanModelWithoutSite.reorder("time", "ipAddress", "message", "username", "authMethod");
	}

	@Inject
	private
	Messages messages;

	private
	TenantUser getTenantUser()
	{
		return (TenantUser) session.createCriteria(TenantUser.class)
								.add(Restrictions.eq("tenant", tenant))
								.add(Restrictions.eq("user", user))
								.uniqueResult()
			;
	}

	@Property
	private
	LogEntry logEntry;

	public
	boolean isFromMyIp()
	{
		final
		TenantIP tenantIP = logEntry.tenantIP;

		if (tenantIP == null)
		{
			return false;
		}
		else
		{
			//TODO: do we need to consider subnet records here?!?!
			return myIpAddress.equals(tenantIP.ipAddress);
		}
	}

	public
	String getImportance()
	{
		if (logEntry.important)
		{
			return "important";
		}
		else
		{
			return null;
		}
	}
}
