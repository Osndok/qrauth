package com.allogy.qrauth.server.services;

import com.allogy.qrauth.server.entities.Attemptable;

/**
 * Created by robert on 2/18/15.
 */
public
interface Journal
{
	void noticeAttempt(Attemptable attemptable);
	void noticeSuccess(Attemptable attemptable);

}
