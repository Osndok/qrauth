package com.allogy.qrauth.server.pages.user.names;

import com.allogy.qrauth.server.pages.user.AbstractUserPage;
import org.apache.tapestry5.annotations.PageActivationContext;

/**
 * Created by robert on 2/27/15.
 */
public
class AddNames extends AbstractUserPage
{
	@PageActivationContext
	private
	String hint;

	public
	AddNames withHint(String hint)
	{
		this.hint=hint;
		return this;
	}
}
