package com.allogy.qrauth.server.services;

import com.allogy.qrauth.server.entities.*;

import java.util.Collection;
import java.util.Date;

/**
 * Created by robert on 2/18/15.
 */
public
interface Journal
{
	void noticeAttempt(Attemptable attemptable);
	void noticeSuccess(Attemptable attemptable);
	void incrementSuccess(Attemptable attemptable);

	void noticeAttempt(Collection<? extends Attemptable> attemptables);
	void incrementSuccess(Collection<? extends Attemptable> attemptables);

	void createdUserAccount(DBUserAuth userAuth, Username username, TenantSession tenantSession);
	void authenticatedUser(DBUserAuth userAuth, Username username, TenantSession tenantSession, Date deadline);

	void updatedUserAuth(DBUserAuth userAuth);
	void revokedUserAuth(DBUserAuth userAuth);

	void transferredUserAuth(DBUserAuth userAuth, DBUser user);

	void addedUserAuthCredential(DBUserAuth userAuth);

	void allocatedUsername(Username username);

	void revokedUsername(Username username);

	void newTenantAccountCreated(TenantUser tenantUser);

	void newTenantUser(TenantUser tenantUser, TenantSession tenantSession, TenantIP tenantIP);
}
