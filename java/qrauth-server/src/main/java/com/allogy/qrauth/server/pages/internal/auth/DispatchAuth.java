package com.allogy.qrauth.server.pages.internal.auth;

import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.entities.Nut;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.helpers.Exec;
import com.allogy.qrauth.server.helpers.RSAHelper;
import com.allogy.qrauth.server.pages.api.AbstractAPICall;
import org.apache.commons.codec.binary.Base64;
import org.hibernate.criterion.Restrictions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by robert on 2/20/15.
 */
public
class DispatchAuth extends AbstractAPICall
{

	Object onActivate() throws IOException
	{
		if (!isPostRequest())
		{
			return mustBePostRequest();
		}

		if (log.isDebugEnabled())
		{
			for (String key : request.getParameterNames())
			{
				String value = request.getParameter(key);
				log.debug("parameter: {} -> {}", key, value);
			}
		}

		if (request.getParameter("do_sqrl") != null)
		{
			//only relevant for noscript support (a button appears for noscript), we only need to check to see if the
			//session is connected, and issue a redirect.
			return new ErrorResponse(500, "noscript sqrl is unimplemented");
		}
		else if (request.getParameter("do_otp") != null)
		{
			return do_otp_attempt();
		}
		else if (request.getParameter("do_ppp_1")!=null)
		{
			//only relevant for noscript support (which also means provider has this as the default?)
			return new ErrorResponse(500, "noscript ppp is unimplemented");
		}
		else
		if (request.getParameter("do_ppp_2")!=null)
		{
			return do_ppp_attempt();
		}
		else
		if (request.getParameter("do_rsa")!=null)
		{
			return do_rsa_attempt();
		}
		else
		{
			return new ErrorResponse(500, "dispatacher does not implement the requested auth method");
		}
	}

	private
	Object do_otp_attempt()
	{
		return new ErrorResponse(500, "otp unimplemented");
	}

	private
	Object do_ppp_attempt()
	{
		return new ErrorResponse(500, "ppp unimplemented");
	}

	private
	Object do_rsa_attempt() throws IOException
	{
		final
		String pubKeyOrUsername=request.getParameter("rsa_pubkey");
		{
			if (pubKeyOrUsername == null)
			{
				return missingParameter("rsa_pubkey");
			}
		}

		final
		String base64Response=request.getParameter("rsa_response");
		{
			if (base64Response==null)
			{
				return missingParameter("rsa_response");
			}
		}

		final
		Nut nut=getNut();
		{
			if (nut==null || Death.hathVisited(nut))
			{
				return authFailure(Death.noteMightSay(nut, "nut is expired, consumed, or missing"));
			}
		}

		//TODO: detect if the user transmits a private key in either text box, and (if there is an *existing* account/key tuple) kill that auth method.

		if (pubKeyOrUsername.indexOf(' ')<0)
		{
			final
			String username=pubKeyOrUsername;

			//Lookup (and test against) all the user's *active* rsa keys

			//return do_rsa_any_pubkey(user, pubkeys);
			return new ErrorResponse(500, "unimplemented; rsa to existing username");
		}
		else
		{
			final
			RSAHelper rsaHelper=new RSAHelper(pubKeyOrUsername);

			try
			{
				if (rsaHelper.signatureIsValid(nut.stringValue, Base64.decodeBase64(base64Response)))
				{
					return createUserWithNewStipulation(rsaHelper.toDBUserAuth());
				}
				else
				{
					return new ErrorResponse(403, "signature problem");
				}
			}
			finally
			{
				rsaHelper.close();
			}
		}
	}

	private
	Object createUserWithNewStipulation(DBUserAuth dbUserAuth)
	{
		return new ErrorResponse(500, "unimplemented");
	}

	private
	Nut getNut()
	{
		final
		String nutStringValue=request.getParameter("nut");

		if (nutStringValue==null)
		{
			return null;
		}

		return (Nut)
		session.createCriteria(Nut.class)
			.add(Restrictions.eq("stringValue", nutStringValue))
			.uniqueResult();
	}

	private
	ErrorResponse authFailure(String message)
	{
		//TODO: this needs to account for both local & with-tenant cases.
		/*
		By the plain reading of the spec, 403 is not technically correct because it implies 'authorization will not help';
		yet 401 (unauthorized) is not correct because we must then provide basic http authentication. Hmm...
		 */
		return new ErrorResponse(403, message);
	}
}
