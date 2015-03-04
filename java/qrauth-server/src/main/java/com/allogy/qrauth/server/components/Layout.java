package com.allogy.qrauth.server.components;

import com.allogy.qrauth.server.services.AuthSession;
import com.allogy.qrauth.server.services.impl.Config;
import org.apache.tapestry5.*;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.ioc.annotations.*;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.SymbolConstants;

/**
 * Layout component for pages of application test-project.
 */
@Import(module="bootstrap/collapse", stylesheet = "context:main.css")
public class Layout
{
	@Inject
	private ComponentResources resources;

	/**
	 * The page title, for the <title> element and the <h1> element.
	 */
	@Property
	@Parameter(required = true, defaultPrefix = BindingConstants.LITERAL)
	private String title;

	@Property
	private String pageName;

	@Property
	@Inject
	@Symbol(SymbolConstants.APPLICATION_VERSION)
	private String appVersion;

	public
	boolean isLoggedIn()
	{
		return authSession.isLoggedIn();
	}

	@Inject
	private
	AuthSession authSession;

	private static final String BRAND_LINK = Config.get().getBrandLink();
	private static final String BRAND_NAME = Config.get().getBrandName();

	public
	String getBrandLink()
	{
		return BRAND_LINK;
	}

	public
	String getBrandName()
	{
		return BRAND_NAME;
	}

	@Inject
	private
	ComponentResources componentResources;

	public
	String getCssClassPageName()
	{
		return componentResources.getPageName().toLowerCase().replaceAll("\\/", "_");
	}
}
