package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;
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

	@Column(nullable = true, unique = true)
	public String username;

	@Column(unique = true)
	public String verifiedEmail;

	@NonVisual
	@Column(nullable = false, columnDefinition = Usual.JSON_OBJECT_2k, length = 2000)
	public String preferencesJson;

	@ManyToOne(optional = true)
	public TenantIP lastLoginIP;

	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
	@MapKey(name = "variety")
	public Map<MethodVariety, Method> methods;

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
