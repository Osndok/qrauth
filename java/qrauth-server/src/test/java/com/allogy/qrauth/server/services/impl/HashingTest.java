package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.entities.UnimplementedHashFunctionException;
import com.allogy.qrauth.server.services.Hashing;
import junit.framework.TestCase;
import org.testng.annotations.Test;

/**
 * Created by robert on 2/18/15.
 */
@Test
public
class HashingTest extends TestCase
{
	private final
	String pepper="pepper";

	@Test
	public
	void testDistinctDigestion()
	{
		final
		String sameInput="sameInput";

		final
		String once=HashingImpl.digest(pepper, sameInput);

		for (int i=0; i<100; i++)
		{
			assertNotSame(once, HashingImpl.digest(pepper, sameInput));
		}
	}

	@Test
	public
	void testRejection() throws UnimplementedHashFunctionException
	{
		//Make sure that the digestMatch() function will reject *something*...
		assertFalse(HashingImpl.match(pepper, "alpha", "1:"));
		assertFalse(HashingImpl.match(pepper, "alpha", "1:bbbe0042a7fba35c"));
		assertFalse(HashingImpl.match(pepper, "alpha", "1:bbbe0042a7fba35c:"));
		assertFalse(HashingImpl.match(pepper, "alpha", "1:bbbe0042a7fba35c:4"));
		assertFalse(HashingImpl.match(pepper, "alpha", "1:bbbe0042a7fba35c:423b53d7542c9227a624991230cd08061d50efd3 ")); // <-- extra space at the end!
		assertFalse(HashingImpl.match(pepper, "", "1:c:c"));
	}

	@Test
	public
	void testNPEs() throws UnimplementedHashFunctionException
	{
		try
		{
			HashingImpl.match(null, "alpha", "1:c:c");
			throw new AssertionError("expecting a NPE");
		}
		catch (NullPointerException e)
		{
			//good...
		}

		try
		{
			HashingImpl.match(pepper, null, "1:c:c");
			throw new AssertionError("expecting a NPE");
		}
		catch (NullPointerException e)
		{
			//good...
		}

		try
		{
			HashingImpl.match(pepper, "alpha", null);
			throw new AssertionError("expecting a NPE");
		}
		catch (NullPointerException e)
		{
			//good...
		}

	}

	@Test
	public
	void testDigestBackwardsCompatibility() throws UnimplementedHashFunctionException
	{
		//NB: it is *NEVER-OKAY* to simply change these values to get the test to work!
		//    Hashed values are stored in the database, and must be able to resolve.
		assertTrue(HashingImpl.match(pepper, "alpha", "1:bbbe0042a7fba35c:423b53d7542c9227a624991230cd08061d50efd3"));
		assertTrue(HashingImpl.match(pepper, "alpha", "1:c8774457581d7220:579ff2bfb7363d962f9421862d07e5e6e1802deb"));
		assertTrue(HashingImpl.match(pepper, "beta" , "1:63fca61ab8b4cf70:e0bf584080be217d8a0f6ed7aaef26aaf93857c2"));
		assertTrue(HashingImpl.match(pepper, "beta" , "1:85b464196e545537:0e559fb8a9dc056235eb785c9eac3c6524ce5745"));

		// ----> It *is* okay to add more tests here <----

		hint("gamma", HashingImpl.digest(pepper, "gamma"));
		hint("gamma", HashingImpl.digest(pepper, "gamma"));
		hint("delta", HashingImpl.digest(pepper, "delta"));
		hint("delta", HashingImpl.digest(pepper, "delta"));
	}

	private
	void hint(String userInput, String hashedValue)
	{
		String helpfulHint=
			String.format("assertTrue(HashingImpl.digestMatch(pepper, \"%s\", \"%s\"));",
							 userInput,
							 hashedValue
			);
		System.err.println(helpfulHint);
	}

	@Test
	public
	void testLoadableAndUpToDate()
	{
		Hashing hashing=One.registry.getService(Hashing.class);

		//NB: This is placed here as a reminder that if you update the preferred algorithm, you must *also* add backwards compatibility tests directly above, which 'prompt()' should have already given you...
		assertFalse(hashing.needsUpdate("1:algorithm"));
	}

	@Test
	public
	void testDatabaseLookupBackwardsCompatibility()
	{
		//NB: it is *NEVER-OKAY* to simply change these strings in order to get the test to work!
		//    ...instead, if you are looking to migrate to a different lookup hash, that would start with additional key fields.
		assertEquals("68faae1f21f207170b317e856ad4fa59ebc256d9", HashingImpl.forDatabaseLookupKey(pepper, "alpha"));
		assertEquals("a0d7b5489aeee8eb4d3ee98a58fa2d669794ec36", HashingImpl.forDatabaseLookupKey(pepper, "beta"));
		assertEquals("e22931bf0cd6b2a22d1d587c8d93c50ec27a894f", HashingImpl.forDatabaseLookupKey(pepper, "gamma"));
		assertEquals("1a645e88eaf2f45de0d8b8bc542eec8af514df28", HashingImpl.forDatabaseLookupKey(pepper, "delta"));
	}

	@Test
	public
	void testHmac() throws UnimplementedHashFunctionException
	{
		final
		Hashing hashing=One.registry.getService(Hashing.class);

		subTestHmac(hashing, "alpha");
		subTestHmac(hashing, "beta");
		subTestHmac(hashing, "gamma");
		subTestHmac(hashing, "alpha:beta:gamma");
		subTestHmac(hashing, "");
	}

	private
	void subTestHmac(Hashing hashing, String input) throws UnimplementedHashFunctionException
	{
		final
		String prefixed=hashing.withHmacPrefix(input);

		final
		String output=hashing.fromHmacPrefixed(prefixed);

		assertEquals(input, output);
	}
}
