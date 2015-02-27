package com.allogy.qrauth.server.pages.user.credentials;

import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.pages.user.Credentials;
import com.allogy.qrauth.server.services.Journal;
import org.apache.tapestry5.ioc.annotations.Inject;

/**
 * Created by robert on 2/26/15.
 */
public
class EditCredentials extends AbstractCredentialsPage
{
	@Inject
	private
	Journal journal;

	boolean onValidate()
	{
		return (userAuth.id!=null);
	}

	Object onSuccess()
	{
		journal.updatedUserAuth(userAuth);
		return Credentials.class;
	}

}
