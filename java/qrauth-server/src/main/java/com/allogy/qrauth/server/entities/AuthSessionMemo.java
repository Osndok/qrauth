package com.allogy.qrauth.server.entities;

import java.util.Date;

/**
 * This class serves as both a place for the AuthSession service to jot down 'memos' (primarily to remember who
 * is responsible/authenticated for the current request), and also to load/store those credentials directly from
 * the primary authentication cookie much like a wire protocol. See the AuthSession service more more details.
 *
 * TODO: this class *might* be best put in the private scope of the authSession class (if Tapestry supports it), so
 * that other classes are not tempted to pull it from the environment and muck with it. Although, it is easier to
 * audit here... as it *does* meet the definition of an 'entity'.
 */
public
class AuthSessionMemo implements Mortal
{
	public final
	long userAuthId;

	public final
	int userEpoch;

	public final
	Long userNameId;

	public final
	Long tenantSessionId;

	public final
	long deadline;

	public final
	long userId;

	/**
	 * NB: this value is not endemic to our cause, but is primarily to just get an idea of which tenants bring the most traffic.
	 * This value sticks until the user re-authenticates...
	 */
	public final
	Long tenantId;

	public
	AuthSessionMemo(String bundle)
	{
		final
		String[] bits = bundle.split(":");

		userAuthId = Long.parseLong(bits[0]);
		userEpoch = Integer.parseInt(bits[1]);
		userNameId = optionalLong(bits, 2);
		tenantSessionId = optionalLong(bits, 3);
		deadline = Long.parseLong(bits[4]);
		userId = Long.parseLong(bits[5]);
		tenantId = optionalLong(bits, 3);

		toStringCache = bundle;
	}

	@Override
	public
	String toString()
	{
		if (toStringCache==null)
		{
			//WARNING: this must be parsable by the one-string constructor above.
			toStringCache=
				userAuthId + ":" +
					userEpoch + ":" +
					orEmpty(userNameId) + ":" +
					orEmpty(tenantSessionId) + ":" +
					deadline + ":" +
					userId + ":" +
					orEmpty(tenantId)
			;
		}
		return toStringCache;
	}

	private static
	Long optionalLong(String[] bits, int i)
	{
		if (bits[i].isEmpty())
		{
			return null;
		}
		else
		{
			return new Long(bits[i]);
		}
	}

	private transient
	String toStringCache;

	public
	AuthSessionMemo(DBUserAuth userAuth, Username username, TenantSession tenantSession, Date sessionDeadline)
	{
		final
		DBUser user=userAuth.user;

		this.userId = user.id;
		this.userAuthId=userAuth.id;
		this.userEpoch = user.epoch;
		this.userNameId=(username==null?null:username.id);
		this.tenantSessionId=(tenantSession==null?null:tenantSession.id);
		this.tenantId=(tenantSession==null?null:tenantSession.tenant.id);
		this.deadline=sessionDeadline.getTime();
	}

	private
	String orEmpty(Long value)
	{
		if (value==null)
		{
			return "";
		}
		else
		{
			return Long.toString(value);
		}
	}

	@Override
	public
	String getDeathMessage()
	{
		return null;
	}

	@Override
	public
	Date getDeadline()
	{
		return new Date(deadline);
	}

	public transient
	DBUserAuth dbUserAuth;

}
