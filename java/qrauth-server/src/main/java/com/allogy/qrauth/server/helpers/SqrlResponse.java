package com.allogy.qrauth.server.helpers;

import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.entities.Nut;
import com.allogy.qrauth.server.services.impl.Config;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.services.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by robert on 3/4/15.
 */
public
class SqrlResponse extends TreeMap<String,String> implements StreamResponse
{

	/**
	 * With a few exceptions, accepts the minimum/required set of response variables.
	 *
	 * @param versions
	 * @param nut
	 * @param queryPath
	 * @param friendlyName
	 */
	public
	SqrlResponse(String versions, Nut nut, String queryPath, String friendlyName)
	{
		put("ver", versions);
		put("nut", nut.stringValue);
		put("qry", queryPath);
		put("sfn", friendlyName);
	}

	private
	SqrlResponse()
	{
	}

	@Override
	public
	String getContentType()
	{
		if (compat_useFormEncoding)
		{
			//return "application/x-www-form-urlencoded";
			return "text/plain";
		}
		else
		{
			return "application/sqrl";
			//return "text/html; charset=UTF-8";
		}

		//return "text/plain; charset=utf-8";
		//return "text/plain";
		//return "text/html; charset=ISO-8859-4";
	}

	private byte[] finalizedData;

	private static final Logger log = LoggerFactory.getLogger(SqrlResponse.class);

	private
	byte[] getFinalizedData()
	{
		if (finalizedData == null)
		{
			final
			StringBuilder sb = new StringBuilder();

			if (compat_useFormEncoding)
			{
				put("tif", Integer.toString(tif));

				/*
				This encoding is intended to match the utility found here:
				https://github.com/vRallev/SQRL-Protocol/blob/master/sqrl-protocol/src/main/java/net/vrallev/java/sqrl/body/SqrlRequestUtil.java
				*/
				for (Map.Entry<String, String> me : entrySet())
				{
					if (sb.length()!=0)
					{
						sb.append('&');
					}

					sb.append(me.getKey());
					sb.append('=');
					sb.append(me.getValue());
				}

				final
				String responseString=sb.toString();

				log.debug("compatibility encoding:\n{}", responseString);
				finalizedData=responseString.getBytes();
			}
			else
			{
				put("tif", Integer.toHexString(tif));
				put("mac", calculateMessageAuthenticationCode());

				for (Map.Entry<String, String> me : entrySet())
				{
					/*
					Quote the spec:
					-----------------------
					When returned by a web server in response to a client's query, the name=value pairs occupy the
					body of the reply, appearing one per line with each pair terminated by a CRLF character pair.
					 */
					sb.append(me.getKey());
					sb.append('=');
					sb.append(me.getValue());
					sb.append("\r\n");
				}

				final
				String responseString = sb.toString();

				final
				String encoded = SqrlHelper.encode(responseString);

				log.debug("string:\n{}\n------------\nEncoded:\n-------------\n{}", responseString, encoded);

				finalizedData = encoded.getBytes();
			}
		}

		return finalizedData;
	}

