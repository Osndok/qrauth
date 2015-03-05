package com.allogy.qrauth.server.helpers;

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;

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
			boolean urlSafe = true;

			return new Base64(urlSafe);
		}
	};

}
