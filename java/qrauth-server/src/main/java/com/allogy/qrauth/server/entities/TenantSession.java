package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

import org.hibernate.annotations.Index;

/**
 * Created by robert on 2/13/15.
 */
@Entity
public
class TenantSession implements Mortal
{
	@Id
	@NonVisual
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	@Override
	public
	String toString()
	{
		return "["+getClass().getSimpleName() + ":" + id + "]";
	}

	/**
	 * Ordinarily, this sort of field would be called 'created', but 'noticed' is used here to further
	 * emphasis that we are not the SoA for our Tenant's sessions; we only 'notice' them when they call-in.
	 */
	@Column(nullable = false, columnDefinition = Usual.INSERT_TIME, insertable = false, updatable = false)
	public Date noticed;

	@ManyToOne(optional = false)
	public Tenant tenant;

	/**
	 * The ban-able network address (and any related counters), etc. therefor.
	 */
	@ManyToOne(optional = false)
	public TenantIP tenantIP;

	/**
	 * This is the session_id (technically unhashed), exactly as the tenant provides it. It is expected and
	 * encouraged that the tenant obfuscate the session_id before ever handing it to us, but we leave it
	 * defined on the tenant-side, in case they need to be able to lookup unhashed session ids.
	 *
	 * Furthermore, failure (of the tenant) to provide differentiating session ids (e.g. if they always provide
	 * the same one) will likely result in undefined behavior; although, we will do the best we can to help.
	 */
	@Index(name = "idx_tenantsession_id")
	@Column(nullable = false)
	public String session_id;

	/**
	 * Returns the Person that is connected with this Tenant's session, or null if we do not yet know
	 * who (if anyone) it should be linked with.
	 *
	 * WARNING: The fact of this field being non-null is sufficient to (at least temporarily) authenticate a Person!
	 */
	@ManyToOne(optional = true)
	public DBUser user;

	/**
	 * The time that we noticed who is associated with this session. The null-ness of this field should always match
	 * the null-ness of the person (person_id) field.
	 */
	@Column(nullable = true, columnDefinition = Usual.TIMESTAMP)
	public Date connected;

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
