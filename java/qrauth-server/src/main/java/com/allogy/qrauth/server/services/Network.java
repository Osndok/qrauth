package com.allogy.qrauth.server.services;

import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.entities.TenantIP;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

/**
 * Created by robert on 2/18/15.
 */
public
interface Network
{
	public static final String IP_IS_BANNED="not allowed from your computer";

	String getIpAddress();

	/**
	 * Returns the most relevant TenantIP, even if it has to create it.
	 *
	 * In order:
	 * (1) any dead/banned records,
	 * (2) tenant-specific single-ip records
	 * (3) tenant-specific subnet-records
	 * (4) general single-ip records
	 * (5) general subnet records
	 *
	 * @param tenantFilter - if provided, will loop-in ip-records specific to the supplied tenant
	 * @return TenantIp, or null if ip information for this request is unavailable
	 */
	TenantIP needIPForThisRequest(Tenant tenantFilter);

	/**
	 * Returns all matching records for the given ip address, across all tenants.
	 * @return
	 * @param andTenant
	 */
	Collection<TenantIP> getExistingTenantIPsForThisOriginator(Tenant andTenant);
	Collection<TenantIP> getExistingTenantIPsForThisOriginatorAllTenants();

	/**
	 * If this function returns true, then the authentication provider has decided to blacklist the ip address
	 * of the current request. Further processing should be halted, and more information might be available
	 * in the low-contention bestEffortBanMessage() function.
	 *
	 * @return true if (and only if) the provided address has a matching-but-dead TenantIP record in the database with a null tenant field
	 */
	boolean addressIsGenerallyBlocked();

	/**
	 * Called immediately after addressIsGenerallyBlocked() to acquire the side-effect-stashed ban message.
	 *
	 * @return a user-presentable string that *might* contain a little more information that simply 'banned'.
	 */
	String bestEffortBanMessage();
	String bestEffortBanMessage(HttpServletRequest httpServletRequest);

	/**
	 * This is a low-overhead opportunistic variant of addressIsGenerallyBlocked() which will never cause a
	 * database lookup. Therefore, it is suitable for use in the low level per-request filters.
	 *
	 * @return true only if the provided address has a matching-but-dead TenantIP record in the database with a null tenant field
	 */
	boolean addressCacheShowsBan(HttpServletRequest request);

}
