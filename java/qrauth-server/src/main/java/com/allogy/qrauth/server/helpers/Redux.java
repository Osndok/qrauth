package com.allogy.qrauth.server.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * WARNING: changing this model requires re-processing all effected database records, and
 * might break any required interoperability.
 *
 * Pretty much a direct java port of "dp-accounts" redux_model algorithm.
 * Created by robert on 3/20/15.
 */
public
class Redux
{

	/*
	# "3-char words that are likely to distinguish one market from another"
	# mostly names, so roughly: grep "^..." propernames | sort | uniq
	# hand-picked most common ones... try to skip adjectives (like "few").
	# some are odd (like amy & "ev") because they are "stemmed" first.
	TODO: consider making this a map, it might speed the lookups.
	*/
	private static final
	Set<String> _three_character_lowercase_common_names_and_uncommon_words;

	static
	{
		_three_character_lowercase_common_names_and_uncommon_words=new HashSet<String>();
		Collections.addAll(_three_character_lowercase_common_names_and_uncommon_words, "ami", "ann", "art", "ben", "bob", "dan", "dog", "don", "ev", "ian", "jen", "joe", "ken",
							  "kim", "lea", "pam", "pat", "rai", "rob", "rod", "ron", "roi", "sal", "sam", "son", "spy",
							  "ted", "jem", "gem", "jew", "leo", "job", "bee", "bra", "bun", "bum", "cat", "cow", "cue",
							  "ey", "end", "fan", "fox", "fry", "gel", "gin", "goo", "hop", "ink", "inn", "jam", "joi",
							  "lag", "mac", "od", "pod", "rap", "rat", "rep", "rip", "sew", "sex", "see", "shy", "sin",
							  "sly", "sod", "sum", "soi", "tea", "tan", "on", "two", "six", "tar", "toi", "yen", "zoo",
							  "acr", "arc", "god"
							  );
	}

	/*
	# This could *certainly* be improved (maybe with actual word-number interpretation?), but
	# it doesn"t have to be perfect... in fact, this is all a bit over-the-top.
	#
	# In testing, the only words in this list which reduces using the stemming algorithim is
	# "one"->"on" and "hundred"=>"hundr", so they are used thusly and we will apply this
	# mapping *after* stemming.
	*/
	private static final
	Map<String, String> _word_to_number_mapping;

	static
	{
		final
		Map<String,String> a=new HashMap<String, String>();

		a.put("zero"   , "0");
		a.put("on"     , "1");
		a.put("two"    , "2");
		a.put("three"  , "3");
		a.put("four"   , "4");
		a.put("five"   , "5");
		a.put("six"    , "6");
		a.put("seven"  , "7");
		a.put("eight"  , "8");
		a.put("nine"   , "9");
		a.put("ten"    , "1"); //TODO: explain why the tens are "1" here, or find out why this is wrong.
		a.put("first"  , "1");
		a.put("second" , "2");
		a.put("third"  , "3");
		a.put("fourth" , "4");
		a.put("fifth"  , "5");
		a.put("sixth"  , "6");
		a.put("seventh", "7");
		a.put("eigth"  , "8");
		a.put("ninth"  , "9");
		a.put("tenth"  , "1");
		/*a.put("hundr", "1e2");*/
		a.put("thousand", "1");
		a.put("million" , "1e6");
		a.put("billion" , "1e7");
		/* Everybody wants to be the "only" one... so we"ll collect those synonyms */
		a.put("onli"    , "1");
		a.put("alon"    , "1");
		a.put("exclus"  , "1");
		a.put("singular", "1");
		a.put("solitari", "1");
		a.put("uniqu"   , "1");
		a.put("singl"   , "1");
		a.put("sole"    , "1");

		/* It"s less often for words to mean "two", but still common enough... */
		a.put("dual"    , "2");
		a.put("doubl"   , "2");
		a.put("pair"    , "2");
		a.put("coupl"   , "2");
		a.put("binari"  , "2");
		a.put("twice"   , "2");

		_word_to_number_mapping= Collections.unmodifiableMap(a);
	}

	private static final
	String _pre_stem_remove = "on";

	private static final
	Logger log = LoggerFactory.getLogger(Redux.class);

