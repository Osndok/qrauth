package com.allogy.qrauth.server.services;

import com.allogy.qrauth.server.entities.DBUser;
import com.allogy.qrauth.server.entities.UsernameType;

import java.util.Date;

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

	String usernameUnixFilter(UsernameType usernameType, String userInput);

	boolean wouldAllowAdditionalUsernames(DBUser user, boolean extraEffort);

	Date passwordDeadlineGivenComplexity(double strength);

	long longestReasonableAddCredentialTaskLength();

	int hotpAdvanceMatch();

	long getMaximumSqrlHandoffPeriod();

	int getMaximumTenantsForUser(DBUser user);

	boolean isAcceptableTenantName(String name);
}
