package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.services.Policy;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.cron.IntervalSchedule;
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor;
import org.apache.tapestry5.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by robert on 2/18/15.
 */
public
class PolicyImpl implements Policy, Runnable
{
	private static final Long   SUPREME_TENANT_ID       = Config.get().getSupremeTenantID();
	private static final long   UPDATE_PERIOD_MILLIS    = TimeUnit.MINUTES.toMillis(5);
	private static final Logger log                     = LoggerFactory.getLogger(Policy.class);
	private static final long   GLOBAL_LOGOUT_PERIOD    = TimeUnit.DAYS.toMillis(7);
	private static final long   DEVEL_LOGOUT_PERIOD     = TimeUnit.HOURS.toMillis(1);
	private static final long   SHORTEST_USABLE_SESSION = TimeUnit.MINUTES.toMillis(3);

	private
	JSONObject supremeTenantConfig = new JSONObject();

	@Inject
	@Symbol(SymbolConstants.PRODUCTION_MODE)
	private
	boolean productionMode;

	@PostInjection
	public
	void serviceStarts(PeriodicExecutor periodicExecutor)
	{
		if (SUPREME_TENANT_ID == null)
		{
			log.error("no supreme tenant id is set... using static default policies *only*");
		}
		else
		{
			periodicExecutor.addJob(new IntervalSchedule(UPDATE_PERIOD_MILLIS),
									   "policy-updater",
									   this);
		}
	}

	@Override
	public
	boolean allowsAnonymousCreationOfNewTenants()
	{
		return bool("allowNewTenants", true);
	}

	@Override
	public
	long getGlobalLogoutPeriod()
	{
		if (productionMode)
		{
			return GLOBAL_LOGOUT_PERIOD;
		}
		else
		{
			return DEVEL_LOGOUT_PERIOD;
		}
	}

	@Override
	public
	long getShortestUsableSessionLength()
	{
		return SHORTEST_USABLE_SESSION;
	}

	private
	boolean bool(String key, boolean _default)
	{
		final
		Object o = supremeTenantConfig.opt(key);

		if (o instanceof Boolean)
		{
			return ((Boolean) o).booleanValue();
		}
		else
		{
			return _default;
		}
	}

	@Override
	public
	void run()
	{
		//TODO: fetch and decode the supreme tenant's config field
		log.warn("unimplemented");
	}
}
