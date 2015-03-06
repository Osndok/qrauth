package com.allogy.qrauth.server.helpers;

import com.allogy.qrauth.server.entities.Nut;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.services.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by robert on 3/4/15.
 */
public
class SqrlResponse extends HashMap<String,String> implements StreamResponse
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

				for (Map.Entry<String, String> me : entrySet())
				{
					if (sb.length()!=0)
					{
						sb.append("\r\n");
					}

					sb.append(me.getKey());
					sb.append('=');
					sb.append(me.getValue());
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
}
