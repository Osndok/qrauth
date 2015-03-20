package com.allogy.qrauth.server.pages.supervisor.tenant;

import com.allogy.qrauth.server.entities.Tenant;
import com.allogy.qrauth.server.entities.TenantSession;
import com.allogy.qrauth.server.helpers.Redux;
import com.allogy.qrauth.server.helpers.Stemmer;
import com.allogy.qrauth.server.pages.supervisor.AbstractSupervisorPage;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.hibernate.HibernateGridDataSource;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by robert on 3/20/15.
 */
public
class ApproveTenant extends AbstractSupervisorPage
{
	private static final
	Set<String> commonSelfReferences = new HashSet<String>();

	static
	{
		commonSelfReferences.add(Stemmer.appliedTo("tenant"));
		commonSelfReferences.add(Stemmer.appliedTo("account"));
	}

	public
	GridDataSource getDataSource()
	{
		//TODO: how to best set the *initial* sorting on such an otherwise-nice high level widget?
		return new HibernateGridDataSource(session, Tenant.class)
		{
			@Override
			protected
			void applyAdditionalConstraints(Criteria criteria)
			{
				criteria.add(Restrictions.eq("needsReview", true));
				//criteria.addOrder(Order.asc("updated"));
			}
		};
	}

	@Property
	private
	Tenant tenant;

	public
	String getRowClass()
	{
		//TODO: non-conflicting name changes should not require approval
		//TODO: color should indicate task (new name, changed name)
		//TODO: color should indicate probability of conflict?
		//TODO: run tenant.requestedName through redux filter, and check for match?
		return "todo";
	}

	public
	String getExampleReturnUrl()
	{
		//TODO: provide a recent url (which is wholly unreliable, and can be fabricated), so the operator can get a false impression by seeing the domain name, etc.
		return null;
	}

	@CommitAfter
	void onApproveTenant(Tenant tenant)
	{
		tenant.name=tenant.requestedName;
		tenant.nameRedux= Redux.digest(tenant.requestedName, commonSelfReferences);
		tenant.needsReview=false;
		session.save(tenant);
	}

	@CommitAfter
	void onRejectTenant(Tenant tenant)
	{
		tenant.needsReview=false;
		session.save(tenant);
	}
}
