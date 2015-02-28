package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * User: robert
 * Date: 2015/02/13
 * Time: 9:41 AM
 */
@Entity
public
class DBUser extends Attemptable implements Mortal
{

	@NonVisual
	@Column(nullable = false, columnDefinition = Usual.TIMESTAMP)
	public Date globalLogout;

	public String displayName;

	@Column(unique = true)
	public String verifiedEmail;

	@NonVisual
	@Column(nullable = false, columnDefinition = Usual.JSON_OBJECT_2k, length = 2000)
	public String preferencesJson;

	@ManyToOne(optional = true)
	public TenantIP lastLoginIP;

	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
	public List<DBUserAuth> authMethods;

	@OneToMany(mappedBy = "user")
	public List<Username> usernames;

	/**
	 * One day, we might decide to finish building this out by checking the user-provided
	 * epoch against the database version, thus truly supporting universal logout (as that
	 * would increment the epoch). But this might add an additional db round trip for
	 * every request! ...and have only a marginal security effect.
	 */
	@Column(nullable = false, columnDefinition = Usual.ZERO_INTEGER)
	public int epoch;

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
