package com.allogy.qrauth.server.services.impl;

import com.yubico.client.v2.YubicoClient;

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

		{
			final
			String yubiSecret=_getYubicoApiSecret();

			final
			Integer yubiId=_getYubicoApiId();

			if (yubiSecret==null || yubiId==null)
			{
				yubicoClientCachingFactory = null;
			}
			else
			{
				yubicoClientCachingFactory = new ThreadLocal<YubicoClient>()
				{
					@Override
					protected
					YubicoClient initialValue()
					{
						return YubicoClient.getClient(yubiId, yubiSecret);
					}
				};
			}
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
	String getCookieDomain()
	{
		return properties.getProperty("cookie.domain", null);
	}

	public
	String getCookiePath()
	{
		return properties.getProperty("cookie.path", "/");
	}

	public
	String getBrandName()
	{
		return properties.getProperty("brand.name", "qrauth");
	}

	public
	String getBrandLink()
	{
		return properties.getProperty("brand.link", "https://github.com/Osndok/qrauth");
	}

	private
	Integer _getYubicoApiId()
	{
		return _integer("yubico.api.id", null);
	}

	private
	String _getYubicoApiSecret()
	{
		return properties.getProperty("yubico.api.secret", null);
	}

	private
	Integer _integer(String key, Integer _default)
	{
		final
		String stringValue=properties.getProperty(key);

		if (stringValue==null)
		{
			return _default;
		}
		else
		{
			return Integer.parseInt(stringValue);
		}
	}

	private final
	ThreadLocal<YubicoClient> yubicoClientCachingFactory;

	/**
	 * @return a YubicoClient that will validate against the public yubi-cloud, or null if there is no api key
	 */
	public
	YubicoClient getYubicoClient()
	{
		if (yubicoClientCachingFactory == null)
		{
			return null;
		}
		else
		{
			return yubicoClientCachingFactory.get();
		}
	}
}
