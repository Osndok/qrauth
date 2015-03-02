package com.allogy.qrauth.server.entities;

import java.util.concurrent.TimeUnit;

import static com.allogy.qrauth.server.entities.AuthMethodGroup.*;

/**
 * Created by robert on 2/13/15.
 */
public
enum AuthMethod
{
	/*
	NB: You cannot simply change the name of these enums or delete a row, as theese names are stored in the database.
	To do either safely, one must perform a multi-step database migration (coordinated with the production manager).
	 */
	/**20-chars-max | RANK | DEADLINE | STATEFUL | 3rd PARTY | NETWORK | CLOCK | LEAKSAFE | P/W ENTRY | GROUP         */
	SQRL            (   1  , false    , true     , false     , true    , false , true     , false     , QR_ONLY       ),
	RSA             (   2  , false    , false    , false     , false   , false , true     , false     , RSA_CRAM      ),
	YUBIKEY_CUSTOM  (   3  , false    , true     , false     , false   , false , false    , true      , USER_AND_PASS ),
	HMAC_OTP        (   4  , false    , true     , false     , false   , false , false    , true      , USER_AND_PASS ),
	TIME_OTP        (   5  , false    , false    , false     , false   , true  , false    , true      , USER_AND_PASS ),
	PAPER_PASSWORDS (   7  , false    , true     , false     , false   , false , false    , true      , PPP_CRAM      ),
	STATIC_OTP      (   8  , false    , true     , false     , false   , false , false    , true      , USER_AND_PASS ),
	YUBIKEY_PUBLIC  (   9  , false    , false    , true      , true    , false , true     , true      , PASS_ONLY     ),
	OPEN_ID         (  10  , false    , false    , true      , true    , false , true     , false     , THIRD_PARTY   ),
	/* --- line of mandatory deadlines ------------------------------------------------------------------------------ */
	ROLLING_PASSWORD(  11  , true     , false    , false     , false   , false , false    , true      , USER_AND_PASS ),
	/* --- line of questionable security ---------------------------------------------------------------------------- */
	EMAILED_SECRET  (  12  , true     , false    , true      , true    , false , false    , false     , THIRD_PARTY   ),
	STATIC_PASSWORD (  13  , true     , false    , false     , false   , false , false    , true      , USER_AND_PASS ),
	;

	private static final int FIRST_QUESTIONABLE_RANK = 12;

	private static final long FIRST_RANK_MILLIS = TimeUnit.DAYS.toMillis(7);

	private final
	int rank;

	private final
	boolean deadlineRequired;

	private final
	boolean stateful;

	private final
	boolean thirdParty;

	private final
	boolean network;

	private final
	boolean clock;

	private final
	boolean leakSafe;

	private final
	boolean passwordEntry;

	private final
	AuthMethodGroup authMethodGroup;

	private
	AuthMethod(
				  int rank,
				  boolean deadlineRequired,
				  boolean stateful,
				  boolean thirdParty,
				  boolean network,
				  boolean clock,
				  boolean leakSafe,
				  boolean passwordEntry,
				  AuthMethodGroup authMethodGroup
	)
	{
		this.rank = rank;
		this.deadlineRequired = deadlineRequired;
		this.stateful = stateful;
		this.thirdParty = thirdParty;
		this.network = network;
		this.clock = clock;
		this.leakSafe = leakSafe;
		this.passwordEntry = passwordEntry;
		this.authMethodGroup = authMethodGroup;
	}

	/**
	 * A method is considered failsafe if it can be used in the absence of networking (and third parties).
	 * Put simply... networks down, machine's clock is reset, can you still log into your workstation?!?
	 *
	 * In particular, the requirement for a state-update is not deemed necessary to  be fail-safe, as in a
	 * fail-safe authentication scenario the proper continuity of the login method is not required.
	 *
	 * @return true if this method should suffice for logging into an isolated and timeless system (e.g. offline shell maintenance)
	 */
	public
	boolean isFailsafe()
	{
		return !(deadlineRequired | clock | network | thirdParty);
	}

	public
	int getRank()
	{
		return rank;
	}

	/**
	 * In general, an authentication method is required to have a deadline if it can be brute-forced
	 * or can be 'guessed'.
	 *
	 * Which is to say, given an arbitrary amount of time (and trials), is there even a slim chance
	 * that the (usually static) secret would be discovered within a longer-than-expected operational
	 * lifetime of the system (say... 200 years), and (if relevant) the magnitude of the user base
	 * (limited to the current population of Earth).
	 *
	 * @return
	 */
	public
	boolean isDeadlineRequired()
	{
		return deadlineRequired;
	}

	/**
	 * @return true if a proper/pure/secure implementation of this method would require a state change at login time
	 */
	public
	boolean isStateful()
	{
		return stateful;
	}

	/**
	 * @return true if contacting a 3rd party is required to actually authenticate using this method
	 */
	public
	boolean isThirdParty()
	{
		return thirdParty;
	}

	public
	boolean isNetwork()
	{
		return network;
	}

	/**
	 * Analog of if the server has "secrets to keep".
	 *
	 * @return true if the full disclosure of the authentication database would not compromise the authentication scheme (see also: offline attacks).
	 */
	public
	boolean isLeakSafe()
	{
		return leakSafe;
	}

	public
	AuthMethodGroup getAuthMethodGroup()
	{
		return authMethodGroup;
	}

	public
	boolean isQuestionable()
	{
		return rank >= FIRST_QUESTIONABLE_RANK;
	}

	public
	long getDefaultLoginLength()
	{
		return FIRST_RANK_MILLIS/rank;
	}

	/**
	 * @return true if (and only if) this auth method might involve receiving text in a password field
	 */
	public
	boolean usesPasswordEntry()
	{
		return passwordEntry;
	}
}
