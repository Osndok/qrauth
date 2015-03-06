package com.allogy.qrauth.server.helpers;

/**
 * Created by robert on 3/6/15.
 */
public abstract
class UsernameScanDetector
{
	public abstract
	void usernameNotFound(String username, String ipAddress);

	private static
	UsernameScanDetector INSTANCE;

	public static
	UsernameScanDetector get()
	{
		if (INSTANCE==null)
		{
			INSTANCE=Plugins.get(UsernameScanDetector.class, NO_OP_IMPL);
		}

		return INSTANCE;
	}

	private static
	UsernameScanDetector NO_OP_IMPL = new UsernameScanDetector()
	{
		@Override
		public
		void usernameNotFound(String username, String ipAddress)
		{
			//no-op...
		}
	};
}
