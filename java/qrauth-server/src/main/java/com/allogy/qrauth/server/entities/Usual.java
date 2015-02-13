package com.allogy.qrauth.server.entities;

/**
 * Collects specialized DB definitions that are used in the entity objects into one place.
 *
 * Created by robert on 2/13/15.
 */
class Usual
{
	static final String INSERT_TIME = "TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP";
	static final String TIMESTAMP   = "TIMESTAMP WITHOUT TIME ZONE";

	static final String JSON_OBJECT_2k  = "VARCHAR(2000) DEFAULT '{}'";
	static final String JSON_OBJECT_25k = "VARCHAR(25000) DEFAULT '{}'";

	static final String ZERO_INTEGER = "INTEGER DEFAULT 0";

	static final String FALSE_BOOLEAN = "BOOLEAN DEFAULT 'f'";
	static final String TRUE_BOOLEAN  = "BOOLEAN DEFAULT 't'";

	static final String CSV_2k   = "VARCHAR(2000) DEFAULT ''";

	/**
	 * Enough room to store IPv4, IPv6, or *both* (tunneling).
	 * http://stackoverflow.com/questions/166132/maximum-length-of-the-textual-representation-of-an-ipv6-address
	 */
	public static final int IP_ADDRESS           = 45;
	public static final int TIMING_STAT_CSV_SIZE = 2000;
}
