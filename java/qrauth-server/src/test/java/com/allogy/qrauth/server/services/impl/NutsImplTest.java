package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.services.Nuts;
import junit.framework.TestCase;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * Created by robert on 2/16/15.
 */
@Test
public
class NutsImplTest extends TestCase
{
	@Test
	public
	void testNutGeneration()
	{
		final
		Nuts nuts=new NutsImpl();

		for (int i=0; i<1000; i++)
		{
			generateAndTestOneNut(nuts);
		}
	}

	private
	void generateAndTestOneNut(Nuts nuts)
	{
		final
		byte[] originalBytes=nuts.generateBytes();

		assertNotNull(originalBytes);
		assertEquals(NutsImpl.BYTES, originalBytes.length);
		assertEquals(NutsImpl.BITS, NutsImpl.BYTES*8);

		final
		String stringValue=nuts.toStringValue(originalBytes);

		assertNotNull(stringValue);
		assertEquals(NutsImpl.CHARS, stringValue.length());

		final
		byte[] back=nuts.fromStringValue(stringValue);

		assertNotNull(back);
		assertTrue(Arrays.equals(originalBytes, back));
	}
}
