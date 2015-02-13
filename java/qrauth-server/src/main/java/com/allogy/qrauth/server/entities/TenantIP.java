package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

/**
 * Provides a place to remember IP-level bans that the Tenants have requested.
 *
 * TenantIPs (as it's name implies TenantIPs are generally tenant-specific, but not always! Operations
 * that have no effecting tenant may produce TenantIPs without a tenant, and the operator of the
 * authentication system might deliberately insert records with a null tenant field to indicate
 * that an ip (or subnet) should be banned (or counted).
 *
 * If another tenant is considering the ban of an ip address, then we can probably share the
 * ban-cause (deathMessage) any other TenantIPs (those not in general or beloging to him),
 * which might help to ease or raise their concern about banning someone known to be banned
 * by others.
 *
 * Basic subnet-level banning should also be supported (for ipv4, anyway), by substituting an
 * astrix for the last numeral group.
 *
 * TODO: we should develop a mechanism whereby the authentication provider can notice repeat
 * offenders, and generally ban them; or notice the need for a ban on a wider-scope.
 */
@Entity
public
class TenantIP extends Attemptable implements Mortal
{
	@Override
	public
	String toString()
	{
		return "[" + getClass().getSimpleName() + ":" + id + "="+ipAddress+"]";
	}

	@org.hibernate.annotations.Index(name = "idx_tenant_ip")
	@Column(nullable = false, length = Usual.IP_ADDRESS)
	public String ipAddress;

	/**
	 * If null, then this record was create in the absence of a Tenant (or to cover all Tenants).
	 * In such a case, the counters should still be effected, and a ban/death should still be
	 * considered, but a tenant is unable to directly actuate (he/she must make a parallel
	 * TenantIP record.
	 */
	@ManyToOne(optional = true)
	public Tenant tenant;

	/* -------------------- Mortal Implementation ------------------ */

	@NonVisual
	public String deathMessage;

	@NonVisual
	@Column(columnDefinition = Usual.TIMESTAMP)
	public Date deadline;

	@Override
	public
	String getDeathMessage()
	{
		return deathMessage;
	}

	@Override
	public
	Date getDeadline()
	{
		return deadline;
	}

	/* ------------------------------------------------------------- */
}
