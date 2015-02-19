package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

/**
 * At a high level, a nut is simply a blob of characters. The length and character set is defined by the
 * SQRL specification (https://www.grc.com/sqrl/server.htm), but usable in it's own right for other
 * authentication factors.
 */
@Entity
public
class Nut /* extends Attemptable */ implements Mortal
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	@Override
	public
	String toString()
	{
		return "["+getClass().getSimpleName() + ":" + id + "]";
	}

	@Column(nullable = false, columnDefinition = Usual.INSERT_TIME, insertable = false, updatable = false)
	public Date created;

	/**
	 * Pursuant to the SQRL specification, we must be able to remember and compare the originating ip address.
	 * Note that, for us, this means the tenant must provide the IP address of the original requestor; so there
	 * might actually be a bit of 3rd-party verification (network wise), or CRAM (that they have to commit to
	 * an IP address)... but all of this is a bit questionable anyway, as network addresses can (and do) change,
	 * especially on a mobile device.
	 */
	@ManyToOne(optional = false)
	public TenantIP tenantIP;

	@Column(nullable = false, unique = true, length = 30)
	public String stringValue;

	@ManyToOne
	public Tenant tenant;

	@ManyToOne
	public DBUser user;

	public
	boolean isClaimable()
	{
		return (user == null
				/*	&& attempts < 10 */
					&& (deadline == null || deadline.getTime() > System.currentTimeMillis())
		);
	}

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
