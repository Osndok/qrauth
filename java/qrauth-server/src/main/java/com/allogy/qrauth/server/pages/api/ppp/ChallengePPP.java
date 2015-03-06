package com.allogy.qrauth.server.pages.api.ppp;

import com.allogy.qrauth.server.entities.AuthMethod;
import com.allogy.qrauth.server.entities.DBUser;
import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.entities.Username;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.helpers.PPP_Engine;
import com.allogy.qrauth.server.helpers.Timing;
import com.allogy.qrauth.server.helpers.UsernameScanDetector;
import com.allogy.qrauth.server.services.DBTiming;
import com.allogy.qrauth.server.services.Network;
import com.allogy.qrauth.server.services.Policy;
import com.allogy.qrauth.server.services.impl.Config;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.util.TextStreamResponse;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Given a username, which may or may not map to a known user account, which may
 * or may not have PPP enabled, generate a challenge such that an account that
 * *does* have PPP cards can readily satisfy, and that an account that does not
 * exist (or does not have PPP enabled) is indistinguishable from one that does,
 * or at least... however close to that ideal as we can get.
 *
 * Created by robert on 3/6/15.
 *
 * NB: This class is pre-authentication, so extra care should be taken.
 */
public
class ChallengePPP
{
	Object onActivate()
	{
		return new ErrorResponse(400, "missing or empty username");
	}

	//For qualifying the semi-stable psuedo-random challenges, allow any time to be specified... in development mode!
	private
	Long simulatedWallTime;

	void onActivate(String username, long wallTime)
	{
		if (!productionMode)
		{
			this.simulatedWallTime = wallTime;
		}
	}

	@Inject
	@Symbol(SymbolConstants.PRODUCTION_MODE)
	private
	boolean productionMode;

	@Inject
	private
	Policy policy;

	@Inject
	private
	DBTiming dbTiming;

	Object onActivate(String userProvidedUsername)
	{
		final
		long startTime = System.currentTimeMillis();

		final
		Timing pppTiming = dbTiming.concerning("PPP");

		final
		String matchableUsername = policy.usernameMatchFilter(userProvidedUsername);

		final
		DBUserAuth pppUserAuth = getPPPUserAuthFor(matchableUsername);

		final
		String retval;
		{
			if (pppUserAuth == null)
			{
				final
				long counter = fakeChallenge(matchableUsername,
												(simulatedWallTime == null ? startTime : simulatedWallTime));

				retval = PPP_Engine.getChallenge(counter);

				//This is deemed the longer path, b/c fakeChallenge might end up doing additional database queries
				pppTiming.longestPath(startTime);
			}
			else
			{
				retval = realChallenge(pppUserAuth);
				pppTiming.shorterPath(startTime);
			}
		}

		log.debug("challenge: '{}'", retval);

		return new TextStreamResponse("text/plain", retval);
	}

	private
	String realChallenge(DBUserAuth userAuth)
	{
		//PPP_Engine pppEngine=from(userAuth.secret);
		return "unimplemented:real";
	}

