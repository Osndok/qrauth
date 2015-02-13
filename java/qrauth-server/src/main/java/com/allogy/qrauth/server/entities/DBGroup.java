package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

/**
 * This would be called aGroup, but Groupis a reserved word (at least in PostgreSQL).
 * It's easiest to just pick a synonym to replace it... crowd, gang, gathering...
 * Or a non-intrusive prefix or suffix.
 */
@Entity
public
class DBGroup implements Mortal
{
	@Id
	@NonVisual
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Integer id;

	@Override
	public
	String toString()
	{
		return "["+getClass().getSimpleName() + ":" + id + "]";
	}

	@Column(nullable = false, columnDefinition = Usual.INSERT_TIME, insertable = false, updatable = false)
	public Date created;

	@Column(nullable = false)
	public String name;

	@Column(nullable = false, unique = true)
	public String nameRedux;

	/**
	 * If null, that indicates that this is a public/well-known (or diassociated) group; otherwise
	 * this is the tenant that created (and thus owns/controls/can-update) this group. Note that
	 * (counter-intuitively) if group definitions are shared, even the owner cannot add or evict
	 * people from another tenant's group.
	 */
	@ManyToOne
	public Tenant owner;

	public
	boolean isPublicGroup()
	{
		return (owner==null);
	}

	/**
	 * If true, then this group's nameRedux will *directly* translate into a shell group (and thus carry
	 * with it very serious permissions) if ever a user with shell-access logs into a system synchronized
	 * with this qrauth service. Note that this can only happen if (1) the tenant marks the user as
	 * 'shellAccess=true', and (2) the tenant adds the user to this group.
	 *
	 * WARNING: this flag is *SERIOUS*, and carries a variable weight based usually on the convention of
	 * the group's nameRedux. For example 'wheel' might mean that the user has SUDO/ROOT access!!!!
	 */
	@Column(nullable = false, columnDefinition = Usual.FALSE_BOOLEAN)
	public boolean shellGroup;

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
