package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.services.DatabaseMigrator;
import org.apache.tapestry5.hibernate.HibernateConfigurer;
import org.apache.tapestry5.ioc.annotations.EagerLoad;
import org.flywaydb.core.Flyway;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by robert on 2/16/15.
 */
@EagerLoad
public
class DatabaseMigratorImpl implements DatabaseMigrator
{
	private static final String  QRAUTH_CONFIG_FILE = System.getProperty("QRAUTH_CONFIG_FILE", "/etc/qrauth.props");
	private static final boolean DB_NO_MIGRATE      = Boolean.getBoolean("DB_NO_MIGRATE");
	private static final boolean DB_MIGRATE_FATAL   = Boolean.getBoolean("DB_MIGRATE_FATAL");

	private static final
	Properties properties;

	static
	{
		try
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
		catch (IOException e)
		{
			//Failure to configure the database should be fatal, and we need the connection information before we can even hope to do so.
			throw new AssertionError(e);
		}
	}

	private static
	boolean migrationSuccessful;

	/**
	 * A bit kludgy, but convenient to be here b/c the migrator and hibernate need the external database password, etc.
	 * @return
	 */
	public static
	String getTapestryHMACPassphrase()
	{
		return properties.getProperty("tapestry.hmac-passphrase", "");
	}

	public static
	class PropertiesHandoff implements HibernateConfigurer
	{
		private final
		boolean productionMode;

		public
		PropertiesHandoff(boolean productionMode)
		{
			this.productionMode = productionMode;
		}

		@Override
		public
		void configure(Configuration configuration)
		{
			try
			{
				kludgy_do_database_migration(productionMode, properties);

				configuration.addProperties(properties);
			}
			catch (IOException e)
			{
				log.error("failure in database migration logic", e);
			}
		}
	}

	private static
	void kludgy_do_database_migration(final boolean productionMode, final Properties p) throws IOException
	{
		final
		String prefix = "hibernate.connection.";

		String url = p.getProperty(prefix + "url");
		String username = p.getProperty(prefix + "username");
		String password = p.getProperty(prefix + "password");

		String driver_class = p.getProperty(prefix + "driver_class");

		try
		{
			Class.forName(driver_class);
		}
		catch (ClassNotFoundException e)
		{
			log.error("bad {}driver_class", prefix, driver_class);
		}

		if (DB_NO_MIGRATE) return;

		log.debug("checking to see if database schema needs to be migrated");

		try
		{
			Flyway flyway = new Flyway();
			flyway.setDataSource(url, username, password);
			flyway.migrate();

			migrationSuccessful = true;
		}
		catch (RuntimeException e)
		{
			if (DB_MIGRATE_FATAL || !productionMode)
			{
				throw e;
			}
			else
			{
				log.error("unable to migrate database", e);
				migrationSuccessful = false;
			}
		}

	}

	private static final Logger log = LoggerFactory.getLogger(DatabaseMigratorImpl.class);

	public
	boolean isMigrationSuccessful()
	{
		return migrationSuccessful;
	}
}
