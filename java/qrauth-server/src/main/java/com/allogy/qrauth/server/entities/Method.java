package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.lang.invoke.MethodType;
import java.util.Date;

/**
 * Created by robert on 2/13/15.
 */
@Entity
public
class Method extends Attemptable implements Mortal
{

	@ManyToOne(optional = false)
	public DBUser DBUser;

	/**
	 * The maximum number of milliseconds that this authentication method will sustain a session from
	 * the time of it's clearance, or null indicating 'unlimited'.
	 */
	@NonVisual
	public Integer millisGranted;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	public MethodType type;

	public String secret;
	public String comment;

	/**
	 * A place to stash SQRL's id-lock challange.
	 */
	public String lock;

	@Column(unique = true, length = 2048)
	public String pubKey;

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
