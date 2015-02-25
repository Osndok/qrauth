package com.allogy.qrauth.server.entities;

import java.util.Date;

/**
 * This class serves as both a place for the AuthSession service to jot down 'memos' (primarily to remember who
 * is responsible/authenticated for the current request), and also to load/store those credentials directly from
 * the primary authentication cookie much like a wire protocol. See the AuthSession service more more details.
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

	public
	AuthSessionMemo(String bundle)
	{
		final
		String[] bits = bundle.split(":");

		userAuthId = Long.parseLong(bits[0]);
		userEpoch = Integer.parseInt(bits[1]);
		userNameId = optionalLong(bits, 2);
		tenantSessionId = optionalLong(bits, 3);
		deadline=Long.parseLong(bits[4]);

		toStringCache=bundle;
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

		this.userAuthId=userAuth.id;
		this.userEpoch = user.epoch;
		this.userNameId=(username==null?null:username.id);
		this.tenantSessionId=(tenantSession==null?null:tenantSession.id);
		this.deadline=sessionDeadline.getTime();
	}

	@Override
	public
	String toString()
	{
		if (toStringCache==null)
		{
			//WARNING: this must be parsable by the one-string constructor above.
			toStringCache=userAuthId+":"+ userEpoch +":"+(userNameId==null?"":userNameId)+":"+(tenantSessionId==null?"":tenantSessionId)+":"+deadline;
		}
		return toStringCache;
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
