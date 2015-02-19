package com.allogy.qrauth.server.pages.api.sqrl;

import com.allogy.qrauth.server.entities.Nut;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.ActivationRequestParameter;
import org.apache.tapestry5.annotations.PageActivationContext;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * This is the recipient of the 'sqrl://' action.
 */
public
class DoSqrl
{
	@ActivationRequestParameter("nut")
	private
	String nutStringValue;

	public
	DoSqrl with(Nut nut)
	{
		nutStringValue=nut.stringValue;
		cachedUrl=null;
		return this;
	}

	public
	DoSqrl with(String nut)
	{
		nutStringValue=nut;
		cachedUrl=null;
		return this;
	}

	@Inject
	private
	Logger log;

	@Inject
	private
	Session session;

	private
	Nut nut;

	Object onActivate()
	{
		if (nutStringValue == null)
		{
			return new ErrorResponse(404, "missing nut string");
		}

		nut = (Nut) session.createCriteria(Nut.class)
			.add(Restrictions.eq("stringValue", nutStringValue))
			.uniqueResult()
			;

		if (nut==null)
		{
			return new ErrorResponse(404, "invalid nut string");
		}

		if (Death.hathVisited(nut))
		{
			return new ErrorResponse(403, Death.noteMightSay(nut, "nut has already been used, or is now expired"));
		}

		log.info("TRY: {}", nut);

		if (log.isDebugEnabled())
		{
			for (String name : request.getParameterNames())
			{
				String value = request.getParameter(name);

				log.debug("parameter: {} -> {}", name, value);
			}
		}

		final
		String client=request.getParameter("client");

		final
		String ids=request.getParameter("ids");

		final
		String server=request.getParameter("server");

		return new ErrorResponse(500, "trying... " + nut);
		//Remember to kill used nuts!
	}

	@Inject
	private
	Request request;

	@Inject
	@Symbol(SymbolConstants.PRODUCTION_MODE)
	private
	boolean productionMode;

	@Inject
	private
	PageRenderLinkSource pageRenderLinkSource;

	private static final
	String SQRL_BASE=System.getenv("SQRL_BASE");

	public
	String getUrl() throws UnknownHostException
	{
		if (cachedUrl == null)
		{
			if (SQRL_BASE==null)
			{
				final
				String url = pageRenderLinkSource.createPageRenderLink(DoSqrl.class).toAbsoluteURI(false);

				final
				int firstColon = url.indexOf(':');

				log.debug("firstColon={}, url={}", firstColon, url);

				if (productionMode)
				{
					//TODO: should we support non-secure "qrl://"? We would have to change the tapestry app config too... :-/
					cachedUrl = "sqrl" + url.substring(firstColon);
				}
				else
				{
					//Development machine generally does not have HTTPS/SSL/TLS, and has a special port (8216).
					final
					int secondColon = url.indexOf(':', firstColon + 1);

					final
					String domain = System.getenv("SQRL_HOST");

					cachedUrl = "qrl://" + (domain == null ? InetAddress.getLocalHost().getHostAddress() : domain) + url.substring(secondColon);

				}
			}
			else
			{
				cachedUrl=SQRL_BASE+pageRenderLinkSource.createPageRenderLink(DoSqrl.class).toRedirectURI();
			}
		}

		return cachedUrl;
	}

	private
	String cachedUrl;

	public
	String getDomain() throws URISyntaxException
	{
		return new URI(cachedUrl).getHost();
	}

}
