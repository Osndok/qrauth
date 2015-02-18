package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Date;

/**
 * Usernames are managed independently of Users (or Persons) so that users can
 * reserve (or retrire) a small number of psuedonyms, use different psuedonyms
 * for different tenants, etc.
 */
@Entity
public
class Username extends Attemptable implements Mortal
{
	@ManyToOne
	public DBUser user;

	@Column(nullable = true, unique = true)
	public String stringValue;

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