	/**
	 * Given a user-provided string, return a related string that is very likely to collide with
	 * another market that has a similar or confusing name.
	 *
	 * For example, it would be nice if: redux("Bobs place") == redux("Place of Bob")
	 */
	public static
	String digest(String name, Set<String> dirty_words)
	{
		//class_exists("PorterStemmer") or GLOBALS["CI"]->load->helper("stemming_helper.php");
		log.debug("digest: '{}'", name);

		String s = _numeric_split(name);

		log.debug("split: '{}'", s);

		//Periods have mixed meanings ("1.3.3", versus "john.doe").
		//Potentially useful as a word boundary (replace with space), but more likely to be numeric (replace with nothing)
		//s=str_replace(".", "", s); [integrated into _numeric_split as a dirty character]
		s = s.toLowerCase();

		String[] words_in = s.split("\\b");
		List<String> words_out = new ArrayList<String>(words_in.length);
		Set<String> repeats = new HashSet<String>();

		for (String w : words_in)
		{
			if (w.isEmpty())
			{
				continue;
			}

			//Consider the "Stemmed" version of the word (which *might* reduce it to three characters).
			if (is_numeric(w))
			{
				log.debug("numeric: '{}'", w);

				//strip: "+-." (period already stripped out)
				//XXX: w=preg_replace("/[+-\\.]/i", "", w);
				//w=str_replace("+-", "", w); [integrated into _numeric_split as a dirty character]
				//trim zeros from both sides...
				w = trimLeadingAndTrailingZeros(w);
				//In general, accept all numerics (no 3-letter-word stuff)
				words_out.add(w);
				log.debug("out: '{}'", w);
				continue;
			}

			log.debug("in: '{}'", w);

			//usual: w=preg_replace("/[^a-zA-Z]/i", "", w); //alphanumeric only
			w = w.replaceAll("[^0-9a-zA-Z]", ""); //no numbers in-words.

			log.debug("1: '{}'", w);

			//------PRE-STEMMING-CHECKS-------

			if (in_array(w, _pre_stem_remove))
			{
				continue;
			}

			if (dirty_words != null && dirty_words.contains(w))
			{
				continue;
			}

			//--------------------------------

			//w=PorterStemmer::Stem(w);
			w = Stemmer.appliedTo(w);

			log.debug("stemmed: '{}'", w);

			if (_word_to_number_mapping.containsKey(w))
			{
				//It"s a number... in word form!
				w = _word_to_number_mapping.get(w);
				log.debug("to_num: '{}'", w);

				//Unlike arabic numbers, we will dupe-suppress wordy numbers...
				if (!repeats.contains(w))
				{
					words_out.add(w);
					repeats.add(w);
					log.debug("out: '{}'", w);
				}
				continue;
			}

			final
			int l = w.length();

			//Is it a specially-allowed word? (three letters, two... one?!?!)
			if (_three_character_lowercase_common_names_and_uncommon_words.contains(w))
			{
				log.debug("un3: '{}'", w);

				if (!repeats.contains(w))
				{
					words_out.add(w);
					repeats.add(w);
					log.debug("out: '{}'", w);
				}
			}
			else if (l <= 3)
			{
				//drop it, no other words under three characters are "significant" in this definition.
			}
			else
			{
				if (!repeats.contains(w))
				{
					words_out.add(w);
					repeats.add(w);
					log.debug("out: '{}'", w);
				}
			}
		}

		Collections.sort(words_out);
		return implode(words_out);
	}

	private static
	String implode(List<String> strings)
	{
		final
		StringBuilder sb = new StringBuilder();

		for (String string : strings)
		{
			sb.append(string);
		}

		final
		String retval=sb.toString();

		log.debug("retval: '{}'", retval);
		return retval;
	}

	//Ha, ha... there's a misnomer for you.
	private static
	boolean in_array(String w, String notReallyAndArray)
	{
		return w.equals(notReallyAndArray);
	}

	private static
	String trimLeadingAndTrailingZeros(String s)
	{
		if (s.charAt(0) == '0' || s.charAt(s.length() - 1) == '0')
		{
			StringBuilder sb = new StringBuilder(s);

			while (sb.charAt(0) == '0') sb.deleteCharAt(0);
			while (sb.charAt(sb.length() - 1) == '0') sb.deleteCharAt(sb.length() - 1);

			return sb.toString();
		}
		else
		{
			return s;
		}
	}

	private static
	boolean is_numeric(String s)
	{
		final
		int l=s.length();

		for (int i=0; i<l; i++)
		{
			final
			char c=s.charAt(i);

			if (!Character.isDigit(c))
			{
				return false;
			}
		}

		return true;
	}

	private static
	String _numeric_split(String s)
	{
		int l=s.length();
		boolean last_was_alpha=false;

		StringBuilder r=new StringBuilder();

		for (int i=0; i<l; i++)
		{
			char c=s.charAt(i);

			if (_is_alpha(c))
			{
				if (!last_was_alpha)
				{
					r.append(' ');
					last_was_alpha=true;
				}
			}
			else
			if (last_was_alpha)
			{
				last_was_alpha=false;
				r.append(' ');
			}

			//REH: minor deviation, remove periods here rather than another trip through the array.
			if (dirty_character(c))
			{
				r.append(' ');
			}
			else
			{
				r.append(c);
			}
		}

		return r.toString();
	}

	private static
	boolean dirty_character(char c)
	{
		return c=='.' || c=='+' || c=='-';
	}

	private static
	boolean _is_alpha(char c)
	{
		/*
		o=ord(c);
		if (o>=ord("a") && o<=ord("z")) return true;
		if (o>=ord("A") && o<=ord("Z")) return true;
		return FALSE;
		*/
		return Character.isAlphabetic(c);
	}

}
