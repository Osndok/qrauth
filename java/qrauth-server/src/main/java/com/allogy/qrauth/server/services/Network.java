package com.allogy.qrauth.server.services;

import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.entities.TenantIP;

import java.util.Collection;

/**
 * Created by robert on 2/18/15.
 */
public
interface Network
{
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
	 */
	Collection<TenantIP> getExistingTenantIPsForThisOriginator();
}
