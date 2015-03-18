package com.allogy.qrauth.server.pages.user;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

/**
 * Answers the question, "where do we send the user now", and can be
 * represented as a static link or called directly.
 *
 * Created by robert on 3/18/15.
 */
public
class ContinueUser extends AbstractUserPage
{
	public
	Object toNextTenantSessionStep()
	{
		log.error("tenant session continue step is unimplemented");
		return ActivityUser.class;
	}

	@Inject
	private
	Logger log;

	private
	Object onActivate()
	{
		return toNextTenantSessionStep();
	}
}
