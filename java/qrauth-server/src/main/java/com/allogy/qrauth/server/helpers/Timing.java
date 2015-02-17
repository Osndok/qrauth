package com.allogy.qrauth.server.helpers;

/**
 * Created by robert on 2/17/15.
 */
public
interface Timing
{
	void longestPath(long startTime);
	void shorterPath(long startTime);

	public static final
	Timing IGNORE = new Timing()
	{
		@Override
		public
		void longestPath(long startTime)
		{
			//no-op...
		}

		@Override
		public
		void shorterPath(long startTime)
		{
			//no-op...
		}
	};
}
