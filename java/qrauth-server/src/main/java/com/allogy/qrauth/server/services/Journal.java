package com.allogy.qrauth.server.services;

import com.allogy.qrauth.server.entities.Attemptable;
import com.allogy.qrauth.server.entities.TenantIP;

import java.util.Collection;

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
}
