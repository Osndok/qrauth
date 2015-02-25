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
	public DBUserAuth userAuth;

	@ManyToOne
	public Username username;

	@ManyToOne
	public Tenant tenant;

	@ManyToOne
	public TenantIP tenantIP;

	@ManyToOne
	public TenantSession tenantSession;

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

	/**
	 * The time at which this LogEntry looses substantial relevance. For example, if the log entry is concerning
	 * (or limited to) a particular session, the deadline might be that session's deadline... that we might
	 * (by default) show the user a list of could-be-active sessions.
	 */
	@Column(columnDefinition = Usual.TIMESTAMP)
	public Date deadline;

}
