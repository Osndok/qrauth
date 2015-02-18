package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.entities.UnimplementedHashFunctionException;
import com.allogy.qrauth.server.services.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An extensible general-purpose hashing service, which ought to provide adequate security for
 * password storage, but is primarily focused on performance (as we don't expect to use many
 * passwords here on out) and ease of implementation (as we will likely need to replicate it
 * in C too).
 */
public
class HashingImpl implements Hashing
{
	private static final Logger  log;
	private static final String  PEPPER;
	private static final Charset UTF8;

	static
	{
		log = LoggerFactory.getLogger(Hashing.class);
		PEPPER = DatabaseMigratorImpl.getHashingPepper();

		if (PEPPER.isEmpty())
		{
			log.error("hashing.pepper property is EMPTY");
		}

		UTF8=Charset.forName("UTF-8");
	}

	private static final boolean HEAVY_LOAD = Boolean.getBoolean("HEAVY_LOAD");

	private static final String ALGO_SEASONED_SHA1 = "1:";

	private static final String BAD_MATCH = "the matches() function does not accept the current digest() function output";

	@Override
	public
	String digest(final String userInput)
	{
		return digest(PEPPER, userInput);
	}

	public static
	String digest(final String pepper, final String userInput)
	{

		final
		String salt = generateArbitrarySaltValue();

		final
		String retval = ALGO_SEASONED_SHA1 + salt + ":" + seasonedSHA1(salt, pepper, userInput);

		log.debug("digest() -> {}", retval);

		if (!HEAVY_LOAD)
		{
			try
			{
				if (!match(pepper, userInput, retval))
				{
					throw new AssertionError(BAD_MATCH);
				}
			}
			catch (UnimplementedHashFunctionException e)
			{
				throw new AssertionError(BAD_MATCH);
			}
		}
		return retval;
	}

	private static
	String generateArbitrarySaltValue()
	{
		final
		Random random = ThreadLocalRandom.current();

		return Long.toHexString(random.nextLong()).toLowerCase();
	}

	@Override
	public
	boolean match(final String userInput, final String hashedValue) throws UnimplementedHashFunctionException
	{
		return match(PEPPER, userInput, hashedValue);
	}

	public static
	boolean match(final String pepper, final String userInput, final String hashedValue) throws UnimplementedHashFunctionException
	{
		if (hashedValue.startsWith(ALGO_SEASONED_SHA1))
		{
			final
			int saltEnds=hashedValue.indexOf(':', ALGO_SEASONED_SHA1.length()+1);

			if (saltEnds>0)
			{
				final
				String salt=hashedValue.substring(ALGO_SEASONED_SHA1.length(), saltEnds);

				final
				String expectedHash=hashedValue.substring(saltEnds+1);

				log.debug("salt={}, expected={}", salt, expectedHash);

				final
				String actualHash=seasonedSHA1(salt, pepper, userInput);

				final
				boolean retval=actualHash.equals(expectedHash);

				log.debug("actual={}, retval={}", actualHash, retval);

				return retval;
			}
			else
			{
				log.warn("no salt?");
				return false;
			}
		}
		else
		{
			throw new UnimplementedHashFunctionException();
		}
	}

	@Override
	public
	boolean needsUpdate(final String hashedValue)
	{
		return false;
	}

	@Override
	public
	String forDatabaseLookupKey(String userInput)
	{
		return forDatabaseLookupKey(PEPPER, userInput);
	}

	/**
	 * This function exists to help ensure that the database-lookup function remains backwards
	 * compatible via a test function.
	 */
	public static
	String forDatabaseLookupKey(String pepper, String userInput)
	{
		return seasonedSHA1("database-lookup", pepper, userInput);
	}

	public static final
	ThreadLocal<MessageDigest> SHA1 = new ThreadLocal<MessageDigest>()
	{
		@Override
		protected
		MessageDigest initialValue()
		{
			try
			{
				return MessageDigest.getInstance("SHA-1");
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new AssertionError(e);
			}
		}
	};

	private static
	String seasonedSHA1(String salt, String pepper, String userInput)
	{
		final
		MessageDigest sha1 = SHA1.get();

		sha1.reset();

		sha1.update(salt.getBytes(UTF8));
		sha1.update(pepper.getBytes(UTF8));
		sha1.update(userInput.getBytes(UTF8));

		final
		byte[] bytes=sha1.digest();

		return javax.xml.bind.DatatypeConverter.printHexBinary(bytes).toLowerCase();
	}

}
