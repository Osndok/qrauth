package com.allogy.qrauth.server.entities;

/**
 * Roughly equivalent to the tabs in the user interface.
 */
public
enum AuthMethodGroup
{
	QR_ONLY,
	USER_AND_PASS,
	PASS_ONLY,
	RSA_CRAM,
	PPP_CRAM,
	THIRD_PARTY
}
