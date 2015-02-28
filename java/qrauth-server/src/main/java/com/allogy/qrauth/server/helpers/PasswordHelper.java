package com.allogy.qrauth.server.helpers;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by robert on 2/28/15.
 */
public
class PasswordHelper
{
	public static
	int gaugeStrength(String password)
	{
		final
		int l=password.length();

		final
		Set<CharacterClass> set=new HashSet<CharacterClass>(10);

		int transitions=0;

		CharacterClass lastClass=classOf(password.charAt(0));

		for (int i=0; i<l; i++)
		{
			final
			CharacterClass thisClass=classOf(password.charAt(i));

			set.add(thisClass);

			if (thisClass!=lastClass) transitions++;

			lastClass=thisClass;
		}

		int bonus=0;

		if (l>5) bonus++;
		if (l>10) bonus++;
		if (l>15) bonus++;
		if (l>20) bonus++;

		return set.size()*3+transitions*2+bonus;
	}

	public static final int DICTIONARY_WORD_STRENGTH=gaugeStrength("password");

	private static
	CharacterClass classOf(char c)
	{
		if (c>='a' && c<='z') return CharacterClass.LOWER;
		if (c>='A' && c<='Z') return CharacterClass.UPPER;
		if (c>='0' && c<='9') return CharacterClass.NUMERAL;
		if (c==' ') return CharacterClass.WHITE;
		if (c=='.' || c==',' || c==';' || c==':' || c=='!' || c=='?' ) return CharacterClass.PUNCTUATION;

		return CharacterClass.OTHER;
	}

	private
	enum CharacterClass
	{
		LOWER,
		UPPER,
		WHITE,
		NUMERAL,
		PUNCTUATION,
		OTHER
	}
}
