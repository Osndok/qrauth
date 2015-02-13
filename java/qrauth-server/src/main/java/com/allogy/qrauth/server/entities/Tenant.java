package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Date;

/**
 * Created by robert on 2/13/15.
 */
@Entity
public
class Tenant extends Attemptable implements Mortal
{

	@Column(unique = true)
	public String name;

	@Column(unique = true)
	public String nameRedux;

	@Column(unique = true)
	public String url;

	@Column(unique = true)
	public String urlRedux;

	@ManyToOne
	public Person contact;

	@Column(nullable = false, unique = true)
	public String hashedApiKeyPrimary;

	@Column(nullable = false, unique = true)
	public String hashedApiKeySecondary;

	@Column(unique = true)
	public String unhashedShellKey;

	@Column(nullable = false, columnDefinition = Usual.FALSE_BOOLEAN)
	public boolean anonRegister;

	@Column(unique = true)
	public String qrauthHostAndPort;

	@Column(nullable = false, columnDefinition = Usual.JSON_OBJECT_25k, length = 25000)
	public String fieldDescriptionsJson;

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
