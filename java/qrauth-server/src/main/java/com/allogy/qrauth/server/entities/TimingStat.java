package com.allogy.qrauth.server.entities;

import org.apache.tapestry5.beaneditor.NonVisual;

import javax.persistence.*;
import java.util.Date;

/**
 * A place to persist measurements of our own performance, in the hopes of alluding timing attacks.
 */
@Entity
public
class TimingStat
{
	@Id
	@NonVisual
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Integer id;

	@Column(nullable = false, columnDefinition = Usual.INSERT_TIME, insertable = false, updatable = false)
	public Date created;

	@Column(nullable = false, unique = true)
	public String name;

	public long min;
	public long max;
	public long count;
	public long sum;

	@Column(nullable = false, columnDefinition = Usual.CSV_2k, length = Usual.TIMING_STAT_CSV_SIZE)
	public String recent_csv;
}
