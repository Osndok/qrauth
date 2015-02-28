package com.allogy.qrauth.server.services;

import com.allogy.qrauth.server.entities.DBUser;

/**
 * Created by robert on 2/18/15.
 */
public
interface Policy
{
	boolean allowsAnonymousCreationOfNewTenants();

	long getGlobalLogoutPeriod();

	long getShortestUsableSessionLength();

	boolean wouldAllowUsernameToBeRegistered(String username);

	String usernameMatchFilter(String userInput);

	boolean wouldAllowAdditionalUsernames(DBUser user, boolean extraEffort);
}
