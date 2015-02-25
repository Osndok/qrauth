package com.allogy.qrauth.server.pages.api.sqrl;

import com.allogy.qrauth.server.entities.Nut;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.pages.api.AbstractAPICall;
import org.apache.commons.codec.binary.Base64;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.ActivationRequestParameter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.util.TextStreamResponse;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the recipient of the 'sqrl://' action.
 */
public
class DoSqrl extends AbstractAPICall
{
	private static final
	String SUPPORTED_SQRL_VERSIONS = "1";

	private static final String PARAMETER_NUT    = "nut";
	private static final String PARAMETER_CLIENT = "client";
	private static final String PARAMETER_IDS    = "ids";
	private static final String PARAMETER_SERVER = "server";

	@ActivationRequestParameter(PARAMETER_NUT)
	private
	String nutStringValue;

	@ActivationRequestParameter("ver")
	private
	String supportedSqrlVersions;

	@ActivationRequestParameter("sfn")
	private
	String serversFriendlyName;

	public
	DoSqrl with(Nut nut)
	{
		nutStringValue = nut.stringValue;
		cachedUrl = null;
		supportedSqrlVersions = SUPPORTED_SQRL_VERSIONS;
		//serversFriendlyName = limit64TenantNameOrNull(nut.tenant);
		return this;
	}

	public
	DoSqrl with(String nut)
	{
		nutStringValue = nut;
		cachedUrl = null;
		supportedSqrlVersions = SUPPORTED_SQRL_VERSIONS;
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

	Object onActivate() throws IOException
	{
		/**
		 * From the SQRL specification:
		 * ----------------------------
		 * The query's “client=” parameter contains a base64url encoded list of name=value pairs, one
		 * per line, with each line terminated by a CRLF character pair. The client assembles a list
		 * of the following name=value pairs to return data to the web server, to specify one or more
		 * command actions it is requesting from the server, and to provide any cryptographic keying
		 * material required to authorize the requested actions and/or authenticate its user's identity.
		 */
		final
		String client = request.getParameter(PARAMETER_CLIENT);
		{
			if (client == null)
			{
				return missingParameter(PARAMETER_CLIENT);
			}
		}

		/**
		 * From the SQRL specification:
		 * ----------------------------
		 * The value of the client's “server=” name=value pair, contains this data, exactly as received,
		 * base64url encoded if necessary.
		 */
		final
		String server = request.getParameter(PARAMETER_SERVER);
		{
			if (server == null)
			{
				return missingParameter(PARAMETER_SERVER);
			}
		}

		/**
		 * From the SQRL specification:
		 * ----------------------------
		 * ids = IDentity Signature
		 * This is the signature used to authenticate the contents of the query block sent to the web server.
		 * The SQRL client synthesizes the site-specific private key, uses that to sign the concatenated
		 * values of the previously mentioned client and server parameters, sends the resulting signature to
		 * the web server as the value of this ids parameter. The web server verifies the signature using the
		 * accompanying idk, which must also digestMatch the value stored in the user's SQRL account association.
		 */
		final
		String identitySignature = request.getParameter(PARAMETER_IDS);
		{
			if (identitySignature == null)
			{
				return missingParameter(PARAMETER_IDS);
			}
		}

		if (nutStringValue == null)
		{
			return missingParameter(PARAMETER_NUT);
		}

		nut = (Nut) session.createCriteria(Nut.class)
						.add(Restrictions.eq("stringValue", nutStringValue))
						.uniqueResult()
		;

		if (nut == null)
		{
			//return invalidParameter(PARAMETER_NUT);
			return badNut();
		}

		if (Death.hathVisited(nut))
		{
			//return new ErrorResponse(403, Death.noteMightSay(nut, "nut has already been consumed, or is now expired"));
			return badNut();
		}

		final
		String serverUrlClient=new String(BASE64URL_CODEC.get().decode(server));

		log.debug("serverUrl-client={}", serverUrlClient);

		final
		String serverUrlMine=removeProtocol(getUrl());

		log.debug("serverUrl-mine={}", serverUrlMine);

		if (!serverUrlClient.equals(serverUrlMine))
		{
			//TODO: this might be too fragile, or harsh. Basically saying that the signed url must be *exactly* the same as we would generate it now (minus the protocol)... in a cluster, differing tapestry, java, or servlet versions might cause this check to fail when there is no security threat.
			return invalidParameter("server");
		}

		final
		String previousIdentitySignature = request.getParameter("pids");

		final
		String unlockRequestSignature = request.getParameter("urs");

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
		Map<String,String> clientMap=decodeBase64Parameters(client);

		if (log.isDebugEnabled())
		{
			for (Map.Entry<String, String> me : clientMap.entrySet())
			{
				String key = me.getKey();
				String value = me.getValue();

				log.debug("client: {} -> {}", key, value);
			}
		}

		final
		String clientVersion=clientMap.get("ver");

		final
		String command=clientMap.get("cmd");

		final
		String identityKey=clientMap.get("idk");

		final
		String previousIdentityKey=clientMap.get("pidk");

		final
		String serverUnlockKey=clientMap.get("suk");

		final
		String vuk=clientMap.get("vuk");


		return functionNotSupported();
		//Remember to kill used nuts!
	}

	private
	TextStreamResponse badNut()
	{
		return new TextStreamResponse("application/sqrl",
										"ver="+SUPPORTED_SQRL_VERSIONS+"\r\n"+
										"tif=140\r\n"
										);
	}

	private
	TextStreamResponse functionNotSupported()
	{
		final
		String message=
			"ver="+SUPPORTED_SQRL_VERSIONS+"\r\n"+
			"tif=240\r\n"
			;

		return new TextStreamResponse("application/sqrl",
										 BASE64URL_CODEC.get().encodeAsString(message.getBytes())
		);
	}

	private
	String removeProtocol(String url)
	{
		final
		int firstColon=url.indexOf(':');

		return url.substring(firstColon+3);
	}

	private
	Map<String, String> decodeBase64Parameters(String base64UrlEncoded) throws IOException
	{
		final
		byte[] bytes = BASE64URL_CODEC.get().decode(base64UrlEncoded);

		final
		Map<String,String> retval=new HashMap<String, String>();

		final
		BufferedReader br=new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));

