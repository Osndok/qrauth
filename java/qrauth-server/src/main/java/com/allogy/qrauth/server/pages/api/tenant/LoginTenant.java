package com.allogy.qrauth.server.pages.api.tenant;

import com.allogy.qrauth.server.entities.*;
import com.allogy.qrauth.server.helpers.BlockHelper;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.services.AuthSession;
import com.allogy.qrauth.server.services.Journal;
import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.ContentType;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;
import org.hibernate.criterion.Restrictions;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * The primary goal of this api call is to *LOCATE* or *CREATE* a tenant session.
 * Created by robert on 3/16/15.
 */
public
class LoginTenant extends StandardTenantAPICall
{
	//REQUIRED
	private static final String PARAMETER_SESSION_ID = "session_id";
	private static final String PARAMETER_USER_IP    = "user_ip";

	//OPTIONAL
	private static final String PARAMETER_CSS_REPLACE  = "css_replace";
	private static final String PARAMETER_EXTRA_PANELS = "extra_panels";

	@Property
	private
	TenantSession tenantSession;

	@Property
	private
	String cssReplacement;

	private
	TenantIP tenantIP;

	@Inject
	private
	Block loginForm;

	@Inject
	private
	Block redirector;

	private
	Object onActivate()
	{
		log.debug("tenant requests login info (or form)");

		if (!isPostRequest())
		{
			return mustBePostRequest();
		}

		final
		String preHashedSessionId = request.getParameter(PARAMETER_SESSION_ID);
		{
			if (empty(preHashedSessionId))
			{
				return missingOrInvalidParameter(PARAMETER_SESSION_ID);
			}
		}

		final
		String userIp = request.getParameter(PARAMETER_USER_IP);
		{
			if (empty(userIp))
			{
				return missingOrInvalidParameter(PARAMETER_USER_IP);
			}
		}

		tenantIP = network.needNonRequestTenantIp(tenant, userIp);

		if (Death.hathVisited(tenantIP))
		{
			return forbiddenCode(403, 1, Death.noteMightSay(tenantIP, tenantIP + " is banned"));
		}

		tenantSession = (TenantSession) session.createCriteria(TenantSession.class)
											.add(Restrictions.eq("tenant", tenant))
											.add(Restrictions.eq("session_id", preHashedSessionId))
											.uniqueResult()
		;

		if (tenantSession == null)
		{
			log.debug("creating tenantSession for session_id: {}", preHashedSessionId);
			tenantSession = createNewTenantSession(preHashedSessionId);
		}
		else if (tenantSession.userAuth == null)
		{
			log.debug("found existing (but unauthenticated) tenantSession");
		}
		else
		{
			log.debug("found existing (and *AUTHENTICATED*) tenantSession");
			return jsonAuthenticationBlob(tenantSession);
		}

		//Render the login form...
		response.setStatus(202);
		return BlockHelper.toResponse("text/html", componentResources, loginForm);
	}

	@Inject
	private
	ComponentResources componentResources;

	@Inject
	private
	Response response;

	private
	Object forbiddenCode(int httpStatus, int errorCode, String humanReadableMessage)
	{
		response.setStatus(httpStatus);

		JSONObject o=new JSONObject()
			.put("error", true)
			.put("code", errorCode)
			.put("message", humanReadableMessage)
			;

		return new TextStreamResponse("application/json", o.toCompactString());
	}

	@CommitAfter
	private
	TenantSession createNewTenantSession(String sessionId)
	{
		final
		TenantSession tenantSession = new TenantSession();

		tenantSession.tenant = tenant;
		tenantSession.tenantIP = tenantIP;
		tenantSession.session_id = sessionId;

		session.save(tenantSession);
		return tenantSession;
	}

	@Inject
	private
	Journal journal;

	@CommitAfter
	private
	Object jsonAuthenticationBlob(TenantSession tenantSession)
	{
		if (Death.hathVisited(tenantSession))
		{
			return forbiddenCode(409, 2, Death.noteMightSay(tenantSession, "session has expired"));
		}

		final
		Tenant tenant = this.tenant;

		final
		TenantIP tenantIP = tenantSession.tenantIP;

		final
		DBUserAuth userAuth = tenantSession.userAuth;

		//NB: userAuth's death-state is checked *BEFORE* assigning it to the session.

		TenantUser tenantUser = locateExisting(tenant, userAuth.user);
		{
			if (tenantUser == null)
			{
				if (tenant.newUsers)
				{
					tenantUser = new TenantUser();
					tenantUser.user = userAuth.user;
					tenantUser.username = tenantSession.username;
					tenantUser.tenant = tenant;
					tenantUser.configJson = "{}";
					session.save(tenantUser);

					journal.newTenantUser(tenantUser, tenantSession, tenantIP);
				}
				else
				{
					//journal.rejectedNewUser(tenant, tenantSession, userAuth, tenantIP);
					return forbiddenCode(403, 3, tenant+" does not allow new users (closed site)");
				}
			}
		}

		final
		Username username=selectUsername(tenant, tenantSession, tenantUser);

		final
		JSONObject retval=new JSONObject()
			.put("uid", tenantUser.id)
			.put("alarm", selectAlarm(userAuth, tenantSession.username))
			.put("security_rank", userAuth.authMethod.getRank())
			.put("seconds", secondsUntil(tenantSession.deadline))
			.put("deadline", tenantSession.deadline.getTime())
			;

		if (username==null)
		{
			if (tenant.requireUsername)
			{
				log.debug("username required, and not available");
				//TODO: return a redirection widget instead (remove a few levels of indirection? but assumes login cookie?)
				return forbiddenCode(409, 4, "user must select a username, but returned to tenant first");
			}

			retval
				.put("unixName", null)
				.put("displayName", null)
				;
		}
		else
		{
			retval
				.put("unixName", username.unixValue)
				.put("displayName", username.displayValue)
				;
		}

		//TODO: add groups & permissions (if requested?)
		response.setStatus(200);
		return new TextStreamResponse("application/json", retval.toCompactString());
	}

	private
	int secondsUntil(Date date)
	{
		long milliseconds=date.getTime()-System.currentTimeMillis();
		return (int)(milliseconds/1000);
	}

	private
	Username selectUsername(Tenant tenant, TenantSession tenantSession, TenantUser tenantUser)
	{
		if (tenant.fixedUsername)
		{
			//Favor the first username ever used with this tenant.

			if (tenantUser.username==null && tenantSession.username!=null)
			{
				tenantUser.username=tenantSession.username;
				session.save(tenantUser);
			}

			return tenantUser.username;
		}
		else
		{
			//Favor the *current* username (if specified)

			if (tenantSession.username==null)
			{
				return tenantUser.username;
			}
			else
			{
				return tenantSession.username;
			}
		}
	}

	private
	boolean selectAlarm(DBUserAuth userAuth, Username username)
	{
		if (username==null)
		{
			return userAuth.silentAlarm;
		}
		else
		{
			return userAuth.silentAlarm || username.silentAlarm;
		}
	}

	private
	TenantUser locateExisting(Tenant tenant, DBUser user)
	{
		final
		List<TenantUser> list =
			session.createCriteria(TenantUser.class)
				.add(Restrictions.eq("tenant", tenant))
				.add(Restrictions.eq("user", user))
				.list();

		final
		Iterator<TenantUser> i = list.iterator();

		if (i.hasNext())
		{
			final
			TenantUser retval = i.next();

			if (i.hasNext())
			{
				log.error("duplicate TenantUser({}, {}): {} & {}", tenant, user, retval, i.next());
			}

			return retval;
		}
		else
		{
			return null;
		}
	}

}
