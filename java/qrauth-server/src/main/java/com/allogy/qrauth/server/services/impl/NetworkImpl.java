package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.entities.TenantIP;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.Timing;
import com.allogy.qrauth.server.services.DBTiming;
import com.allogy.qrauth.server.services.Network;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by robert on 2/18/15.
 */
public
class NetworkImpl implements Network
{
	public static final boolean IN_CLUSTER = (System.getenv("HJ_PORT") != null);

	private static final Logger log = LoggerFactory.getLogger(Network.class);

	@Inject
	private
	DBTiming dbTiming;

	public
	TenantIP needIPForThisRequest(Tenant tenantFilter)
	{
		final
		long startTime=System.currentTimeMillis();

		final
		String ipAddress = getIpAddress();
		{
			if (ipAddress == null)
			{
				//dbTiming.concerning("ip-fetch").shorterPath(startTime);
				return null;
			}
		}

		final
		Object[] addressStringsToMatch;
		{
			final
			String subnetWildcard = getSubnetWildcard(ipAddress);

			log.debug("lookup({}, {}, {})", ipAddress, subnetWildcard, tenantFilter);

			if (subnetWildcard==null)
			{
				addressStringsToMatch=new Object[]{ipAddress};
			}
			else
			{
				addressStringsToMatch=new Object[]{ipAddress, subnetWildcard};
			}
		}

		final
		Session session=hibernateSessionManager.getSession();

		final
		Criteria criteria=session.createCriteria(TenantIP.class)
			.add(Restrictions.in("ipAddress", addressStringsToMatch))
			;

		if (tenantFilter == null)
		{
			criteria.add(Restrictions.isNull("tenant"));
		}
		else
		{
			criteria.add(Restrictions.or(
											Restrictions.isNull("tenant"),
											Restrictions.eq("tenant", tenantFilter)
				)
			);
		}

		final
		List<TenantIP> list=criteria.list();

		TenantIP generalSingle=null;
		TenantIP generalSubnet=null;
		TenantIP tenantSingle =null;
		TenantIP tenantSubnet =null;

		for (TenantIP tenantIP : list)
		{
			if (Death.hathVisited(tenantIP))
			{
				log.debug("dead/banned: {}", tenantIP);
				//no-timing, just dead...
				return tenantIP;
			}

			log.debug("got: {}", tenantIP);

			if (tenantIP.tenant==null)
			{
				if (isSubnetIP(tenantIP))
				{
					generalSubnet=tenantIP;
				}
				else
				{
					generalSingle=tenantIP;
				}
			}
			else
			{
				if (isSubnetIP(tenantIP))
				{
					tenantSubnet=tenantIP;
				}
				else
				{
					tenantSingle=tenantIP;
				}
			}

		}

		final
		Timing timing=dbTiming.concerning("ip-fetch");

		//Because we expect to return...
		//timing.longPath(startTime);

		if (tenantSingle!=null) return tenantSingle;
		if (tenantSubnet!=null) return tenantSubnet;
		if (generalSingle!=null) return generalSingle;
		if (generalSubnet!=null) return generalSubnet;

		final
		TenantIP tenantIP=new TenantIP();

		tenantIP.tenant=tenantFilter;
		tenantIP.ipAddress=ipAddress;

		session.save(tenantIP);

		hibernateSessionManager.commit();

		timing.longestPath(startTime);

		return tenantIP;
	}

	private
	boolean isSubnetIP(TenantIP tenantIP)
	{
		String s=tenantIP.ipAddress;
		char c=s.charAt(s.length()-1);
		return c=='*';
	}

	@Inject
	private
	HibernateSessionManager hibernateSessionManager;

	@Inject
	private
	RequestGlobals requestGlobals;

	private
	String getIpAddress()
	{
		final
		Request request = requestGlobals.getRequest();

		if (IN_CLUSTER)
		{
			return request.getHeader("AI-Gateway-IP");
		}
		else
		{
			return request.getRemoteHost();
		}
	}

	private
	String getSubnetWildcard(String ipAddress)
	{
		final
		int lastPeriod = ipAddress.lastIndexOf('.');

		if (lastPeriod > 0)
		{
			return ipAddress.substring(0, lastPeriod) + ".*";
		}
		else
		{
			return ipAddress;
		}
	}

	@Override
	public
	Collection<TenantIP> getExistingTenantIPsForThisOriginator()
	{
		final
		long startTime=System.currentTimeMillis();

		final
		String ipAddress = getIpAddress();
		{
			if (ipAddress == null)
			{
				//dbTiming.concerning("ip-fetch").shorterPath(startTime);
				return null;
			}
		}

		final
		Object[] addressStringsToMatch;
		{
			final
			String subnetWildcard = getSubnetWildcard(ipAddress);

			log.debug("list({}, {}, {})", ipAddress, subnetWildcard);

			if (subnetWildcard==null)
			{
				addressStringsToMatch=new Object[]{ipAddress};
			}
			else
			{
				addressStringsToMatch=new Object[]{ipAddress, subnetWildcard};
			}
		}

		final
		Session session=hibernateSessionManager.getSession();

		final
		Criteria criteria=session.createCriteria(TenantIP.class)
							  .add(Restrictions.in("ipAddress", addressStringsToMatch))
			;

		return criteria.list();
	}
}
