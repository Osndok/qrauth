package com.allogy.qrauth.server.services;

import java.io.IOException;

import com.allogy.qrauth.server.entities.OutputStreamResponse;
import com.allogy.qrauth.server.services.filters.HighLevelBanFilter;
import com.allogy.qrauth.server.services.filters.LowLevelBanFilter;
import com.allogy.qrauth.server.services.impl.*;
import org.apache.tapestry5.*;
import org.apache.tapestry5.hibernate.HibernateConfigurer;
import org.apache.tapestry5.hibernate.HibernateConstants;
import org.apache.tapestry5.hibernate.HibernateSymbols;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Local;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.ApplicationDefaults;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.apache.tapestry5.services.*;
import org.slf4j.Logger;

import com.allogy.qrauth.common.Version;

/**
 * This module is automatically included as part of the Tapestry IoC Registry, it's a good place to
 * configure and extend Tapestry, or to place your own service definitions.
 */
public
class AppModule
{
    public static
	void bind(ServiceBinder binder)
    {
		binder.bind(AuthSession     .class, AuthSessionImpl     .class);
        binder.bind(DatabaseMigrator.class, DatabaseMigratorImpl.class);
		binder.bind(DBTiming        .class, DBTimingImpl        .class);
		binder.bind(Journal         .class, JournalImpl         .class);
		binder.bind(Hashing         .class, HashingImpl         .class);
		binder.bind(Network         .class, NetworkImpl         .class);
		binder.bind(Nuts            .class, NutsImpl            .class);
		binder.bind(Policy          .class, PolicyImpl          .class);
    }

    public static void contributeFactoryDefaults(
            MappedConfiguration<String, Object> configuration)
    {
        // The application version is incorporated into URLs for most assets. Web
        // browsers will cache assets because of the far future expires header.
    	// If existing assets change (or if the Tapestry version changes) you
    	// should also change this number, to force the browser to download new
    	// versions. This overrides Tapesty's default (a random hexadecimal
    	// number), but may be further overridden by DevelopmentModule or QaModule
    	// by adding the same key in the contributeApplicationDefaults method.
        configuration.override(SymbolConstants.APPLICATION_VERSION, Version.FULL);

		if (System.getenv("HJ_CONFIG_FILE")!=null)
		{
			if (Version.IS_SNAPSHOT)
			{
				configuration.add(SymbolConstants.CONTEXT_PATH + "2", "/qrauth");
			}
			else
			{
				configuration.add(SymbolConstants.CONTEXT_PATH + "2", "/qrauth/v" + Version.MAJOR);
			}
		}
	}

	/**
	 * When in production mode, all pages & actions should be presumed as "secure" (using HTTPS).
	 * Because this is a security-sensitive project, if left unset, Tapestry would render all links
	 * to pages not specifically marked with the 'secure' annotation as http (insecure); commonly
	 * known as a mixed security model (e.g. for blogs and whatnot).
	 *
	 * @url http://tapestry.apache.org/https.html
	 * @param configuration
	 */
	public static
	void contributeMetaDataLocator(MappedConfiguration<String,String> configuration)
	{
		configuration.add(MetaDataConstants.SECURE_PAGE, "true");
	}

    public static void contributeApplicationDefaults(
            MappedConfiguration<String, Object> configuration)
    {
        // Contributions to ApplicationDefaults will override any contributions to
        // FactoryDefaults (with the same key). Here we're restricting the supported
        // locales to just "en" (English). As you add localised message catalogs and other assets,
        // you can extend this list of locales (it's a comma separated series of locale names;
        // the first locale name is the default when there's no reasonable match).
        configuration.add(SymbolConstants.SUPPORTED_LOCALES, "en");

		//only secure, disable tapestry's to-and-fro roaming (see below).
		configuration.add(SymbolConstants.SECURE_ENABLED, "false");

		//For an instant, a dark overlay and a busy-spinner is shown until the page becomes idle (by default).
		configuration.add(SymbolConstants.ENABLE_PAGELOADING_MASK, "false");

		configuration.add(HibernateSymbols.EARLY_START_UP, "true");
		configuration.add(HibernateSymbols.DEFAULT_CONFIGURATION, "false");

		configuration.add(SymbolConstants.HMAC_PASSPHRASE, Config.get().getTapestryHMACPassphrase());
    }

