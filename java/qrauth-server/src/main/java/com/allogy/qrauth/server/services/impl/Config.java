package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.entities.Tenant;
import com.yubico.client.v2.YubicoClient;
import org.apache.tapestry5.services.Request;

import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by robert on 2/18/15.
 */
public
class Config
{
	private static final String QRAUTH_CONFIG_FILE = System.getProperty("QRAUTH_CONFIG_FILE", "/etc/qrauth.props");

	private static
	Config INSTANCE;

	public static synchronized
	Config get()
	{
		if (INSTANCE == null)
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

	public
	String getSqrlServerFriendlyName(Request request, Tenant tenant)
	{
		final
		String bestSqrlProviderName;
		{
			/**
			 * "main" is the possibly-longer, standalone identifier; such as "Allogy Interactive", or "Gibson Research Corporation"
			 */
			final
			String main = getProperty("sqrl.sfn.main");

			/**
			 * "short" or "shorter" is the possibly-shorter, identifier; such as "Allogy", or "GRC".
			 */
			final
			String shorter = properties.getProperty("sqrl.sfn.short", getProperty("sqrl.sfn.shorter"));

			if (shorter!=null && tenant!=null)
			{
				bestSqrlProviderName=shorter;
			}
			else
			if (main!=null)
			{
				bestSqrlProviderName=main;
			}
			else
			{
				final
				String hostHeaderOfArbitraryRequest = request.getHeader("Host");

				if (hostHeaderOfArbitraryRequest == null || hostHeaderOfArbitraryRequest.length() == 0)
				{
					//NB: waits for another (non-api?) request with a host header
					if (tenant == null)
					{
						return "Unknown";
					}
					else
					{
						return presentableTenantIdentification(tenant);
					}
				}
				else
				{
					//TODO: remove all but registered and top-level domains from 'host' header.
					bestSqrlProviderName = hostHeaderOfArbitraryRequest;
				}
			}
		}

		if (tenant==null)
		{
			return bestSqrlProviderName;
		}
		else
		{
			final
			String suffix = properties.getProperty("sqrl.sfn.suffix", " (via "+bestSqrlProviderName+")");

			return presentableTenantIdentification(tenant)+suffix;
		}
	}

	/**
	 * TODO: is there a clean way to have this under the 'policy' service?
	 * @param tenant
	 * @return
	 */
	public
	String presentableTenantIdentification(Tenant tenant)
	{
		if (tenant.name==null)
		{
			//NB: our policy is that we cannot display unapproved user-provided possibly-duplicate tenant names.
			return tenant.toString()+" [name pending]";
		}
		else
		{
			return tenant.name;
		}
	}

	/**
	 * This is used by the PPP system to ensure it does not give a value that would be much too old to plausibly
	 * be a true account in this system.
	 *
	 * @return
	 */
	public
	long getOriginationTime()
	{
		final
		String stringValue=properties.getProperty("origin.time");

		if (stringValue==null)
		{
			return 1425671757000l;
		}
		else
		{
			return Long.parseLong(stringValue);
		}
	}

	public
	String getProperty(String name)
	{
		return properties.getProperty(name);
	}

	private
	SecretKeySpec hmacSha1SigningKey;

	public
	SecretKeySpec getHmacSha1SigningKey()
	{
		if (hmacSha1SigningKey==null)
		{
			hmacSha1SigningKey=new SecretKeySpec(getHashingPepper().getBytes(), "HmacSHA1");
		}
		return hmacSha1SigningKey;
	}
}
