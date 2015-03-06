package com.allogy.qrauth.server.pages.api.ppp;

import com.allogy.qrauth.server.services.impl.Config;
import junit.framework.TestCase;
import org.testng.annotations.Test;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by robert on 3/6/15.
 */
@Test
public
class ChallengePPPTest extends TestCase
{
	private static final long ONE_WEEK = TimeUnit.DAYS.toMillis(7);

	private static final String[] TEST_NAMES = {"bob", "alice", "charles", "david"};

	@Test
	public
	void testFakeAccountChangeOverTime()
	{
		final
		long origin = Config.get().getOriginationTime();

		System.err.println("Fake PPP counter generation test...");

		final
		PrintStream out = System.out;

		for (int week=0; week<52; week++)
		{
			long now=origin+week*ONE_WEEK;

			out.print(week);

			for (String name : TEST_NAMES)
			{
				out.print(' ');
				long challenge=ChallengePPP.fakeChallenge(name, now);
				out.print(challenge);
			}

			out.println();
		}

		out.println("20 years pass...");

		final
		long future=origin+20*52*ONE_WEEK;

		for (int week=0; week<52; week++)
		{
			long now=future+week*ONE_WEEK;

			out.print(week);

			for (String name : TEST_NAMES)
			{
				out.print(' ');
				long challenge=ChallengePPP.fakeChallenge(name, now);
				out.print(challenge);
			}

			out.println();
		}

		out.println("200 years pass...");

		final
		long farFuture=origin+200*52*ONE_WEEK;

		for (int week=0; week<52; week++)
		{
			long now=farFuture+week*ONE_WEEK;

			out.print(week);

			for (String name : TEST_NAMES)
			{
				out.print(' ');
				long challenge=ChallengePPP.fakeChallenge(name, now);
				out.print(challenge);
			}

			out.println();
		}
	}
}
