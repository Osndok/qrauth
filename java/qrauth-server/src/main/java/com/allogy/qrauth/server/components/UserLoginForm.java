package com.allogy.qrauth.server.components;

import com.allogy.qrauth.server.entities.Nut;
import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.entities.TenantIP;
import com.allogy.qrauth.server.entities.TenantSession;
import com.allogy.qrauth.server.pages.api.sqrl.DoSqrl;
import com.allogy.qrauth.server.services.Network;
import com.allogy.qrauth.server.services.Nuts;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.BaseURLSource;
import org.apache.tapestry5.services.Request;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Dual-purpose:
 * (1) when delivering the login page logic to a tenant, and
 * (2) when a user is logging into this service directly,
 *
 * ...therefore, we must use absolute links for just about everything.
 */
public
class UserLoginForm
{
	@Parameter
	@Property
	private
	TenantSession tenantSession;

	//-------- end component parameters ---------

	@Inject
	private
	Nuts nuts;

	@Inject
	private
	Network network;

	@Inject
	@Symbol(SymbolConstants.PRODUCTION_MODE)
	private
	boolean productionMode;

	@Inject
	private
	BaseURLSource baseURLSource;

	@Inject
	private
	Request request;

	private
	TenantIP tenantIP;

	@Property
	private
	Nut nut;

	@Property
	private
	String base;

	@Inject
	private
	Logger log;

	void setupRender()
	{
		final
		Tenant tenant;
		{
			if (tenantSession==null)
			{
				tenant=null;
			}
			else
			{
				tenant=tenantSession.tenant;
			}
		}

		tenantIP = network.needIPForThisRequest(tenant);
		nut = nuts.allocate(tenant, tenantIP);

		doSqrl.with(nut);

		final
		String contextPath = request.getContextPath();

		base = baseURLSource.getBaseURL(productionMode) + contextPath;

		log.debug("setup: {} -> {} -> {} & {} -> {} -> {}", tenantIP, nut, nut.stringValue, contextPath, base);
	}

	@InjectPage
	@Property
	private
	DoSqrl doSqrl;

}
