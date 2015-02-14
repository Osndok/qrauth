package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by robert on 2/13/15.
 */
@Entity
public
class TenantGroupMember
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

	@ManyToOne(cascade = CascadeType.REMOVE, optional = false)
	public TenantGroup tenantGroup;

	@ManyToOne(cascade = CascadeType.REMOVE, optional = false)
	public DBUser user;

}
