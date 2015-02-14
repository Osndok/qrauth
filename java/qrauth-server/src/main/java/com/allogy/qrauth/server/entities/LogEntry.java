package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by robert on 2/13/15.
 */
@Entity
public
class LogEntry
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

	@Column(nullable = false, columnDefinition = Usual.INSERT_TIME, updatable = false)
	public Date time;

	@ManyToOne
	public DBUser user;

	@ManyToOne
	public Method method;

	@ManyToOne
	public Tenant tenant;

	@NonVisual
	@Column(nullable = false, length = 25)
	public String actionKey;

	@Column(nullable = false)
	public String message;

	@Column(nullable = false, columnDefinition = Usual.FALSE_BOOLEAN)
	public boolean userSeen;

	@Column(nullable = false, columnDefinition = Usual.FALSE_BOOLEAN)
	public boolean tenantSeen;

	@Column(nullable = false, columnDefinition = Usual.FALSE_BOOLEAN)
	public boolean important;
}