	public static
	long fakeChallenge(String username, long now)
	{
		/*
		What we want to accomplish is a return value that is:
		(1) stable enough that if they enter the same username *soon*, they will get the same challenge (i.e. we can't just print a random challenge), and yet
		(2) unstable enough that (over a certain period of time), it will change as if a user is (however slowly) using the PPP cards, and
		(3) stable across clusters (so we can't generate and stash a nonce... but we can use the cluster pepper!), and
		(4) isolated enough that any small update to it's constituents (such as stats, or a person we are following) will not cause a mass-update to all fake challenges, and
		(5) non-uniform enough so that (at the moment a fake challenge changes) it will not cause a mass-update to all fake challenges, and
		(6) unpredictable enough that if someone has the complete source code to this application, they cannot 'test' against the fake challenge generator's output, period, etc.

		It helps that 'username' is already a stabilized value (i.e. if it exists or not, equivalent usernames
		[like 'bob' and 'BoB'] are reduced) before we get to this function.

		NB: any change to this algorithm will, unfortunately, cause a mass-update effect too; as well as a differential signal (if they can compare different major versions).

		NB: if we were to simply follow a particular user (salted with the given username), and be assured it will change at whatever rate a real user uses his/her ppp cards.
		 */
		final
		MessageDigest md;
		{
			try
			{
				md = MessageDigest.getInstance("SHA-256");
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new AssertionError(e);
			}
		}
		
		md.reset();
		md.update(Config.get().getHashingPepper().getBytes());
		md.update(username.getBytes());

		/*
		 * This random utility will produce the same path per (username,cluster) tuple.
		 */
		final
		Random userClusterStable=new Random(toLong(md.digest()));

		/*
		Ranges roughly from 0 to 2*EXPECTED_PPP_USAGE_INTERVAL, per-user.
		 */
		final
		long thisUsersUpdatePeriod=MINIMUM_PPP_USAGE_INTERVAL+EXPECTED_PPP_USAGE_INTERVAL+(userClusterStable.nextLong() % EXPECTED_PPP_USAGE_INTERVAL);

		/*
		Ranges, roughly, from 0 to EXPECTED_PPP_USAGE_INTERVAL, per-user.
		 */
		final
		long thisUsersUpdateOffset=MINIMUM_PPP_USAGE_INTERVAL+Math.abs(userClusterStable.nextLong() % EXPECTED_PPP_USAGE_INTERVAL);

		//NB: this changes every millisecond
		final
		long systemRuntime = now-OLDEST_POSSIBLE_ACCOUNT;

		/*
		Having a per-account acceptable window allows us to have plausibly low counters even into the
		distant future (otherwise new accounts would really stick out), at the cost of particular fake
		accounts having much lower reset intervals... which means if you were to be watching such an
		account for long enough... you might notice the reset.
		 */
		final
		long accountCap = SHORTEST_EXPECTED_PPP_TRACK + userClusterStable.nextInt(LONGEST_EXPECTED_PPP_TRACK);

		/*
		 * This is *nearly* the counter we are looking for... except that it is always origin-based,
		 * and monotonically increasing. Meaning that real accounts (which start at password 0) will
		 * eventually stick out like a sore thumb.
		 */
		final
		long counterFromOrigin = ( (systemRuntime+thisUsersUpdateOffset)/(thisUsersUpdatePeriod) );

		return counterFromOrigin % accountCap;
	}

	private static final
	UsernameScanDetector usernameScanDetector=UsernameScanDetector.get();

	private static final Logger log = LoggerFactory.getLogger(ChallengePPP.class);

	/*
	 * Originally a clever way to avoid division-by-zero, now a reasonable tunable.
	 */
	private static final long MINIMUM_PPP_USAGE_INTERVAL = TimeUnit.HOURS.toMillis(1);

	/*
	In the worst case, this roughly translates into:
	"how long should we make a lurker watch an account to determine if they use PPP",

	as (due to the nature and constraints of the fake-out algorithm) the next value presented might be
	implausible (if it runs over a modulus boundary), and if you watch it for a *really* long time, you
	can probably pick out a regular interval with a high degree of certainty.
	 */
	private static final long EXPECTED_PPP_USAGE_INTERVAL = TimeUnit.DAYS.toMillis(3 * 30);

	private static final long A_VERY_LONG_HUMAN_LIFETIME = TimeUnit.DAYS.toMillis(150 * 365);

	/*
	 * This helps to disguise new accounts, by
	 */
	private static final int SHORTEST_EXPECTED_PPP_TRACK = 30;

	/*
	How many PPP passwords will the average user use until they regenerate their key, or their account becomes obsolete?
	 */
	private static final int LONGEST_EXPECTED_PPP_TRACK = (int) (A_VERY_LONG_HUMAN_LIFETIME / EXPECTED_PPP_USAGE_INTERVAL);

	private static final long OLDEST_POSSIBLE_ACCOUNT = Config.get().getOriginationTime();

	private static
	long toLong(byte[] bytes)
	{
		long value = 0;

		for (int i = 0; i < bytes.length; i++)
		{
			int wrapped = (i % 8);
			value ^= ((long) bytes[i] & 0xffL) << (8 * wrapped);
		}

		return value;
	}

	@Inject
	private
	Session session;

	@Inject
	private
	Network network;

	private
	DBUserAuth getPPPUserAuthFor(String matchableUsername)
	{
		final
		DBUser dbUser = (DBUser)
							session.createCriteria(Username.class)
								.add(Restrictions.eq("matchValue", matchableUsername))
								.uniqueResult();

		if (dbUser == null)
		{
			log.debug("username dne:{}", matchableUsername);
			usernameScanDetector.usernameNotFound(matchableUsername, network.getIpAddress());
			return null;
		}

		return (DBUserAuth)
				   session.createCriteria(DBUserAuth.class)
					   .add(Restrictions.eq("user", dbUser))
					   .add(Restrictions.eq("authMethod", AuthMethod.PAPER_PASSWORDS))
					   .uniqueResult()
			;
	}
}
