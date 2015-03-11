package com.allogy.qrauth.server.pages.api.sqrl;

import com.allogy.qrauth.server.crypto.Ed25519;
import com.allogy.qrauth.server.entities.*;
import com.allogy.qrauth.server.helpers.*;
import com.allogy.qrauth.server.pages.api.AbstractAPICall;
import com.allogy.qrauth.server.pages.internal.auth.DispatchAuth;
import com.allogy.qrauth.server.services.Journal;
import com.allogy.qrauth.server.services.Nuts;
import com.allogy.qrauth.server.services.Policy;
import com.allogy.qrauth.server.services.impl.Config;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.ActivationRequestParameter;
import org.apache.tapestry5.annotations.InjectPage;
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
import java.io.IOException;
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

	private static final String FRIENDLY_NAME = Config.get().getSqrlServerFriendlyName();

	//For debugging, it is easier to be able to repeat old nuts. Has no effect in production.
	private static final boolean DEBUG_CONSUME_NUTS = false;

	/**
	 * Originally, it was envisioned that each response would use a new nut. It is unclear if
	 * this would provide any additional security, and it makes the implementation more complex.
	 * Setting this to true without significant code changes would result in the SQRL subsystem
	 * being broken. It is here, at least, to preserve what was already done towards this end
	 * (in case it is found useful later) and to mark where in the code such logic is important.
	 */
	public static final boolean REQUIRES_NEW_NUTS_FOR_EACH_RESPONSE = false;

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
	boolean useOriginalAndroidLogic;

	private
	SqrlResponse response;

	public
	DoSqrl with(Nut nut)
	{
		this.nut = nut;
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

	@CommitAfter
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
			else if (contentType.startsWith("application/x-www-form-urlencoded"))
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

		if (REQUIRES_NEW_NUTS_FOR_EACH_RESPONSE)
		{
			with(nuts.allocate(null, tenantIP));
		}
		else
		{
			with(originalNut);
		}

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

		if (useOriginalAndroidLogic)
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
		Map<String,String> untrustedClientParameters=SqrlHelper.getParameterMap(clientBytes, new HashMap<String, String>());

		final
		String idkString;

		final
		byte[] idkPublicKey;
		{
			idkString = untrustedClientParameters.get(CLIENT_PARAMETER_IDK);

			if (idkString==null || idkString.isEmpty())
			{
				log.debug("missing idk client parameter");
				return response.tifClientFailure();
			}
			else
			{
				idkPublicKey = SqrlHelper.decode(idkString);
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

		final
		byte[] authMessageBytes;
		{
			final
			byte[] serverBytes = parameters.get(PARAMETER_SERVER).getBytes();

			authMessageBytes=Bytes.concat(clientBytes2, serverBytes);

			if (signatureChecksOut(idkPublicKey, clientIdkSignature, authMessageBytes))
			{
				log.debug("AUTHENTIC request: {}", idkString);
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

		DBUserAuth currentIdentity=byPublicKey(idkString);
		{
			if (currentIdentity == null)
			{
				log.debug("no current-id match");
			}
			else
			{
				log.debug("found match for current id (idk): {}", currentIdentity);
				response.foundCurrentIdentity(currentIdentity);
			}
		}

		final
		DBUserAuth previousIdentity=getOptionalPreviousIdentity(authMessageBytes);
		{
			if (previousIdentity != null)
			{
				log.debug("valid previous-id match");
				//response.foundPreviousIdentity(previousIdentity);
				response.tifPreviousIDMatch();
			}
		}

		//The first SQRL client to scan the code gets it.
		//If a second SQRL client scans the code (before the auth finishes), the nut is nullified.
		if (originalNut.mutex==null)
		{
			final
			DBUserAuth nutAuth = originalNut.userAuth;

			if (nutAuth!=null && currentIdentity!=null && currentIdentity.user!=null && !sameUser(nutAuth.user, currentIdentity))
			{
				//This nut was allocated for a special purpose, and someone else got to it...
				log.error("no mutex, and pre-set userAuths do not match: {} != {}", nutAuth.user, currentIdentity.user);
				terminateMultiClientNut(originalNut);
				return response.tifCommandFailed();
			}
			else
			{
				log.debug("no mutex, set to: {}", idkString);
				originalNut.mutex = idkString;
			}
		}
		else
		if (!originalNut.mutex.equals(idkString))
		{
			log.error("{} was scanned by one SQRL client, then another");

			if (!Death.hathVisited(nut))
			{
				terminateMultiClientNut(originalNut);
			}

			//TODO: we can instead send a new nut and set 'transient error'
			return response.tifCommandFailed();
		}

		/* *
		 * From the SQRL specification:
		 * ----------------------------
		 * The value of the client's “server=” name=value pair, contains this data, exactly as received,
		 * base64url encoded if necessary.
		 * /
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
		*/

		if (log.isDebugEnabled())
		{
			for (Map.Entry<String, String> me : clientParameters.entrySet())
			{
				String key = me.getKey();
				String value = me.getValue();

				log.debug("client: {} -> {}", key, value);
			}
		}

		final
		String commandBlock=clientParameters.get("cmd");

		final
		String[] commands=commandBlock.split("~");

		log.debug("{} commands in block", commands.length);

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

			//TODO: do something with previous-identity...

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
					{
						final
						String qrauthCommand=nut.command;

						if (qrauthCommand!=null)
						{
							if (qrauthCommand.equals("attach"))
							{
								//the user *and* the userAuth are already ready... we just need to claim it.
								if (currentIdentity==null)
								{
									currentIdentity = nut.userAuth;
									currentIdentity.pubKey = idkString;
									currentIdentity.comment = attachmentComment(nut.tenantIP, tenantIP);
									currentIdentity.deadline = null;

									log.info("{} is claiming {} for {}", tenantIP, currentIdentity,
												currentIdentity.user);
									journal.addedUserAuthCredential(currentIdentity);
								}
								else
								{
									//TODO: consider handling *informed* take-over/transfer... sorta like yubikey? except with an "are you sure?"
									if (sameUser(nut.userAuth.user, currentIdentity))
									{
										log.warn("trying to attach SQRL id already in use by another user");
										nut.userAuth.comment = "Tried to re-attach SQRL identity already in use by this account.";
										nut.userAuth.deadline = new Date();
										session.save(nut.userAuth);
									}
									else
									{
										log.warn("trying to attach SQRL id already in use by another user");
										nut.userAuth.comment = "Tried to attach SQRL identity already in use by " + currentIdentity.user;
										nut.userAuth.deadline = new Date();
										session.save(nut.userAuth);

										currentIdentity.comment += " " + nut.userAuth.user + " tried to transfer this SQRL identity to his/her account on " + DateHelper.iso8601();
									}
								}
							}
							else
							{
								log.error("unknown qrauth-command: {}", qrauthCommand);
							}

							maybeRememberSukAndVuk(currentIdentity);
						}
						else
						if (currentIdentity == null)
						{
							currentIdentity=maybeCreateAccountAndPublicKeyCredentials(idkString);
						}
						else
						if (!response.containsKey("suk"))
						{
							maybeRememberSukAndVuk(currentIdentity);
						}

						session.save(currentIdentity);

						originalNut.userAuth=currentIdentity;
						originalNut.deadline=new Date(System.currentTimeMillis()+policy.getMaximumSqrlHandoffPeriod());
						originalNut.deathMessage="SQRL hand-off timed out";
						session.save(originalNut);

						response.foundCurrentIdentity(currentIdentity);

						//TODO: if (nut.command!=null)...

						/*
						TODO: letLooseTheSessionRosebush();
						*/
						log.debug("SQRL ident completed successfully");

						break;
					}
				}
			}
			catch (Exception e)
			{
				log.debug("{} command failed", commandString, e);
				response.tifFunctionNotSupported();
			}
		}

		return response;
	}

	private
	String attachmentComment(TenantIP generator, TenantIP claimant)
	{
		if (network.ipMatch(generator, claimant))
		{
			return "New SQRL identity attached from same IP address";
		}
		else
		{
			return "New SQRL identity attached from a different IP address: "+claimant;
		}
	}

	@Inject
	private
	Journal journal;

	@Inject
	private
	Policy policy;

	private
	boolean sameUser(DBUser user, DBUserAuth userAuth)
	{
		if (userAuth == null)
		{
			log.debug("userAuth==null -> sameUser()=false");
			return false;
		}
		else
		{
			return user.id.equals(userAuth.user.id);
		}
	}

	@CommitAfter
	private
	void terminateMultiClientNut(Nut nut)
	{
		nut.deadline = new Date();
		nut.deathMessage = "The nut you are trying to use was already seen/scanned by another SQRL client.";
		session.save(nut);
	}

	private
	DBUserAuth maybeCreateAccountAndPublicKeyCredentials(String pubKey)
	{
		log.debug("creating new user/credential pair for SQRL login");

		final
		DBUserAuth userAuth = new DBUserAuth();

		userAuth.authMethod = AuthMethod.SQRL;
		userAuth.millisGranted = (int) AuthMethod.SQRL.getDefaultLoginLength();
		userAuth.pubKey = pubKey;

		maybeRememberSukAndVuk(userAuth);
		dispatchAuth.createUserWithNewStipulation(userAuth);

		return userAuth;
	}

	@InjectPage
	private
	DispatchAuth dispatchAuth;

	private
	void maybeRememberSukAndVuk(DBUserAuth userAuth)
	{
		final
		String suk = clientParameters.get("suk");

		final
		String vuk = clientParameters.get("vuk");

		if (empty(suk) || empty(vuk))
		{
			log.debug("missing suk or vuk");
		}
		else
		{
			userAuth.idRecoveryLock = suk + ":" + vuk;
		}
	}

	private
	DBUserAuth byPublicKey(String pubKey)
	{
		return (DBUserAuth)
				   session.createCriteria(DBUserAuth.class)
					   .add(Restrictions.eq("pubKey", pubKey))
					   .uniqueResult()
			;
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
			tenantIP = network.needIPForThisRequest(null);
			return false;
		}

		originalNut = (Nut) session.createCriteria(Nut.class)
								.add(Restrictions.eq("stringValue", incomingNutString))
								.uniqueResult();

		if (originalNut == null)
		{
			log.debug("bad nut / not found");
			tenantIP = network.needIPForThisRequest(null);
			return false;
		}

		tenantIP = network.needIPForSession(originalNut.tenantSession);

		if (Death.hathVisited(originalNut))
		{
			log.debug("bad nut / consumed");
			return false;
		}

		if (REQUIRES_NEW_NUTS_FOR_EACH_RESPONSE && (productionMode || DEBUG_CONSUME_NUTS))
		{
			originalNut.deadline = new Date();
			session.save(originalNut);
		}

		return true;
	}

	//TODO: this might be too fragile, or harsh. Basically saying that the signed url must be *exactly* the same as we would generate it now (minus the protocol)... in a cluster, differing tapestry, java, or servlet versions might cause this check to fail when there is no security threat.
	private
	boolean agreeOnServerUrl(String serverUrlMine) throws UnknownHostException
	{
		final
		String serverParameter = parameters.get(PARAMETER_SERVER);

		if (serverParameter == null || serverParameter.isEmpty())
		{
			log.debug("server parameter is missing or empty");
			return false;
		}

		final
		String serverUrlClient = new String(SqrlHelper.decode(serverParameter));

		if (looksLikeDataDumpAsOpposedToAURL(serverUrlClient))
		{
			return verifyEmbeddedMAC(serverUrlClient);
		}
		else if (serverUrlClient.equals(serverUrlMine))
		{
			log.debug("agree on server url: {}", serverUrlClient);
			return true;
		}
		else if (serverUrlClient.equals(removeProtocol(serverUrlMine)))
		{
			log.warn("adjusting protocol to suite old android client (protocol missing from server url): {}",
						serverUrlClient);
			useOriginalAndroidLogic = true;
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
	boolean looksLikeDataDumpAsOpposedToAURL(String keyValuesOrUrl)
	{
		return keyValuesOrUrl.indexOf('\n') >= 0;
	}

	private
	boolean verifyEmbeddedMAC(String serverDataDump)
	{
		try
		{
			SqrlResponse sr = SqrlResponse.fromMacSignedServerResponse(serverDataDump);
			//TODO: We will probably need to pull values from the old response here
			return sr != null;
		}
		catch (IOException e)
		{
			log.error("unable to verify MAC'd server blob", e);
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

					final
					String value;
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

	public
	DBUserAuth getOptionalPreviousIdentity(byte[] authMessageBytes)
	{
		final
		String pidkString = clientParameters.get("pidk");

		final
		String pidsSignature = parameters.get("pids");

		if (empty(pidkString) && empty(pidsSignature))
		{
			//Common case...
			log.debug("no previous identity info");
			return null;
		}
		else if (empty(pidkString))
		{
			log.debug("missing previous identity key");
			response.tifClientFailure();
			return null;
		}
		else if (empty(pidsSignature))
		{
			log.debug("missing previous identity signature");
			response.tifClientFailure();
			return null;
		}

		final
		byte[] pidkPublicKey = SqrlHelper.decode(pidkString);

		final
		byte[] clientPidkSignature = SqrlHelper.decode(pidsSignature);

		if (signatureChecksOut(pidkPublicKey, clientPidkSignature, authMessageBytes))
		{
			log.debug("AUTHENTIC previous-id: {}", pidkString);
			return byPublicKey(pidkString);
		}
		else
		{
			log.debug("previous-identity signature check failed");
			response.tifClientFailure();
			return null;
		}
	}

	private
	boolean empty(String s)
	{
		return (s == null || s.isEmpty());
	}
}
