package com.allogy.qrauth.server.services.impl;

import com.allogy.qrauth.server.helpers.Timing;
import com.allogy.qrauth.server.services.DBTiming;

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
		return Timing.IGNORE;
	}
}
