package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.entities.TenantIP;
import com.allogy.qrauth.server.entities.TenantSession;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.Timing;
import com.allogy.qrauth.server.services.DBTiming;
import com.allogy.qrauth.server.services.Network;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.services.cron.IntervalSchedule;
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by robert on 2/18/15.
 */
public
class NetworkImpl implements Network, Runnable
{
	public static final boolean IN_CLUSTER = (System.getenv("HJ_PORT") != null);

	private static final long PERIODIC_FLUSH_MILLIS = TimeUnit.MINUTES.toMillis(5);

	private static final Logger log = LoggerFactory.getLogger(Network.class);

	@Inject
	private
	DBTiming dbTiming;

	@PostInjection
	public
	void serviceStarts(PeriodicExecutor periodicExecutor)
	{
		periodicExecutor.addJob(new IntervalSchedule(PERIODIC_FLUSH_MILLIS), "flush-banned-ip-cache", this);
	}

	public
	TenantIP needIPForThisRequest(Tenant tenantFilter)
	{
		final
		long startTime = System.currentTimeMillis();

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

			if (subnetWildcard == null)
			{
				addressStringsToMatch = new Object[]{ipAddress};
			}
			else
			{
				addressStringsToMatch = new Object[]{ipAddress, subnetWildcard};
			}
		}

		final
		Session session = hibernateSessionManager.getSession();

		final
		Criteria criteria = session.createCriteria(TenantIP.class)
								.add(Restrictions.in("ipAddress", addressStringsToMatch));

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

