package com.allogy.qrauth.server.helpers;

import com.allogy.qrauth.server.entities.Nut;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.services.Response;

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
		return "application/sqrl";
	}

	@Override
	public
	InputStream getStream() throws IOException
	{
		final
		StringBuilder sb=new StringBuilder();

		put("tif", Integer.toHexString(tif));

		for (Map.Entry<String, String> me : entrySet())
		{
			sb.append(me.getKey());
			sb.append('=');
			sb.append(me.getValue());
			sb.append("\r\n");
		}

		return new ByteArrayInputStream(sb.toString().getBytes());
	}

	@Override
	public
	void prepareResponse(Response response)
	{
		//no-op...
	}

	private
	int tif;

	public
	SqrlResponse tifCurrentIDMatch()
	{
		tif|=0x001;
		return this;
	}

	public
	SqrlResponse tifPreviousIDMatch()
	{
		tif|=0x002;
		return this;
	}

	public
	SqrlResponse tifIPsMatched()
	{
		tif|=0x004;
		return this;
	}

	public
	SqrlResponse tifSQRLDisabled()
	{
		tif|=0x008;
		return this;
	}

	public
	SqrlResponse tifCurrentlyUnused()
	{
		tif|=0x010;
		return this;
	}

	public
	SqrlResponse tifTransientFailure()
	{
		tif|=0x020;
		return this;
	}

	public
	SqrlResponse tifCommandFailed()
	{
		tif|=0x040;
		return this;
	}

	public
	SqrlResponse tifClientFailure()
	{
		tif|=0x080;
		return tifCommandFailed();
	}

	public
	SqrlResponse tifInvalidNut()
	{
		tif|=0x100;
		return this;
	}

	public
	SqrlResponse tifFunctionNotSupported()
	{
		tif|=0x200;
		return this;
	}

}
