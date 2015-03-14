package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Date;

/**
 * This entity is the crux of the system, as it represents the Tenant's view of a person.
 * For example, what the Tenant knows as the numeric id (UID) is actually this TenantPerson::id
 */
@Entity
public
class TenantUser extends Attemptable implements Mortal
{
	@ManyToOne(optional = false)
	public DBUser user;

	/**
	 * If not null, this is the first (and hopefully the only) username which has been (or will be)
	 * handed off to this tenant for identification. This is more 'something to be known by', and
	 * thus will often still point to old/disabled usernames, as it is more important for the
	 * per-tenant identity to be consistent than to force all tenants to support multiple usernames.
	 */
	@ManyToOne
	public Username username;

	@ManyToOne(optional = false)
	public Tenant tenant;

	/**
	 * If true, then this Person has been blessed by this Tenant to make basic changes to their authentication.
	 * This may include seeing (and modifying) the user list, banning persons, etc.
	 */
	@Column(nullable = false, columnDefinition = Usual.FALSE_BOOLEAN)
	public boolean authAdmin;

	/**
	 * If true (and *if* the tenant has shell access setup), then this Person has been blessed to directly log into
	 * this Tenant's infrastructure (SHELL-LEVEL-ACCESS), and possibly to then make very serious changes usually
	 * left only to software developers, infrastructure engineers, and production managers.
	 *
	 * This flag will automatically be set to true for the first/primary administrator of any particular tenant.
	 *
	 * This may include shutting down and rebooting systems, directly accessing tenant databases, and without
	 * careful planning... irrevocably screwing *everything* up...
	 */
	@Column(nullable = false, columnDefinition = Usual.FALSE_BOOLEAN)
	public boolean shellAccess;

	/**
	 * This is a place where tenants can stash just about anything (so long as it falls under the size limit), and
	 * operates much like a key/value store. It is not reccomended for storing permissions (the group/permissions
	 * system is better for that), but is suitable for user flags, preferences, and even database-like fields
	 * (email addresses and what-not).
	 *
	 * In general, the Tenant has absolute control of this record, and this field in particular; but in the interest
	 * of transparency, we may allow users read-only access to this field, so they might get a peek into what a
	 * Tenant knows about them.
	 */
	@NonVisual
	@Column(nullable = false, columnDefinition = Usual.JSON_OBJECT_2k, length = 2000)
	public String configJson;

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
