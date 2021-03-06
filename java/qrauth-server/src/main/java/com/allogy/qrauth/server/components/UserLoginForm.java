package com.allogy.qrauth.server.components;

import com.allogy.qrauth.server.entities.*;
import com.allogy.qrauth.server.pages.api.nut.StateNut;
import com.allogy.qrauth.server.pages.api.sqrl.DoSqrl;
import com.allogy.qrauth.server.services.Network;
import com.allogy.qrauth.server.services.Nuts;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.apache.tapestry5.services.BaseURLSource;
import org.apache.tapestry5.services.PageRenderLinkSource;
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
	@Property
	private
	Request request;

	private
	TenantIP tenantIP;

	@Property
	private
	Nut nut;

	/**
	 * NB: UserLoginForm is included in html fragments sent via the tenant api, therefore we cannot use
	 * relative urls.
	 */
	@Property
	private
	String base;

	@Inject
	private
	Logger log;

	void setupRender()
	{
		tenantIP = network.needIPForSession(tenantSession);
		nut = nuts.allocate(tenantSession, tenantIP);

		doSqrl.with(nut);

		base = baseURLSource.getBaseURL(productionMode);

		log.debug("setup: {} -> {} -> {} => {}", tenantIP, nut, nut.stringValue, base);
	}

	@InjectPage
	@Property
	private
	DoSqrl doSqrl;

	private static String REGISTRATION_METHODS;

	public
	String getRegistrationMethods()
	{
		if (REGISTRATION_METHODS == null)
		{
			final
			StringBuilder sb = new StringBuilder();

			for (AuthMethod authMethod : AuthMethod.values())
			{
				if (authMethod.isRegistrationCapable())
				{
					if (sb.length() > 0)
					{
						sb.append(", ");
					}

					sb.append(InternalUtils.toUserPresentable(authMethod.toString()));
				}
			}

			REGISTRATION_METHODS = sb.toString();
		}

		return REGISTRATION_METHODS;
	}

	@InjectPage
	private
	StateNut stateNutPage;

	@Inject
	private
	PageRenderLinkSource pageRenderLinkSource;

	public
	String getPollStateUrl()
	{
		stateNutPage.with(nut);
		return pageRenderLinkSource.createPageRenderLink(StateNut.class).toRedirectURI();
	}

	public
	String getSmsPhoneNumber()
	{
		//NB: must be formatted thus, for the "tel://" links??
		return "1-555-123-4567";
	}

	public
	String getSmsSendCode()
	{
		//TODO: when ready, the sms code should have a moving epoch, to keep the id numbers *very* small.
		return Long.toHexString(nut.id).toUpperCase();
	}

	public
	String getSmsSendLink()
	{
		return "sms:"+getSmsPhoneNumber()+"?body="+getSmsSendCode();
	}
}
