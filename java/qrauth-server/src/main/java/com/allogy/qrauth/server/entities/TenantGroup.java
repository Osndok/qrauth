package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by robert on 2/13/15.
 */
@Entity
public
class TenantGroup
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
	public Tenant tenant;

	@ManyToOne(cascade = CascadeType.REMOVE, optional = false)
	public DBGroup DBGroup;

	public String customName;

	@Column(nullable = false, columnDefinition = Usual.CSV_2k, length = 2000)
	public String permissionsCsv;
}
