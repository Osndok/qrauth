package com.allogy.qrauth.server.services.impl;

import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.test.PageTester;

/**
 * Created by robert on 2/18/15.
 */
public
class One
{
	public static final PageTester pageTester;
	public static final Registry   registry;

	private static
	void log(String s)
	{
		System.out.println(s);
	}

	static
	{
		System.setProperty("DB_NO_MIGRATE", "true");
		System.setProperty("tapestry.execution-mode", "DevelopmentMode");
		System.setProperty("tapestry.production-mode", "false");

		log("starting up registry");
		pageTester=new PageTester("com.allogy.qrauth.server", "App");
		registry=pageTester.getRegistry();
		//registry.performRegistryStartup();
		log("ready");
	}
}
