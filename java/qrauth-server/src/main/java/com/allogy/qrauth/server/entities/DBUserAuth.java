package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by robert on 2/13/15.
 */
@Entity
public
class DBUserAuth extends Attemptable implements Mortal
{

	@ManyToOne(optional = false)
	public DBUser user;

	/**
	 * The maximum number of milliseconds that this authentication method will sustain a session from
	 * the time of it's clearance, or null indicating 'unlimited'.
	 */
	@NonVisual
	public Integer millisGranted;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(20)", length = 20)
	public AuthMethod authMethod;

	@NonVisual
	public String secret;

	public String comment;

	/**
	 * A place to stash SQRL's id-lock challenge.
	 */
	@NonVisual
	public String idRecoveryLock;

	@Column(unique = true, length = 2048)
	public String pubKey;

	/**
	 * This idea is that (particularly for the yubikey, which has two operating modes:
	 * short press & long press), we can have one of them *work* yet silently raise an
	 * alarm tenant-side (or plausibly lock/disable an account). For that matter, we
	 * could set up any number of "working" one-time-passwords that would do the same.
	 *
	 * Requires build out of (or attachment to) a good notification system tenant-side.
	 * For now, this will only result in the 'alarm' flag being set to true.
	 */
	@Column(nullable = false, columnDefinition = Usual.FALSE_BOOLEAN)
	public boolean silentAlarm;

	/**
	 * If not null, this is a comma-separated list of tenant::id that is allowed access
	 * to the *SECRET* components of this authentication method. Only relevant for
	 * auth methods that are not marked as leak-safe, and only relevant for tenants
	 * which have granted this user shell-access.
	 *
	 * This should surly be a *small* list, as sharing a bunch of secrets with a large
	 * number of 3rd parties would be a bad thing.
	 */
	@NonVisual
	@Column(columnDefinition = Usual.CSV_30, length = 30)
	public String disclose_csv;

	/* -------------------- Mortal Implementation ------------------ */

	@NonVisual
	public String deathMessage;

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
