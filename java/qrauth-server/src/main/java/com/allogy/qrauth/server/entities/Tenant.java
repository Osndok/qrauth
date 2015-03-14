package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Date;

/**
 * Created by robert on 2/13/15.
 */
@Entity
public
class Tenant extends Attemptable implements Mortal
{
	@Column(nullable = false)
	public String requestedName;

	@Column(unique = true)
	public String name;

	@Column(unique = true)
	public String nameRedux;

	@Column(unique = true)
	public String url;

	@Column(unique = true)
	public String urlRedux;

	/**
	 * Since (ultimately, in theory) we are going to allow anyone to create a Tenant account...
	 * We don't *expected* that any special info will be leaked to tenants, but...
	 * We should probably at least track the origin ip, for accountability.
	 */
	@ManyToOne
	public TenantIP tenantIP;

	@ManyToOne
	public DBUser primaryContact;

	@Column(nullable = false, unique = true)
	public String hashedApiKeyPrimary;

	@Column(nullable = false, unique = true)
	public String hashedApiKeySecondary;

	@Column(unique = true)
	public String unhashedShellKey;

	/**
	 * True if (and only if) the tenant accepts previously-unseen users. False indicates that no TenantUser records
	 * should be automatically fabricated for this tenant.
	 */
	@Column(nullable = false, columnDefinition = Usual.TRUE_BOOLEAN)
	public boolean newUsers;

	/**
	 * True if (and only if) the tenant is unable to process users that have no username.
	 */
	@Column(nullable = false, columnDefinition = Usual.TRUE_BOOLEAN)
	public boolean requireUsername;

	/**
	 * True if (and only if) the tenant is unable to process usernames that change.
	 */
	@Column(nullable = false, columnDefinition = Usual.TRUE_BOOLEAN)
	public boolean fixedUsername;

	@Column(unique = true)
	public String qrauthHostAndPort;

	@Column(nullable = false, columnDefinition = Usual.JSON_OBJECT_2k, length = 2000)
	public String config;

	@Column(nullable = false, columnDefinition = Usual.JSON_OBJECT_25k, length = 25000)
	public String fieldDescriptionsJson;

	@Column(nullable = false, columnDefinition = Usual.JSON_OBJECT_25k, length = 25000)
	public String permissionsDescriptionsJson;

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
