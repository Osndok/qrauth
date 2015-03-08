package com.allogy.qrauth.server.helpers;

import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by robert on 3/5/15.
 */
public
class SqrlHelper
{
	public static
	byte[] decode(String utf8)
	{
		return BASE64URL_CODEC.get().decode(utf8);
	}

	public static
	String encode(byte[] data)
	{
		return BASE64URL_CODEC.get().encodeAsString(data);
	}

	public static
	String encode(String data)
	{
		try
		{
			return encode(data.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static final
	ThreadLocal<Base64> BASE64URL_CODEC = new ThreadLocal<Base64>()
	{
		@Override
		protected
		Base64 initialValue()
		{
			final
			int lineLength=0;

			final
			byte[] lineSeperator=new byte[0];

			final
			boolean urlSafe = true;

			return new Base64(lineLength, lineSeperator, urlSafe);
		}
	};

	public static
	Map<String, String> getParameterMap(byte[] bytes, Map<String,String> retval) throws IOException
	{
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

	public
	enum Command
	{
		query,
		ident,
		enable,
		disable,

		/**
		 * "login" is a command that the old Android client submits.
		 */
		@Deprecated
		login
	}

	public
	enum Option
	{
		sqrlonly,
		hardlock
	}
}
