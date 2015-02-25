package com.allogy.qrauth.server.services;

/**
 * Created by robert on 2/18/15.
 */
public
interface Policy
{
	boolean allowsAnonymousCreationOfNewTenants();

	long getGlobalLogoutPeriod();

	long getShortestUsableSessionLength();
}
