package com.allogy.qrauth.server.pages.user;

import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.entities.Username;
import com.allogy.qrauth.server.helpers.Death;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.hibernate.HibernateGridDataSource;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * Created by robert on 2/26/15.
 */
public
class NamesUser extends AbstractUserPage
{
	public
	GridDataSource getDataSource()
	{
		return new HibernateGridDataSource(session, Username.class)
		{
			@Override
			protected
			void applyAdditionalConstraints(Criteria criteria)
			{
				criteria.add(Restrictions.eq("user", user));
			}
		};
	}

	@Property
	private
	Username username;

	public
	String getDeadOrAlive()
	{
		return (Death.hathVisited(username)?"dead":"alive");
	}

}