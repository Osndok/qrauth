package com.allogy.qrauth.server.services;

import com.allogy.qrauth.server.entities.UnimplementedHashFunctionException;

import java.util.List;

/**
 * Created by robert on 2/18/15.
 */
public
interface Hashing
{
	/**
	 * Returns a String that is cryptographically related to the given userInput and the current system deployment that
	 * is unlikely to be particularly useful to a 3rd party or as a database key/index.
	 *
	 * @param userInput - the unhashed, user-provided input string which we may need to verify later, but never need to straightly recall
	 * @return a string that is very unlikely to collide with any other given input, and yet will surly trigger a positive 'match' in the counterposing match() function.
	 */
	String digest(String userInput);

	/**
	 * @param userInput - the unhashed, user-provided input string to verify
	 * @param hashedValue - the previously-hashed value (e.g. from the database)
	 * @return true if (and only if) the given input/hashedValue combination is overwhelmingly likely a result of this same service
	 * @throws UnimplementedHashFunctionException if the given hashedValue
	 */
	boolean match(String userInput, String hashedValue) throws UnimplementedHashFunctionException;

	/**
	 * @param hashedValue - the previously-hashed value (e.g. from the database)
	 * @return true if (and only if) the provided hashedValue uses an old hashing algorithim, and should be replaced with a new digest()
	 */
	boolean needsUpdate(String hashedValue);

	/**
 	 * Returns a String that is cryptographically related to the given userInput and the current system deployment that
	 * is suitable for use as a database key/index insomuch as for any given userInput, this function should now (and
	 * forever) provide the same output.
	 *
	 * This is for those times that a hash is used primary as identification (e.g. looking up an API key in a database
	 * table), in which we find that we cannot embed 'salt' in the field (as that by definition would generate a new
	 * value each time).
	 *
	 * @param userInput - the unhashed, user-provided input string which we will use to identify a record
	 * @return a very-stable string that
	 */
	String forDatabaseLookupKey(String userInput);
}