	public
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
	String getIpAddress(HttpServletRequest request)
	{
		if (IN_CLUSTER)
		{
			return request.getHeader("AI-Gateway-IP");
		}
		else
		{
			return request.getRemoteAddr();
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
	Collection<TenantIP> getExistingTenantIPsForThisOriginator(Tenant tenant)
	{
		final
		Session session=hibernateSessionManager.getSession();

		final
		Criteria criteria=session.createCriteria(TenantIP.class)
							  .add(Restrictions.in("ipAddress", ipOrSubnet()))
			;

		if (tenant==null)
		{
			criteria.add(Restrictions.isNull("tenant"));
		}
		else
		{
			criteria.add(Restrictions.or(
											Restrictions.isNull("tenant"),
											Restrictions.eq("tenant", tenant)
			));
		}

		return criteria.list();
	}

	private
	Object[] ipOrSubnet()
	{
		final
		String ipAddress = getIpAddress();
		{
			if (ipAddress == null)
			{
				log.error("unable to get ip address");
				return new Object[]{"0.0.0.0"};
			}
		}

		final
		String subnetWildcard = getSubnetWildcard(ipAddress);

		log.debug("list({}, {})", ipAddress, subnetWildcard);

		if (subnetWildcard==null)
		{
			return new Object[]{ipAddress};
		}
		else
		{
			return new Object[]{ipAddress, subnetWildcard};
		}
	}

	@Override
	public
	Collection<TenantIP> getExistingTenantIPsForThisOriginatorAllTenants()
	{
		final
		Session session=hibernateSessionManager.getSession();

		final
		Criteria criteria=session.createCriteria(TenantIP.class)
							  .add(Restrictions.in("ipAddress", ipOrSubnet()))
			;

		return criteria.list();
	}

	@Override
	public
	boolean addressCacheShowsBan(HttpServletRequest httpServletRequest)
	{
		final
		String ipAddress = getIpAddress(httpServletRequest);
		{
			if (ipAddress == null)
			{
				return false;
			}
		}

		final
		String possibleCacheHit=STASHED_BAN_IPS[ipAddress.hashCode() % BAN_STASH_SIZE];

		return (possibleCacheHit!=null && possibleCacheHit.equals(ipAddress));
	}

	@Override
	public
	boolean ipMatch(TenantIP tenantIP, TenantIP rule)
	{
		if (tenantIP==null || rule==null) return false;

		String ip=tenantIP.ipAddress;
		String ipOrSubnet=rule.ipAddress;

		if (ipOrSubnet.endsWith(".*"))
		{
			String subnet=getSubnetWildcard(ip);

			return subnet.equals(ipOrSubnet);
		}
		else
		{
			return ip.equals(ipOrSubnet);
		}
	}

	@Override
	public
	TenantIP needIPForSession(TenantSession tenantSession)
	{
		if (tenantSession==null)
		{
			return needIPForThisRequest(null);
		}
		else
		{
			return needIPForThisRequest(tenantSession.tenant);
		}
	}

	@Override
	public
	boolean addressIsGenerallyBlocked()
	{
		final
		String ipAddress = getIpAddress();
		{
			if (ipAddress == null)
			{
				return false;
			}
		}

		final
		String possibleBanCacheHit=STASHED_BAN_IPS[ipAddress.hashCode() % BAN_STASH_SIZE];
		{
			if (possibleBanCacheHit!=null && possibleBanCacheHit.equals(ipAddress))
			{
				//Bypass repeated database accesses
				return true;
			}
		}

		final
		String possibleWhiteCacheHit=STASHED_WHITE_IPS[ipAddress.hashCode() % WHITE_STASH_SIZE];
		{
			if (possibleWhiteCacheHit!=null && possibleWhiteCacheHit.equals(ipAddress))
			{
				//Bypass repeated database accesses
				return false;
			}
		}

		final
		Object[] addressStringsToMatch;
		{
			final
			String subnetWildcard = getSubnetWildcard(ipAddress);

			log.debug("banned?({}, {})", ipAddress, subnetWildcard);

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

		//TODO: we can significantly reduce the number of fields here...
		final
		Criteria criteria=session.createCriteria(TenantIP.class)
			.add(Restrictions.in("ipAddress", addressStringsToMatch))
			.add(Restrictions.isNull("tenant"))
			;

		final
		List<TenantIP> list=criteria.list();

		for (TenantIP tenantIP : list)
		{
			if (Death.hathVisited(tenantIP))
			{
				stashBanMessage(ipAddress, Death.noteMightSay(tenantIP, null));
				return true;
			}
		}

		/*
		Now that we have incurred a database lookup to verify that this ip address is not banned, maybe we can
		avoid another db round trip for the next request?
		 */
		stashWhiteList(ipAddress);

		return false;
	}

	/**
	 * Together, these make up a sort of size-limited hash table.
	 * @512, one array might fit into a single memory page.
	 * @256, both arrays might fit into a single memory page.
	 */
	private static final
	int BAN_STASH_SIZE = 200;

	private final
	String[] STASHED_BAN_IPS = new String[BAN_STASH_SIZE];

	/**
	 * This array mirrors the STASHED_BAN_IPS array, and acts as a sort of
	 * lossy/fuzzy/best-effort map that operates in near-constant time.
	 *
	 * Note that the small possibilities of race conditions delivering the
	 * wrong message are acceptable for the possibility of some DoS respite
	 * under load.
	 */
	private final
	String[] STASHED_BAN_MESSAGES = new String[BAN_STASH_SIZE];

	private static final
	int WHITE_STASH_SIZE = 1000;

	private final
	String[] STASHED_WHITE_IPS = new String[WHITE_STASH_SIZE];

	private
	void stashWhiteList(String ipAddress)
	{
		STASHED_WHITE_IPS[ipAddress.hashCode() % WHITE_STASH_SIZE] = ipAddress;
	}

	private
	void stashBanMessage(String ipAddress, String s)
	{
		STASHED_BAN_MESSAGES[ipAddress.hashCode() % BAN_STASH_SIZE] = s;
		STASHED_BAN_IPS[ipAddress.hashCode() % BAN_STASH_SIZE] = ipAddress;
	}

	@Override
	public
	String bestEffortBanMessage()
	{
		final
		String ipAddress = getIpAddress();

		final
		String preferredBanMessage = STASHED_BAN_MESSAGES[ipAddress.hashCode() % BAN_STASH_SIZE];

		if (preferredBanMessage == null)
		{
			return IP_IS_BANNED;
		}
		else
		{
			return preferredBanMessage;
		}
	}

	@Override
	public
	String bestEffortBanMessage(HttpServletRequest httpServletRequest)
	{
		final
		String ipAddress = getIpAddress(httpServletRequest);

		final
		String preferredBanMessage = STASHED_BAN_MESSAGES[ipAddress.hashCode() % BAN_STASH_SIZE];

		if (preferredBanMessage == null)
		{
			return IP_IS_BANNED;
		}
		else
		{
			return preferredBanMessage;
		}
	}

	@Override
	public
	void run()
	{
		log.trace("flushing banned ip addresses");

		for (int i = 0; i < BAN_STASH_SIZE; i++)
		{
			STASHED_BAN_IPS[i] = null;
			STASHED_BAN_MESSAGES[i] = null;
		}

		for (int i = 0; i < WHITE_STASH_SIZE; i++)
		{
			STASHED_WHITE_IPS[i] = null;
		}
	}
}
