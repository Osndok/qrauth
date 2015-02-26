package com.allogy.qrauth.server.pages.user.credentials;

import com.allogy.qrauth.server.pages.user.Credentials;
import com.allogy.qrauth.server.services.Journal;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;

import java.util.Date;

/**
 * Created by robert on 2/26/15.
 */
public
class RevokeCredentials extends AbstractCredentialsPage
{
	@Property
	private
	String memo;

	@Inject
	private
	Journal journal;

	boolean onValidate()
	{
		return (userAuth.id != null);
	}

	Object onSuccess()
	{
		userAuth.deadline=new Date();
		userAuth.deathMessage=memo;
		session.save(userAuth);

		journal.revokedUserAuth(userAuth);

		return Credentials.class;
	}
}
