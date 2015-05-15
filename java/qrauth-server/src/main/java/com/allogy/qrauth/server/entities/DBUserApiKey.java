package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Date;

/**
 * As opposed to tenant API keys, which allow API access to the qrauth application, these keys
 * are intended for non-tenants (users) to access the API of the *Tenant*.
 */
@Entity
public
class DBUserApiKey extends Attemptable implements Mortal
{
	@ManyToOne(optional = false)
	public DBUser user;

	public String name;

	public String description;

	@Column(nullable = false, unique = true)
	public String hashedApiKey;

	/**
	 * For some applications, the user might opt to have the *UNHASHED* api key so that they can
	 * come back into the system to reference it. A security trade off, to be sure.
	 */
	public String unhashedApiKey;

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
