package com.allogy.qrauth.server.pages.user.credentials.rsa;

import com.allogy.qrauth.server.entities.AuthMethod;
import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.entities.Nut;
import com.allogy.qrauth.server.helpers.DateHelper;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.helpers.RSAHelper;
import com.allogy.qrauth.server.pages.user.AbstractUserPage;
import com.allogy.qrauth.server.pages.user.credentials.EditCredentials;
import com.allogy.qrauth.server.pages.user.names.AbstractNamesPage;
import com.allogy.qrauth.server.services.Journal;
import com.allogy.qrauth.server.services.Network;
import com.allogy.qrauth.server.services.Nuts;
import org.apache.commons.codec.binary.Base64;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.PageActivationContext;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;

/**
 * Created by robert on 3/2/15.
 */
public
class ReclaimRSA extends AbstractUserPage
{
	@PageActivationContext
	private
	DBUserAuth userAuth;

	public
	ReclaimRSA with(DBUserAuth userAuth)
	{
		this.userAuth = userAuth;
		return this;
	}

	Object onActivate()
	{
		if (userAuth==null) return new ErrorResponse(404, "invalid (or missing) credentials id");
		if (userAuth.authMethod!= AuthMethod.RSA) return new ErrorResponse(400, "only works for RSA credentials");
		return null;
	}

	@Inject
	private
	Nuts nuts;

	@Inject
	private
	Network network;

	public
	String getNewNutString()
	{
		if (nut == null)
		{
			this.nut = nuts.allocate(null, network.needIPForThisRequest(null));
		}

		return this.nut.stringValue;
	}

	@Property
	private
	Nut nut;

	@Property
	private
	String response;

	@Inject
	private
	Request request;

	@Inject
	private
	Session session;

	Object onSuccess() throws IOException
	{
		final
		String response=request.getParameter("response");

		if (response==null || response.isEmpty())
		{
			return new ErrorResponse(400, "response field cannot be empty");
		}

		final
		byte[] binaryResponse = Base64.decodeBase64(response);

		final
		Nut nut;
		{
			final
			String nutString = request.getParameter("nut");

			nut = (Nut) session.createCriteria(Nut.class)
				.add(Restrictions.eq("stringValue", nutString))
				.uniqueResult();

			if (nut==null)
			{
				return new ErrorResponse(400, "did not find nut value");
			}
		}

		if (Death.hathVisited(nut))
		{
			return new ErrorResponse(400, Death.noteMightSay(nut, "that nut is no longer valid, try going back and refreshing the page"));
		}

		final
		RSAHelper rsaHelper=new RSAHelper(userAuth);

		try
		{
			if (rsaHelper.signatureIsValid(nut.stringValue, binaryResponse))
			{
				return reclaimPublicKeyForMyOwn(rsaHelper, nut);
			}
			else
			{
				return new ErrorResponse(400, "unable to validate signature");
			}
		}
		finally
		{
			rsaHelper.close();
		}
	}

	@Inject
	private
	Journal journal;

	@Inject
	private
	Logger log;

	@CommitAfter
	private
	Object reclaimPublicKeyForMyOwn(RSAHelper rsaHelper, Nut nut) throws IOException
	{
		log.debug("{} has proved that he holds the private key to {}, invoking transfer...", user, userAuth);

		final
		Date now = new Date();

		userAuth.deadline = now;
		userAuth.pubKey = userAuth.pubKey + " [until reclaimed by " + user + " on " + DateHelper.iso8601(now) + "]";
		session.save(userAuth);

		journal.transferredUserAuth(userAuth, user);

		//NB: while convenient, this preserves the possibly-vile 'comment' field from the foreign/remote/untrusted public key.
		final
		DBUserAuth duplicate = rsaHelper.toDBUserAuth();

		duplicate.user = user;
		session.save(duplicate);

		journal.addedUserAuthCredential(duplicate);

		nut.deadline = new Date();
		session.save(nut);

		return editCredentials.with(duplicate);
	}

	@InjectPage
	private
	EditCredentials editCredentials;

}
