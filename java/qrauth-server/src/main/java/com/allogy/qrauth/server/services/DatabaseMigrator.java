package com.allogy.qrauth.server.services;

import org.apache.tapestry5.hibernate.HibernateConfigurer;

/**
 * Created by robert on 2/16/15.
 */
public
interface DatabaseMigrator
{
	boolean isMigrationSuccessful();
}
