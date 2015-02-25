package com.allogy.qrauth.server.services.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by robert on 2/18/15.
 */
public
class Config
{
	private static final String QRAUTH_CONFIG_FILE = System.getProperty("QRAUTH_CONFIG_FILE", "/etc/qrauth.props");

	private static Config INSTANCE;

	public static synchronized
	Config get()
	{
		if (INSTANCE==null)
		{
			try
			{
				INSTANCE = new Config();
			}
			catch (IOException e)
			{
				throw new AssertionError(e);
			}
		}

		return INSTANCE;
	}

	private final
	Properties properties;

	private
	Config() throws IOException
	{
		final
		InputStream in = new FileInputStream(QRAUTH_CONFIG_FILE);

		try
		{
			properties = new Properties();
			properties.load(in);
		}
		finally
		{
			in.close();
		}
	}

	/* package access, for hibernate configuration */
	Properties getProperties()
	{
		return properties;
	}

	/**
	 * A bit kludgy, but convenient to be here b/c the migrator and hibernate need the external database password, etc.
	 * @return
	 */
	public
	String getTapestryHMACPassphrase()
	{
		return properties.getProperty("tapestry.hmac-passphrase", "");
	}

	public
	String getHashingPepper()
	{
		return properties.getProperty("hashing.pepper", "");
	}

	public
	Long getSupremeTenantID()
	{
		final
		String supremeTenant=properties.getProperty("supreme.tenant");

		if (supremeTenant==null)
		{
			return null;
		}
		else
		{
			return Long.parseLong(supremeTenant);
		}
	}

	public
	String getCookieName()
	{
		return properties.getProperty("cookie.name", "qrauth");
	}

	public
	String getCookieHmac()
	{
		return properties.getProperty("cookie.hmac", "");
	}
}
