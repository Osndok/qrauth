package com.allogy.qrauth.server.pages.api.sqrl;

import com.allogy.qrauth.server.crypto.Ed25519;
import com.allogy.qrauth.server.entities.Nut;
import com.allogy.qrauth.server.entities.TenantIP;
import com.allogy.qrauth.server.helpers.Bytes;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.SqrlHelper;
import com.allogy.qrauth.server.helpers.SqrlResponse;
import com.allogy.qrauth.server.pages.api.AbstractAPICall;
import com.allogy.qrauth.server.services.Nuts;
import com.allogy.qrauth.server.services.impl.Config;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.ActivationRequestParameter;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.util.TextStreamResponse;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
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

	private static final String CLIENT_PARAMETER_IDK = "idk";

	private static final String FRIENDLY_NAME    = Config.get().getSqrlServerFriendlyName();

	//For debugging, it is easier to be able to repeat old nuts. Has no effect in production.
	private static final boolean DEBUG_CONSUME_NUTS = false;

	@ActivationRequestParameter(PARAMETER_NUT)
	private
	String nutStringValue;

	/*
	Removed, because GRC's reference implementation does not include these in the original url.

	@ActivationRequestParameter("ver")
	private
	String supportedSqrlVersions;

	@ActivationRequestParameter("sfn")
	private
	String serversFriendlyName;
	*/

	private
	boolean useOldAndroidLogic;

	private
	SqrlResponse response;

	public
	DoSqrl with(Nut nut)
	{
		this.nut=nut;
		nutStringValue = nut.stringValue;
		cachedUrl = null;
		//supportedSqrlVersions = SUPPORTED_SQRL_VERSIONS;
		//serversFriendlyName = limit64TenantNameOrNull(nut.tenant);
		return this;
	}

	public
	DoSqrl with(String nut)
	{
		nutStringValue = nut;
		cachedUrl = null;
		//supportedSqrlVersions = SUPPORTED_SQRL_VERSIONS;
		return this;
	}

	@Inject
	private
	Logger log;

	@Inject
	private
	Session session;

	@Inject
	private
	RequestGlobals requestGlobals;

	private
	Nut nut;

	/**
	 * These are the POST parameters, are a bit 'lower-level' than the crypto/signing, and are initially distrusted.
	 * The first 'query' request usually includes: client, ids, server
	 * And subsequent requests usually include:
	 */
	private
	Map<String, String> parameters;

	private
	Map<String, String> clientParameters;

	Object onActivate() throws IOException
	{
		log.debug("onActivate()");

		//final
		//Map<String, String> parameters;
		{
			final
			String contentType = request.getHeader("Content-Type");

			if (contentType == null)
			{
				log.warn("client is missing 'Content-Type' header, so post variables were not parsed");
				parameters = manuallyParsePostBody();
			}
			else
			if (contentType.startsWith("application/x-www-form-urlencoded"))
			{
				parameters = new HashMap<String, String>();

				for (String key : request.getParameterNames())
				{
					parameters.put(key, request.getParameter(key));
				}
			}
			else
			{
				log.warn("incorrect content-type might prevent parameter parsing: '{}'", contentType);
				parameters = manuallyParsePostBody();
			}
		}

		if (log.isDebugEnabled())
		{

			for (String key : request.getHeaderNames())
			{
				String value = request.getHeader(key);

				log.debug("header: {} -> {}", key, value);
			}

			for (Map.Entry<String, String> me : parameters.entrySet())
			{
				log.debug("parameter: {} -> {}", me.getKey(), me.getValue());
			}
		}

		final
		String preNutRotationUrl = getUrl();

		final
		boolean incomingNutWasValid = consumedAndRotatedPerfectlyGoodNut();

		final
		String newQueryPath=pageRenderLinkSource.createPageRenderLink(DoSqrl.class).toRedirectURI();

		response = new SqrlResponse(SUPPORTED_SQRL_VERSIONS, nut, newQueryPath, FRIENDLY_NAME);

		/*
		-------------------------- MARK: we can now generate a 'usable' sqrl response ------------------------
		 */

		if (!agreeOnServerUrl(preNutRotationUrl))
		{
			return response.tifClientFailure();
		}

		if (useOldAndroidLogic)
		{
			response.compat_useFormEncoding=true;
			//response.put("nut", originalNut.stringValue);
			//response.setTif(0x21);
			//response.remove("qry");
			response.put("server", "required-for-happy-face");
		}

		if (!incomingNutWasValid)
		{
			log.debug("returning invalid-nut response");

			return response
					   .tifClientFailure()
					   .tifTransientFailure()
					   .tifInvalidNut();
		}

		if (ipMismatchIsCauseForAbort())
		{
			return response
				.tifTransientFailure()
				;
		}

		//The first order of business is to verify the 'client' block, but the key is wrapped up in the
		//client block itself. So... we must parse the client block before we can verify it.

		final
		byte[] clientBytes;

		final
		byte[] clientBytes2;
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
			String client = parameters.get(PARAMETER_CLIENT);
			{
				if (client == null)
				{
					log.debug("no client parameter");
					return response.tifClientFailure();
				}
				else
				{
					clientBytes= SqrlHelper.decode(client);
					clientBytes2=client.getBytes("UTF-8");
				}
			}
		}

		log.debug("client parameter is {} bytes", clientBytes.length);

		final
		Map<String,String> untrustedClientParameters=decodeParameterMap(clientBytes);

		final
		String idk;

		final
		byte[] idkPublicKey;
		{
			idk = untrustedClientParameters.get(CLIENT_PARAMETER_IDK);

			if (idk==null || idk.isEmpty())
			{
				log.debug("missing idk client parameter");
				return response.tifClientFailure();
			}
			else
			{
				idkPublicKey = SqrlHelper.decode(idk);
			}
		}

		//32
		log.trace("idk is a {} byte key", idkPublicKey.length);

		final
		byte[] clientIdkSignature;
		{
			/**
			 * From the SQRL specification:
			 * ----------------------------
			 * ids = IDentity Signature
			 * This is the signature used to authenticate the contents of the query block sent to the web server.
			 * The SQRL client synthesizes the site-specific private key, uses that to sign the concatenated
			 * values of the previously mentioned client and server parameters, sends the resulting signature to
			 * the web server as the value of this ids parameter. The web server verifies the signature using the
			 * accompanying idk, which must also match the value stored in the user's SQRL account association.
			 */
			final
			String identitySignature = parameters.get(PARAMETER_IDS);
			{
				if (identitySignature == null)
				{
					log.debug("no IDS parameter");
					return response.tifClientFailure();
				}
				else
				{
					clientIdkSignature= SqrlHelper.decode(identitySignature);
				}
			}
		}

		//64
		log.trace("ids is a {} byte signature", clientIdkSignature.length);

		{
			final
			byte[] serverBytes = parameters.get(PARAMETER_SERVER).getBytes();

			if (signatureChecksOut(idkPublicKey, clientIdkSignature, Bytes.concat(clientBytes2, serverBytes)))
			{
				log.debug("AUTHENTIC request: {}", idk);
			}
			else
			{
				log.debug("signature is invalid");
				return response.tifClientFailure();
			}
		}

		/*
		--------------------- MARK: below this, we can authenticate idk[PublicKey] ----------------------
		 */

		clientParameters=untrustedClientParameters;

		/**
		 * From the SQRL specification:
		 * ----------------------------
		 * The value of the client's “server=” name=value pair, contains this data, exactly as received,
		 * base64url encoded if necessary.
		 */
		final
		String server = parameters.get(PARAMETER_SERVER);
		{
			if (server == null)
			{
				log.debug("no server parameter");
				return missingParameter(PARAMETER_SERVER);
			}
		}

		final
		String previousIdentitySignature = parameters.get("pids");

		final
		String unlockRequestSignature = parameters.get("urs");

		if (log.isDebugEnabled())
		{
			for (Map.Entry<String, String> me : clientParameters.entrySet())
			{
				String key = me.getKey();
				String value = me.getValue();

				log.debug("client: {} -> {}", key, value);
			}
		}

		if (true) return response;

		final
		String commandBlock=clientParameters.get("cmd");

		final
		String[] commands=commandBlock.split("~");

		for (String commandString : commands)
		{
			log.debug("command={}", commandString);

			final
			SqrlHelper.Command command;
			{
				try
				{
					command = SqrlHelper.Command.valueOf(commandString);
				}
				catch (IllegalArgumentException e)
				{
					log.info("function not supported: {}", commandString);
					response.tifFunctionNotSupported();
					continue;
				}
			}

			try
			{
				switch(command)
				{
					case query:
						//no-op
						break;

					case enable:
					case disable:
						response.tifFunctionNotSupported();
						break;

					case ident:
					case login:
						/* TODO
						createAccountAndPublicKeyCredentials();
						letLooseTheWaitingRosebush();
						*/
						break;
				}
			}
			catch (Exception e)
			{
				log.debug("{} command failed", commandString, e);
				response.tifFunctionNotSupported();
			}
		}

		return response;

		/*
		final
		String clientVersion=clientMap.get("ver");

		final
		String identityKey=clientMap.get("idk");

		final
		String previousIdentityKey=clientMap.get("pidk");

		final
		String serverUnlockKey=clientMap.get("suk");

		final
		String vuk=clientMap.get("vuk");
		* /

		log.info("unimplemented function: {}", command);
		return response
			.tifCommandFailed()
			.tifFunctionNotSupported();
		*/
	}

	private
	byte[] concat_sha512(byte[] alpha, byte[] beta)
	{
		final
		MessageDigest sha256;
		{
			try
			{
				sha256 = MessageDigest.getInstance("SHA-512");
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new AssertionError(e);
			}
		}

		sha256.reset();
		sha256.update(alpha);
		sha256.update(beta);
		return sha256.digest();
	}

	private
	boolean signatureChecksOut(byte[] publicKey, byte[] signatureValue, byte[] utf8message)
	{
		if (log.isTraceEnabled())
		{
			log.trace(" public key: {}", Bytes.toHex(publicKey));
			log.trace("  signature: {}", Bytes.toHex(signatureValue));
			log.trace("utf8message: {}", Bytes.toHex(utf8message));
		}

		try
		{
			return Ed25519.checkvalid(signatureValue, utf8message, publicKey);
		}
		catch (Exception e)
		{
			log.error("unable to verify signature", e);
			return false;
		}
	}

	private
	boolean ipMismatchIsCauseForAbort()
	{
		if (Death.hathVisited(tenantIP))
		{
			log.debug("bad/banned ip address");
			//???: should we even spend the time to allocate a new nut?
			return false;
		}

		if (network.ipMatch(tenantIP, originalNut.tenantIP))
		{
			log.debug("nut/ip match");
			response.tifIPsMatched();
		}
		else
		{
			//TODO: under what circumstances should we abort the request for ip mismatch?
			log.info("nut/ip mismatch: {} != {}", tenantIP, originalNut.tenantIP);
		}

		return false;
	}

	@Inject
	private
	Nuts nuts;

	private
	Nut originalNut;

	private
	TenantIP tenantIP;

	/**
	 * SE: 'originalNut' is set to the nut that started this request
	 * SE: 'nut' is set to the next nut
	 * SE: 'tenant' is set to the tenant from the original nut
	 * SE: 'tenantIP' is set to the address of the current request
	 * @return
	 */
	@CommitAfter
	private
	boolean consumedAndRotatedPerfectlyGoodNut()
	{
		final
		String incomingNutString = nutStringValue;

		if (incomingNutString == null)
		{
			log.debug("no incoming nut string");
			with(nuts.allocate(null, network.needIPForThisRequest(null)));
			return false;
		}

		originalNut = (Nut) session.createCriteria(Nut.class)
								 .add(Restrictions.eq("stringValue", incomingNutString))
								 .uniqueResult();

		if (originalNut == null)
		{
			log.debug("bad nut / not found");
			with(nuts.allocate(null, network.needIPForThisRequest(null)));
			return false;
		}

		tenantIP=network.needIPForThisRequest(originalNut.tenant);

		if (Death.hathVisited(originalNut))
		{
			log.debug("bad nut / consumed");
			with(nuts.allocate(null, tenantIP));
			return false;
		}

		if (productionMode || DEBUG_CONSUME_NUTS)
		{
			originalNut.deadline=new Date();
			session.save(originalNut);
		}

		with(nuts.allocate(null, tenantIP));
		return true;
	}

	//TODO: this might be too fragile, or harsh. Basically saying that the signed url must be *exactly* the same as we would generate it now (minus the protocol)... in a cluster, differing tapestry, java, or servlet versions might cause this check to fail when there is no security threat.
	private
	boolean agreeOnServerUrl(String serverUrlMine) throws UnknownHostException
	{
		final
		String serverParameter=parameters.get(PARAMETER_SERVER);

		if (serverParameter==null || serverParameter.isEmpty())
		{
			log.debug("server parameter is missing or empty");
			return false;
		}

		final
		String serverUrlClient = new String(SqrlHelper.decode(serverParameter));

		if (serverUrlClient.equals(serverUrlMine))
		{
			log.debug("agree on server url: {}", serverUrlClient);
			return true;
		}
		else if (serverUrlClient.equals(removeProtocol(serverUrlMine)))
		{
			log.warn("adjusting protocol to suite old android client (protocol missing from server url): {}",
						serverUrlClient);
			useOldAndroidLogic = true;
			return true;
		}
		else
		{
			log.info("disagree on server url...");
			log.debug("serverUrl-client={}", serverUrlClient);
			log.debug("serverUrl-mine={}", serverUrlMine);
			return false;
		}
	}

	private
	Map<String, String> manuallyParsePostBody()
	{
		final
		Map<String, String> retval = new HashMap<String, String>();

		try
		{
			final
			StringBuilder sb = new StringBuilder();

			final
			BufferedReader br = requestGlobals.getHTTPServletRequest().getReader();

			int c;

			try
			{
				do
				{
					String key = null;
					{
						while ((c = br.read()) > 0)
						{
							if (c == '=')
							{
								key = URLDecoder.decode(sb.toString(), "UTF-8");
								break;
							}
							else
							{
								sb.append((char) c);
							}
						}
					}

					if (key == null) return retval;
					sb.delete(0, sb.length());

					String value = null;
					{
						while ((c = br.read()) > 0 && c != '&')
						{
							sb.append((char) c);
						}

						value = URLDecoder.decode(sb.toString(), "UTF-8");
					}

					sb.delete(0, sb.length());
					retval.put(key, value);
				}
				while (c == '&');
			}
			finally
			{
				br.close();
			}
		}
		catch (Exception e)
		{
			log.error("unable to manually parse post body", e);
		}

		return retval;
	}

	private
	TextStreamResponse missingClientParameter()
	{
		return new TextStreamResponse("application/sqrl",
										 "ver=" + SUPPORTED_SQRL_VERSIONS + "\r\n" +
											 "tif=140\r\n" +
											 "sf=Hello\r\n" +
											 "nut=blather\r\n"
		);
	}

	private
	TextStreamResponse badNut()
	{
		return new TextStreamResponse("application/sqrl",
										 "ver=" + SUPPORTED_SQRL_VERSIONS + "\r\n" +
											 "tif=140\r\n"
		);
	}

	private
	TextStreamResponse functionNotSupported()
	{
		final
		String message =
			"ver=" + SUPPORTED_SQRL_VERSIONS + "\r\n" +
				"tif=240\r\n";

		return new TextStreamResponse("application/sqrl",
										 SqrlHelper.encode(message.getBytes())
		);
	}

	private
	String removeProtocol(String url)
	{
		final
		int firstColon = url.indexOf(':');

		return url.substring(firstColon + 3);
	}

	private
	Map<String, String> decodeParameterMap(byte[] bytes) throws IOException
	{
		final
		Map<String, String> retval = new HashMap<String, String>();

		final
		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));

		try
		{
			String line = br.readLine();

			while (line != null)
			{
				final
				int equalsSign = line.indexOf('=');

				if (equalsSign > 0)
				{
					final
					String key = line.substring(0, equalsSign);

					final
					String value = line.substring(equalsSign + 1);

					retval.put(key, value);
				}

				line = br.readLine();
			}
		}
		finally
		{
			br.close();
		}

		return retval;
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