	public static
	void contributeHibernateSessionSource(OrderedConfiguration<HibernateConfigurer> configuration,
										  @Symbol(SymbolConstants.PRODUCTION_MODE)
										  final
										  boolean productionMode
	)
	{
		configuration.add("qrauth", new DatabaseMigratorImpl.PropertiesHandoff(productionMode));
	}

	public static
	void contributeHttpServletRequestHandler(OrderedConfiguration<HttpServletRequestFilter> configuration,
											Network network)
	{
		configuration.add("low-level-ban", new LowLevelBanFilter(network), "first");
	}

	public static
	void contributeMasterDispatcher(OrderedConfiguration<Dispatcher> configuration,
									Network network, AuthSession authSession)
	{
		configuration.add("high-level-ban", new HighLevelBanFilter(network), "before:RootPath");
		configuration.add("auth-session", authSession, "before:PageRender");
	}

	/**
	 * Use annotation or method naming convention: <code>contributeApplicationDefaults</code>
	 */
	@Contribute(SymbolProvider.class)
	@ApplicationDefaults
	public static void setupEnvironment(MappedConfiguration<String, Object> configuration)
	{
		configuration.add(SymbolConstants.JAVASCRIPT_INFRASTRUCTURE_PROVIDER, "jquery");
//		configuration.add(SymbolConstants.BOOTSTRAP_ROOT, "context:mybootstrap");
		configuration.add(SymbolConstants.MINIFICATION_ENABLED, true);
	}

	/*
	// This will override the bundled bootstrap version and will compile it at runtime
	@Contribute(JavaScriptStack.class)
	@Core
	public static void overrideBootstrapCSS(OrderedConfiguration<StackExtension> configuration)
	{
		configuration.override("bootstrap.css",
				new StackExtension(StackExtensionType.STYLESHEET, "context:mybootstrap/css/bootstrap.css"), "before:tapestry.css");
	}
	*/

	/**
	 * Adds ComponentEventResultProcessors
	 *
	 * @param configuration the configuration where new ComponentEventResultProcessors are registered by the type they are processing
	 * @param response the response that the event result processor handles
	 * @url http://wiki.apache.org/tapestry/Tapestry5HowToCreateAComponentEventResultProcessor
	 */
	public static
	void contributeComponentEventResultProcessor(MappedConfiguration<Class<?>, ComponentEventResultProcessor<?>> configuration, Response response)
	{
		configuration.add(OutputStreamResponse.class, new OutputStreamResponseResultProcessor(response));
	}

    /**
     * This is a service definition, the service will be named "TimingFilter". The interface,
     * RequestFilter, is used within the RequestHandler service pipeline, which is built from the
     * RequestHandler service configuration. Tapestry IoC is responsible for passing in an
     * appropriate Logger instance. Requests for static resources are handled at a higher level, so
     * this filter will only be invoked for Tapestry related requests.
     * <p/>
     * <p/>
     * Service builder methods are useful when the implementation is inline as an inner class
     * (as here) or require some other kind of special initialization. In most cases,
     * use the static bind() method instead.
     * <p/>
     * <p/>
     * If this method was named "build", then the service id would be taken from the
     * service interface and would be "RequestFilter".  Since Tapestry already defines
     * a service named "RequestFilter" we use an explicit service id that we can reference
     * inside the contribution method.
     */
    public RequestFilter buildTimingFilter(final Logger log)
    {
        return new RequestFilter()
        {
            public boolean service(Request request, Response response, RequestHandler handler)
                    throws IOException
            {
                long startTime = System.currentTimeMillis();

                try
                {
                    // The responsibility of a filter is to invoke the corresponding method
                    // in the handler. When you chain multiple filters together, each filter
                    // received a handler that is a bridge to the next filter.

                    return handler.service(request, response);
                } finally
                {
                    long elapsed = System.currentTimeMillis() - startTime;

                    log.info(String.format("Request time: %d ms", elapsed));
                }
            }
        };
    }

    /**
     * This is a contribution to the RequestHandler service configuration. This is how we extend
     * Tapestry using the timing filter. A common use for this kind of filter is transaction
     * management or security. The @Local annotation selects the desired service by type, but only
     * from the same module.  Without @Local, there would be an error due to the other service(s)
     * that implement RequestFilter (defined in other modules).
     */
    public void contributeRequestHandler(OrderedConfiguration<RequestFilter> configuration,
                                         @Local
                                         RequestFilter filter)
    {
        // Each contribution to an ordered configuration has a name, When necessary, you may
        // set constraints to precisely control the invocation order of the contributed filter
        // within the pipeline.

        configuration.add("Timing", filter);
    }
}
