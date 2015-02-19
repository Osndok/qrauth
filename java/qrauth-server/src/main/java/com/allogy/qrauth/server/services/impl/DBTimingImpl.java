package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.helpers.Timing;
import com.allogy.qrauth.server.services.DBTiming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by robert on 2/17/15.
 */
public
class DBTimingImpl implements DBTiming
{
	@Override
	public
	Timing concerning(String key)
	{
		//return Timing.IGNORE;
		return new LogTiming(key);
	}

	private static final
	Logger log = LoggerFactory.getLogger(DBTiming.class);

	private static final
	class LogTiming implements Timing
	{
		private
		final String key;

		private
		LogTiming(String key)
		{
			this.key = key;
		}

		@Override
		public
		void longestPath(final long startTime)
		{
			final
			long duration=System.currentTimeMillis()-startTime;

			log.debug("{} took {}ms (success, long path)", key, duration);
		}

		@Override
		public
		void shorterPath(long startTime)
		{
			final
			long duration=System.currentTimeMillis()-startTime;

			log.debug("{} took {}ms (failure, short path)", key, duration);
		}
	}
}
