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

	static final String JSON_OBJECT_2k = "VARCHAR(2000) DEFAULT '{}'";
	static final String JSON_OBJECT_25k = "VARCHAR(25000) DEFAULT '{}'";

	static final String ZERO_INTEGER = "INTEGER DEFAULT 0";

	static final String FALSE_BOOLEAN = "BOOLEAN DEFAULT 'f'";
}
