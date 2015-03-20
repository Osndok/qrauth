package com.allogy.qrauth.server.helpers;

import junit.framework.TestCase;
import org.testng.annotations.Test;

/**
 * Created by robert on 3/20/15.
 */
@Test
public
class StemmerTest extends TestCase
{
	@Test
	public
	void testStemming()
	{
		t("lovability", "lovabl");
		t("lovable", "lovabl");
		t("lovableness", "lovabl");
		t("lovably", "lovabl");
		t("lovage", "lovag");
		t("love", "love");
		t("lovebird", "lovebird");
		t("loveflower", "loveflow");
	}

	private
	void t(String full, String expectedReduction)
	{
		final
		String reduced=Stemmer.appliedTo(full);

		System.out.println(String.format("Stem(%s)->%s =?= %s", full, reduced, expectedReduction));
		assertEquals(expectedReduction, reduced);
	}
}
