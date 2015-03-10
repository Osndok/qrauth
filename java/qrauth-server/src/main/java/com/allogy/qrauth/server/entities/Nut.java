package com.allogy.qrauth.server.entities;

import com.allogy.qrauth.server.helpers.Death;
import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

import static com.allogy.qrauth.server.entities.NutState.*;

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

	/**
	 * To aid in a graceful session handoff, and to better defend against "over the shoulder" attacks,
	 * we desire a "secret" value that is required by the underlying api calls which (while being
	 * embedded *IN* the webpage) is never rendered *ON* the webpage, neither in the QR code. This
	 * does not offer any protection against the well-known MITM attack/weakness.
	 */
	@Column(nullable = false, length = 30)
	public String semiSecretValue;

	@ManyToOne(optional = true)
	public TenantSession tenantSession;

	/**
	 * This is the hinge upon which the SQRL authentication mechanism pivots.
	 */
	@ManyToOne
	public DBUser user;

	/**
	 * This allows the SQRL subsystem to lock a nut while it is being considered by the user.
	 * If not null, then this nut is in limbo, which means that the SQRL client has requested
	 * server data, but not actually authenticated... For a bit of extra security, any web page
	 * that notices that this field becoming not-null should hide the qr code immediately.
	 */
	public String mutex;

	/**
	 * This field is provided for the connection of non-login activities to SQRL activity.
	 */
	public String command;

	public
	NutState getState()
	{
		if (Death.hathVisited(this))
		{
			if (user == null)
			{
				return FAILED;
			}
			else
			{
				return COMPLETE;
			}
		}

		if (user == null)
		{
			if (mutex == null)
			{
				return LIMBO;
			}
			else
			{
				return INIT;
			}
		}
		else
		{
			return READY;
		}
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
