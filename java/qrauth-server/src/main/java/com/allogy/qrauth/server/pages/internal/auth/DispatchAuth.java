package com.allogy.qrauth.server.pages.internal.auth;

import com.allogy.qrauth.server.entities.Nut;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.helpers.Exec;
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
	private static final
	File SSH_KEYGEN = new File(System.getProperty("SSH_KEYGEN", "/usr/bin/ssh-keygen"));

	private static final
	File PUBKEY2SSH = new File(System.getProperty("PUBKEY2SSH", "/usr/bin/qrauth-pubkey2ssh"));

	/**
	 * @url http://rt.openssl.org/Ticket/Display.html?user=guest&pass=guest&id=2618
	 */
	private static final
	boolean OPENSSL_BUG_2618_IS_STILL_WIDESPREAD = true;

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
			String pubKey=pubKeyOrUsername;

			final
			String sshRsaFormat;

			final
			File pemFile;
			{
				if (pubKey.contains("ssh-rsa"))
				{
					//They provided the public key in SSH-RSA format
					sshRsaFormat=pubKey;

					if (!SSH_KEYGEN.canExecute())
					{
						log.error("cannot execute: {}", SSH_KEYGEN);
						return new ErrorResponse(500, "sorry, unable to support ssh-rsa keys at this time");
					}

					final
					File tempSshRsaFile=File.createTempFile("qrauth-rsa-ssh-",".pub");

					try
					{
						writeBytesToFile(sshRsaFormat.getBytes(), tempSshRsaFile);

						final
						String pemData = Exec.toString(SSH_KEYGEN.toString(), "-f", tempSshRsaFile.getAbsolutePath(), "-e", "-m", "PKCS8");

						log.debug("pemData:\n{}", pemData);

						pemFile=File.createTempFile("qrauth-rsa-ssh-",".pem");
						writeBytesToFile(pemData.getBytes(), pemFile);
					}
					finally
					{
						tempSshRsaFile.delete();
					}
				}
				else
				if (pubKey.contains("BEGIN SSH2 PUBLIC KEY"))
				{
					//TODO: support this format too...
					//---> ssh-keygen -i -f ${KEY}
					return new ErrorResponse(500, "that pubkey format is not yet supported :-(");
				}
				else
				if (pubKey.contains("BEGIN RSA PUBLIC KEY"))
				{
					//TODO: support this format too...
					//---> ssh-keygen -i -m PEM -f ${RSA}
					return new ErrorResponse(500, "that pubkey format is not yet supported :-(");
				}
				else
				if (pubKey.contains("BEGIN PUBLIC KEY"))
				{
					//They provided the public key in PEM format

					if (!PUBKEY2SSH.canExecute())
					{
						log.error("cannot execute: {}", PUBKEY2SSH);
						return new ErrorResponse(500, "sorry, unable to support PEM public key format at this time");
					}

					pemFile=File.createTempFile("qrauth-rsa-",".pem");
					writeBytesToFile(pubKey.getBytes(), pemFile);

					sshRsaFormat = Exec.toString(PUBKEY2SSH.toString(), pemFile.getAbsolutePath(), "pem-no-comment");
				}
				else
				{
					return invalidParameter("rsa_pubkey");
				}
			}

			try
			{
				log.info("got: {}", sshRsaFormat);

				final
				File signatureFile=File.createTempFile("qrauth-rsa-",".sig");

				try
				{
					writeBytesToFile(Base64.decodeBase64(base64Response), signatureFile);

					//actually do the public key verification...

					//Uggh... it is very inconvenient to use pkeyutl due to a widespread exit-status bug...
					if (OPENSSL_BUG_2618_IS_STILL_WIDESPREAD)
					{
						final
						String signedNut=Exec.toString("/usr/bin/openssl", "rsautl", "-verify", "-in",
														  signatureFile.getAbsolutePath(), "-pubin", "-inkey",
														  pemFile.getAbsolutePath());

						if (!signedNut.equals(nut.stringValue))
						{
							log.error("signature mismatch: '{}' != '{}'", signedNut, nut.stringValue);
							return invalidParameter("rsa_response");
						}
					}
					else
					{
						//echo -n ${NUT} | openssl pkeyutl -verify -sigfile ${SIGNED} -pubin -inkey ${PEM}

						Exec.withInput(nut.stringValue.getBytes(),
										  "/usr/bin/openssl", "pkeyutl", "-verify", "-sigfile",
										  signatureFile.getAbsolutePath(), "-pubin", "-inkey",
										  pemFile.getAbsolutePath());
					}
				}
				finally
				{
					signatureFile.delete();
				}
			}
			finally
			{
				pemFile.delete();
			}

			//TODO: create user & method (with new public key)
			//return authSuccess();
			return new ErrorResponse(500, "trying...");
		}
	}

	private
	void writeBytesToFile(byte[] bytes, File file) throws IOException
	{
		final
		OutputStream out=new FileOutputStream(file);

		try
		{
			out.write(bytes);
		}
		finally
		{
			out.close();
		}
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
