package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by robert on 2/13/15.
 */
@MappedSuperclass
public abstract
class Attemptable
{
	/**
	 * id is provided in the Attemptable superclass so that we have the option of a low-contest background update
	 * mechanism if we find no compelling security reason to use a blocking update, and scaling seems to require it.
	 */
	@Id
	@NonVisual
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Integer id;

	@Column(nullable = false, columnDefinition = Usual.INSERT_TIME, insertable = false, updatable = false)
	public Date created;

	@NonVisual
	@Column(columnDefinition = Usual.TIMESTAMP)
	public Date lastAttempt;

	@NonVisual
	@Column(columnDefinition = Usual.TIMESTAMP)
	public Date lastSuccess;

	@NonVisual
	@Column(nullable = false, columnDefinition = Usual.ZERO_INTEGER)
	public int attempts;

	@NonVisual
	@Column(nullable = false, columnDefinition = Usual.ZERO_INTEGER)
	public int successes;

}