	private
	String calculateMessageAuthenticationCode()
	{
		final
		Mac mac;
		{
			try
			{
				mac = Mac.getInstance("HmacSHA1");
				mac.init(Config.get().getHmacSha1SigningKey());
			}
			catch (RuntimeException e)
			{
				throw e;
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		for (Map.Entry<String, String> me : entrySet())
		{
			final
			String key=me.getKey();

			if (!key.equals("mac"))
			{
				mac.update(key.getBytes());
				mac.update(me.getValue().getBytes());
			}
		}

		return Bytes.toHex(mac.doFinal());
	}

	public static
	SqrlResponse fromMacSignedServerResponse(String base64Blob) throws IOException
	{
		final
		SqrlResponse sr=new SqrlResponse();

		SqrlHelper.getParameterMap(base64Blob.getBytes(), sr);

		final
		String providedMac=sr.get("mac");

		if (providedMac==null)
		{
			log.error("no 'mac' field in server blob");
			return null;
		}

		final
		String calculatedMac=sr.calculateMessageAuthenticationCode();

		//TODO: consider using the salt/peppered 'hashing' instead of standard mac???
		if (providedMac.equals(calculatedMac))
		{
			log.debug("mac code matches");
			return sr;
		}
		else
		{
			log.error("mac code mismatch: {} != {}", calculatedMac, providedMac);
			return null;
		}
	}

	public
	boolean compat_useFormEncoding;

	@Override
	public
	InputStream getStream() throws IOException
	{
		return new ByteArrayInputStream(getFinalizedData());
	}

	@Override
	public
	void prepareResponse(Response response)
	{
		//TODO: Shouldn't this be the only header one needed?
		response.setHeader("Content-Length", Integer.toString(getFinalizedData().length));
		response.setHeader("Connection", "close");
		/*
		These headers are copied from GRC's reference implementation...
		HTTP/1.1 200 OK
		Cache-Control: no-cache
		Pragma: no-cache
		Content-Length: 122
		Content-Type: text/html; charset=ISO-8859-4
		Expires: Mon, 01 Jan 1990 00:00:00 GMT
		Server: GRC/IIS Hybrid Application Webserver
		Date: Thu, 05 Mar 2015 21:37:09 GMT
		 * /
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma","no-cache");
		//response.setHeader("Content-Length: 122"); ... above...
		//"\t\tContent-Type: text/html; charset=ISO-8859-4" ... further above
		response.setDateHeader("Expires", 0);
		response.setHeader("Server", "GRC/IIS Hybrid Application Webserver");
		response.setDateHeader("Date", System.currentTimeMillis());

		/* And here are some headers from a different server that works with the GRC client...
		HTTP/1.1 200 OK
		Server: G-WAN
		Date: Fri, 06 Mar 2015 05:37:55 GMT
		Last-Modified: Fri, 06 Mar 2015 05:37:55 GMT
		ETag: "d9c63dd7-54f93d33-be"
		Content-Type: text/html; charset=UTF-8
		Content-Length: 190
		Connection: close
		 */
	}

	private
	int tif;

	public
	SqrlResponse tifCurrentIDMatch()
	{
		tif |= 0x001;
		return this;
	}

	public
	SqrlResponse tifPreviousIDMatch()
	{
		tif |= 0x002;
		return this;
	}

	public
	SqrlResponse tifIPsMatched()
	{
		tif |= 0x004;
		return this;
	}

	public
	SqrlResponse tifSQRLDisabled()
	{
		tif |= 0x008;
		return this;
	}

	public
	SqrlResponse tifFunctionNotSupported()
	{
		tif |= 0x010;
		return tifCommandFailed();
	}

	public
	SqrlResponse tifTransientFailure()
	{
		tif |= 0x020;
		return this;
	}

	public
	SqrlResponse tifCommandFailed()
	{
		tif |= 0x040;
		return this;
	}

	public
	SqrlResponse tifClientFailure()
	{
		tif |= 0x080;
		return tifCommandFailed();
	}

	@Deprecated
	public
	SqrlResponse tifInvalidNut()
	{
		tif |= 0x100;
		return this;
	}

	public
	void setTif(int tif)
	{
		this.tif = tif;
	}

	public
	void foundCurrentIdentity(DBUserAuth userAuth)
	{
		//TODO: the spec is a bit unclear... if this flag should only be set if vuk & suk are present (as "").
		tifCurrentIDMatch();

		final
		String sukAndVuk=userAuth.idRecoveryLock;

		if (sukAndVuk!=null)
		{
			final
			String[] bits=sukAndVuk.split(":");

			if (bits.length==2)
			{
				put("suk", bits[0]);
				put("vuk", bits[1]);
			}
		}
	}
}
