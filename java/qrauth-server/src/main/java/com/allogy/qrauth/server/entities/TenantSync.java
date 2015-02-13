package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

import org.hibernate.annotations.Index;

/**
 * Created by robert on 2/13/15.
 */
@Entity
public
class TenantSync
{
	@Id
	@NonVisual
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	@Override
	public
	String toString()
	{
		return "["+getClass().getSimpleName() + ":" + id + "]";
	}

	@ManyToOne(optional = false)
	public Tenant tenant;

	@Column(nullable = false, columnDefinition = Usual.TIMESTAMP)
	public Date effectiveTime;

	@Column(nullable = false)
	public String userName;

	@Column(nullable = false)
	public String userEmail;

	@Column(nullable = false)
	public String authMethod;

	@Column(nullable = false)
	public String requestString;

	@Index(name = "idx_tenantsync_clearTime")
	@Column(columnDefinition = Usual.TIMESTAMP)
	public Date clearTime;

}
