package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

/**
 * User: robert
 * Date: 2015/02/13
 * Time: 9:41 AM
 */
@Entity
public
class Person extends Attemptable implements Mortal
{

	@NonVisual
	@Column(nullable=false, columnDefinition = Usual.TIMESTAMP)
	public Date globalLogout;

	public String displayName;

	@Column(nullable=false, unique=true)
	public String username;

	@Column(unique=true)
	public String verifiedEmail;

	@NonVisual
	@Column(nullable = false, columnDefinition = Usual.JSON_OBJECT_2k, length = 2000)
	public String preferencesJson;

	@Override
	public
	String toString()
	{
		return "["+getClass().getSimpleName() + ":" + id + "]";
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
