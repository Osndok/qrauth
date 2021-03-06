package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

/**
 * Usernames are managed independently of Users (or Persons) so that users can
 * reserve (or retire) a small number of pseudonyms, use different pseudonyms
 * for different tenants, etc.
 */
@Entity
public
class Username extends Attemptable implements Mortal
{
	@ManyToOne
	public DBUser user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(20)", length = 20)
	public UsernameType type;

	@Column(nullable = false, unique = true)
	public String displayValue;

	@Column(nullable = false, unique = true)
	public String unixValue;

	@NonVisual
	@Column(nullable = false, unique = true)
	public String matchValue;

	/**
	 * Counterpart to the DBUserAuth silent alarm, perhaps someone would want a special
	 * auxiliary username that would trigger a silent alarm?
	 *
	 * Requires build out of (or attachment to) a good notification system tenant-side.
	 * For now, this will only result in the 'alarm' flag being set to true.
	 */
	@Column(nullable = false, columnDefinition = Usual.FALSE_BOOLEAN)
	public boolean silentAlarm;

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