		try
		{
			String line=br.readLine();

			while (line!=null)
			{
				final
				int equalsSign=line.indexOf('=');

				if (equalsSign>0)
				{
					final
					String key=line.substring(0, equalsSign);

					final
					String value=line.substring(equalsSign+1);

					retval.put(key, value);
				}

				line=br.readLine();
			}
		}
		finally
		{
			br.close();
		}

		return retval;
	}

	private static final
	ThreadLocal<Base64> BASE64URL_CODEC = new ThreadLocal<Base64>()
	{
		@Override
		protected
		Base64 initialValue()
		{
			final
			boolean urlSafe=true;

			return new Base64(urlSafe);
		}
	};

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
	String SQRL_BASE = System.getenv("SQRL_BASE");

	public
	String getUrl() throws UnknownHostException
	{
		if (cachedUrl == null)
		{
			if (SQRL_BASE == null)
			{
				final
				String url = pageRenderLinkSource.createPageRenderLink(DoSqrl.class).toAbsoluteURI(false);

				final
				int firstColon = url.indexOf(':');

				log.trace("firstColon={}, url={}", firstColon, url);

				if (productionMode)
				{
					//TODO: should we support non-secure "qrl://"? We would have to change the tapestry app config too... :-/
					cachedUrl = "sqrl" + url.substring(firstColon);
				}
				else
				{
					//Development machine generally does not have HTTPS/SSL/TLS, and has a special port (8216).
					final
					int slash = url.indexOf('/', firstColon + 3);

					log.trace("slash={} where url={}", slash, url);

					final
					String domain = System.getenv("SQRL_HOST");

					cachedUrl = "qrl://" + (domain == null ? InetAddress.getLocalHost().getHostAddress() : domain) + url.substring(slash);

				}
			}
			else
			{
				cachedUrl = SQRL_BASE + pageRenderLinkSource.createPageRenderLink(DoSqrl.class).toRedirectURI();
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
