package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.services.Nuts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by robert on 2/16/15.
 */
public
class NutsImpl implements Nuts
{
	private static final Logger log = LoggerFactory.getLogger(Nuts.class);

	public static final int BITS=160;
	public static final int BYTES=20;
	public static final int CHARS=27;

	/**
	 * @TODO: look into methods to easily get high-quality random numbers (rather than simply psuedo-random) for nut generation.
	 * @return
	 */
	@Override
	public
	byte[] generateBytes()
	{
		final
		byte[] bytes;
		{
			bytes = new byte[20];
			ThreadLocalRandom.current().nextBytes(bytes);
		}

		return bytes;
	}

	/**
	 * To support java-6, we use the DatatypeConverter class.
	 * To ensure that the last character is valid, start with the correct number of bytes.
	 * @return the string value of a base64url decodable nut
	 */
	public
	String toStringValue(byte[] bytes)
	{
		if (log.isDebugEnabled())
		{
			log.debug("origin: {}", Arrays.toString(bytes));
		}

		final
		String base64 = javax.xml.bind.DatatypeConverter.printBase64Binary(bytes);

		final
		String nutty = toUrlSafeCharacters(base64);

		log.debug("generated: {} -> {}", base64, nutty);

		return nutty;
	}

	private
	String toUrlSafeCharacters(final String input)
	{
		final
		StringBuilder sb=new StringBuilder(input);

		sb.setLength(CHARS);

		for (int i=CHARS-1; i>=0; i--)
		{
			final
			char c=sb.charAt(i);

			if (c=='+') { sb.replace(i,i+1,"-"); } else
			if (c=='/') { sb.replace(i,i+1,"_"); }
		}

		return sb.toString();
	}

	@Override
	public
	byte[] fromStringValue(String value)
	{
		if (value==null || value.length()!=CHARS) return null;

		final
		StringBuilder sb=new StringBuilder(value);

		for (int i=CHARS-1; i>=0; i--)
		{
			final
			char c=sb.charAt(i);

			if (c=='-') { sb.replace(i,i+1,"+"); } else
			if (c=='_') { sb.replace(i,i+1,"/"); }
		}

		sb.append('=');

		final
		String base64=sb.toString();

		log.debug("from-string: {} -> {}", value, base64);

		try
		{
			return javax.xml.bind.DatatypeConverter.parseBase64Binary(base64);
		}
		catch (IllegalArgumentException e)
		{
			log.debug("bad (user-provided?) base64 string", e);
			return null;
		}
	}


}
