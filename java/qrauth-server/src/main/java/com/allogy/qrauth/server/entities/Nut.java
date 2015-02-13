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
class Nut extends Attemptable implements Mortal
{
	/**
	 * Pursuant to the SQRL specification, we must be able to remember and compare the originating ip address.
	 */
	@ManyToOne(optional = false)
	public TenantIP tenantIP;

	@Column(unique = true)
	public String stringValue;

	@ManyToOne
	public Tenant tenant;

	@ManyToOne
	public DBUser DBUser;

	public
	boolean isClaimable()
	{
		return (DBUser == null
					&& attempts < 10
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
