package com.allogy.qrauth.server.pages.user.credentials;

import com.allogy.qrauth.server.entities.AuthMethod;
import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.entities.OutputStreamResponse;
import com.allogy.qrauth.server.entities.UnimplementedHashFunctionException;
import com.allogy.qrauth.server.helpers.*;
import com.allogy.qrauth.server.pages.user.AbstractUserPage;
import com.allogy.qrauth.server.pages.user.Credentials;
import com.allogy.qrauth.server.pages.user.credentials.ppp.DisplayPPP;
import com.allogy.qrauth.server.pages.user.credentials.rsa.ReclaimRSA;
import com.allogy.qrauth.server.services.Hashing;
import com.allogy.qrauth.server.services.Journal;
import com.allogy.qrauth.server.services.Policy;
import com.allogy.qrauth.server.services.impl.Config;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.yubico.client.v2.VerificationResponse;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;
import com.yubico.client.v2.exceptions.YubicoVerificationException;
import org.apache.tapestry5.*;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

/**
 * Created by robert on 2/27/15.
 */
public
class AddCredentials extends AbstractUserPage
{
	@Property
	private
	AuthMethod authMethod;

	Object onActivate(AuthMethod authMethod)
	{
		this.authMethod=authMethod;

		if (OTPHelper.fitsAuthMethod(authMethod) && otpHelper==null)
		{
			//TODO: count the user's HOTP methods, and forbid them from adding too many
			otpHelper=new OTPHelper(authMethod);
		}
		else
		if (authMethod==AuthMethod.PAPER_PASSWORDS)
		{
			maybeLoadExistingPPP();
		}

		return null;
	}

	Object onPassivate()
	{
		if (userAuthReveal==null)
		{
			return authMethod;
		}
		else
		{
			return new Object[]{
				authMethod.toString(),
				userAuthReveal.id.toString()
			};
		}
	}

	public
	AuthMethod[] getAuthMethods()
	{
		return AuthMethod.values();
	}

	public
	String getAuthMethodName()
	{
		return InternalUtils.toUserPresentable(authMethod.toString());
	}

	public
	String getActuallyWorks()
	{
		switch (authMethod)
		{
			case SQRL:
				return "almost";

			case RSA:
			case YUBIKEY_PUBLIC:
			case HMAC_OTP:
			case TIME_OTP:
			case STATIC_OTP:
			case STATIC_PASSWORD:
			case ROLLING_PASSWORD:
				return "Yes";

			case YUBIKEY_CUSTOM:
			case PAPER_PASSWORDS:
			case OPEN_ID:
			case EMAILED_SECRET:
			default:
				return "";
		}
	}

	/*
	------------------- Dispatching ---------------------------
	 */
	@Inject
	private
	Block selectAuthMethod;

	@Inject
	private
	Block unimplemented;

	public
	Block getAuthMethodBlock()
	{
		if (authMethod==null)
		{
			return selectAuthMethod;
		}

		switch (authMethod)
		{
			case YUBIKEY_PUBLIC  : return yubiPublic;
			case STATIC_PASSWORD : return staticPassword;
			case ROLLING_PASSWORD: return rollingPassword;
			case STATIC_OTP      : return staticOtp;
			case RSA             : return rsaBlock;
			case TIME_OTP        : return otpBlock;
			case HMAC_OTP        : return otpBlock;
			case PAPER_PASSWORDS : return pppBlock;

			case SQRL:
			case YUBIKEY_CUSTOM:
			case OPEN_ID:
			case EMAILED_SECRET:
			default:
				return unimplemented;
		}
	}

	@Inject
	private
	Logger log;

	@InjectPage
	private
	EditCredentials editCredentials;

	@Inject
	private
	Journal journal;

	/*
	----------- YUBI_PUBLIC -------------
	 */

	@Inject
	private
	Block yubiPublic;

	@Property
	private
	String yubiInput;

	Object onSelectedFromDoYubiPublic()
	{
		final
		YubicoClient yubicoClient = Config.get().getYubicoClient();

		if (yubicoClient == null)
		{
			return new ErrorResponse(500, "unable to test yubikeys... missing yubico api key");
		}

		final
		String yubiInput = this.yubiInput;

		log.debug("test yubikey: {}", yubiInput);

		final
		VerificationResponse verificationResponse;

		try
		{
			verificationResponse=yubicoClient.verify(yubiInput);
		}
		catch (YubicoVerificationException e)
		{
			log.warn("yubikey exception", e);
			return new ErrorResponse(500, "unable to test yubikey... please try again... "+e);
		}
		catch (YubicoValidationFailure e)
		{
			log.warn("yubikey failure", e);
			return new ErrorResponse(500, "unable to verify yubikey... please try again... "+e);
		}

		if (verificationResponse.isOk())
		{
			final
			String publicId=YubicoClient.getPublicId(yubiInput);

			return addOrReclaimPublicYubikey(publicId);
		}
		else
		{
			return new ErrorResponse(400, "invalid yubikey otp");
		}
	}

	/**
	 * By allowing other users to transparently register and use a yubikey that they found, it actually increases
	 * the overall security of the system (automatically revoking yubikey use for the other account), and at worst
	 * will lock a user out of his/her account (if they are trying to do some kind of juggling act).
	 *
	 * @param publicId - the constant prefix that is emitted by the yubikey before the actual OTP
	 * @return
	 */
	@CommitAfter
	private
	Object addOrReclaimPublicYubikey(String publicId)
	{
		final
		String pubKey="yubico:"+publicId;

		DBUserAuth userAuth=byPubKey(pubKey);

		if (userAuth!=null)
		{
			if (user.id.equals(userAuth.user.id))
			{
				//Same user... it's already added... a double click? thought I lost it, but now found it?

				if (Death.hathVisited(userAuth))
				{
					log.info("trying to add a previously-disabled yubikey with the same user.id revives it: {}", pubKey);
					userAuth.deadline=null;
					session.save(userAuth);

					journal.updatedUserAuth(userAuth);
				}
				else
				{
					log.info("user tries to add a yubikey that is already attached to account: {}", pubKey);
				}

				return editCredentials.with(userAuth);
			}
			else
			{
				log.warn("{} is trying to add '{}' to account, which was being used by {} as {}", user, pubKey, userAuth.user, userAuth);

				//pubkey is a unique index/lookup field, so we must slide the old key out of the way... so an easy way to do this is pollute the pubKey with diagnostic info...
				userAuth.pubKey=pubKey+" (until "+user+" proved possession on "+ DateHelper.iso8601()+")";

				if (!Death.hathVisited(userAuth))
				{
					userAuth.deadline = new Date();
					userAuth.deathMessage = user+" proved possession of this Yubikey, therefore invalidating it for authentication.";
				}

				session.save(userAuth);

				journal.transferredUserAuth(userAuth, user);
			}
		}

		userAuth=new DBUserAuth();
		userAuth.user=user;
		userAuth.authMethod=AuthMethod.YUBIKEY_PUBLIC;
		userAuth.millisGranted=(int)userAuth.authMethod.getDefaultLoginLength();
		userAuth.pubKey=pubKey;
		session.save(userAuth);

		journal.addedUserAuthCredential(userAuth);

		return editCredentials.with(userAuth);
	}

	private
	DBUserAuth byPubKey(String pubKey)
	{
		return (DBUserAuth)session.createCriteria(DBUserAuth.class)
			.add(Restrictions.eq("pubKey", pubKey))
			.uniqueResult();
	}

	/*
	--------------------------- ROLLING_PASSWORD --------------------------------
	 */

	@Inject
	private
	Block rollingPassword;

	@Property
	private
	String password;

	@Inject
	private
	Hashing hashing;

	@Inject
	private
	Policy policy;

	Object onSelectedFromDoRollingPassword() throws UnimplementedHashFunctionException
	{
		if (alreadyHavePasswordOnFile())
		{
			return new ErrorResponse(400, "that password has already been used, and cannot therefore be reused");
		}

		if (password == null || password.isEmpty())
		{
			return new ErrorResponse(400, "the password cannot be empty");
		}

		//NB: password is escaping!
		final
		int strength = PasswordHelper.gaugeStrength(password);

		final
		DBUserAuth userAuth = new DBUserAuth();

		userAuth.user = user;
		userAuth.authMethod = AuthMethod.ROLLING_PASSWORD;
		userAuth.millisGranted = (int) userAuth.authMethod.getDefaultLoginLength();
		userAuth.comment = "Strength=" + strength;
		userAuth.secret = hashing.digest(password);
		userAuth.deadline = policy.passwordDeadlineGivenComplexity(strength);
		userAuth.deathMessage = "That password has expired";
		session.save(userAuth);

		journal.addedUserAuthCredential(userAuth);

		return editCredentials.with(userAuth);
	}

	private
	boolean alreadyHavePasswordOnFile() throws UnimplementedHashFunctionException
	{
		final
		String password = this.password;

		for (DBUserAuth userAuth : user.authMethods)
		{
			if (isBasicPasswordMechanism(userAuth.authMethod))
			{
				if (hashing.digestMatch(password, userAuth.secret))
				{
					return true;
				}
			}
		}

		return false;
	}

	private
	boolean isBasicPasswordMechanism(AuthMethod authMethod)
	{
		return (authMethod == AuthMethod.STATIC_OTP || authMethod == AuthMethod.ROLLING_PASSWORD);
	}

	/*
	------------------------------ STATIC_OTP -------------------------------
	staticOtp
	 */

	@Inject
	private
	Block staticOtp;

	@Property
	private
	String comment;

	Object onSelectedFromDoStaticOtp() throws UnimplementedHashFunctionException
	{
		if (alreadyHavePasswordOnFile())
		{
			return new ErrorResponse(400, "that password has already been used, and cannot therefore be reused");
		}

		if (password == null || password.isEmpty())
		{
			return new ErrorResponse(400, "the password cannot be empty");
		}

		if (comment == null || comment.isEmpty())
		{
			//NB: password is escaping!
			final
			int strength = PasswordHelper.gaugeStrength(password);

			comment = "Strength=" + strength + ", and is only valid once";
		}

		final
		DBUserAuth userAuth = new DBUserAuth();

		userAuth.user = user;
		userAuth.authMethod = AuthMethod.STATIC_OTP;
		userAuth.millisGranted = (int) userAuth.authMethod.getDefaultLoginLength();
		userAuth.comment = comment;
		userAuth.secret = hashing.digest(password);
		userAuth.deadline = null;
		userAuth.deathMessage = null;
		session.save(userAuth);

		journal.addedUserAuthCredential(userAuth);

		return editCredentials.with(userAuth);
	}

	/*
	--------------------------- STATIC_PASSWORD --------------------------------
	 */

	@Inject
	private
	Block staticPassword;

	@Property
	private
	Date deadline;

	Object onSelectedFromDoStaticPassword() throws UnimplementedHashFunctionException
	{
		//TODO: instead of blocking them outright... if it only matches an old *dead* password, let them re-affirm (with a little grief) that it is truly their wish...
		//      This is important in case they really, truly, want to use a static password that they once set as a temporary password, etc.
		if (alreadyHavePasswordOnFile())
		{
			return new ErrorResponse(400, "that password has already been used, and cannot therefore be reused");
		}

		if (deadline==null)
		{
			return new ErrorResponse(400, "you must select a deadline for this password");
		}

		if (password == null || password.isEmpty())
		{
			return new ErrorResponse(400, "the password cannot be empty");
		}

		if (comment == null || comment.isEmpty())
		{
			//NB: password is escaping!
			final
			int strength = PasswordHelper.gaugeStrength(password);

			comment = "Strength=" + strength;
		}

		final
		DBUserAuth userAuth = new DBUserAuth();

		userAuth.user = user;
		userAuth.authMethod = AuthMethod.STATIC_PASSWORD;
		userAuth.millisGranted = (int) userAuth.authMethod.getDefaultLoginLength();
		userAuth.comment = comment;
		userAuth.secret = hashing.digest(password);
		userAuth.deadline = deadline;
		userAuth.deathMessage = "That password has reached it's end-of-life.";
		session.save(userAuth);

		journal.addedUserAuthCredential(userAuth);

		return editCredentials.with(userAuth);
	}

	/*
	--------------------------- RSA --------------------------------
	 */

	@Inject
	private
	Block rsaBlock;

	@Property
	private
	String pubKey;

	@InjectPage
	private
	ReclaimRSA reclaimRSA;

	@CommitAfter
	Object onSelectedFromDoRSA() throws IOException
	{
		if (pubKey==null || pubKey.isEmpty())
		{
			return new ErrorResponse(400, "public key field cannot be empty");
		}

		final
		RSAHelper rsaHelper=new RSAHelper(pubKey);

		try
		{
			DBUserAuth userAuth=byPubKey(rsaHelper.getSshKeyBlob());

			if (userAuth==null)
			{
				userAuth=rsaHelper.toDBUserAuth();
				userAuth.user=user;
				session.save(userAuth);

				journal.addedUserAuthCredential(userAuth);
				return editCredentials.with(userAuth);
			}
			else
			if (userAuth.user.id.equals(user.id))
			{
				//TODO: could they be trying to re-activate this keypair? should we check to see if it is dead?
				//We already have this public key on file... for this same account.
				return editCredentials.with(userAuth);
			}
			else
			{
				//They entered a key that we already have on file, go to the reclaim-key procedure...
				return reclaimRSA.with(userAuth);
			}
		}
		finally
		{
			rsaHelper.close();
		}
	}

	/*
	----------------------------- When SECRETS are to be EXPOSED !!! ---------------------------
	 */

	@Property
	private
	DBUserAuth userAuthReveal;

	Object onActivate(AuthMethod authMethod, long revealId)
	{
		if (userAuthReveal==null)
		{
			return psuedoActivate(authMethod, revealId);
		}

		return null;
	}

	Object psuedoActivate(AuthMethod authMethod, long revealId)
	{
		final
		DBUserAuth requested=(DBUserAuth)session.get(DBUserAuth.class, revealId);

		if (requested==null)
		{
			return new ErrorResponse(404, "auth method not found");
		}

		//Make sure the credential is *ours* and *recent*
		if (!requested.user.id.equals(user.id))
		{
			return new ErrorResponse(400, "user/credential mismatch");
		}

		if (requested.created.getTime() < oldestRevealableTime())
		{
			return new ErrorResponse(400, "sorry, only recently-created credentials can be revealed");
		}

		//Alright... let 'em have it!
		this.userAuthReveal = requested;

		if (OTPHelper.fitsAuthMethod(requested.authMethod))
		{
			otpHelper=new OTPHelper(requested);
		}

		return null;
	}

	private
	long oldestRevealableTime()
	{
		return System.currentTimeMillis() - policy.longestReasonableAddCredentialTaskLength();
	}



	/*
	-------------------------------------- TIME_OTP -----------------------------------------
	 */

	@Inject
	private
	Block otpBlock;

	@Inject
	@Path("context:images/qr-code-ready.png")
	private
	Asset qrCodeReady;

	@Property
	private
	OTPHelper otpHelper;

	public
	String getOtpQrUrl()
	{
		if (userAuthReveal == null)
		{
			return qrCodeReady.toClientURL();
		}
		else
		{
			return userAuthReveal.id+"/qr.png";
		}
	}

	Object onActivate(AuthMethod authMethod, long revealId, String qrCodeFilename) throws WriterException
	{
		if (!qrCodeFilename.equals("qr.png"))
		{
			return new ErrorResponse(404, "unknown secret attachment");
		}

		//Work around usually-works-fine tapestry activation method ordering...
		{
			final
			Object retval=psuedoActivate(authMethod, revealId);

			if (retval!=null)
			{
				return retval;
			}
		}

		/*
		GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator(Config.get().getTOTPConfig());
		GoogleAuthenticatorKey credentials = googleAuthenticator.createCredentials();
		String key = credentials.getKey();
		log.debug("google authenticator key: {}", key);

		//BUG: need to add 'digits=8' to url
		String url = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL("Allogy", "Username", credentials);
		log.debug("got uri: {}", url);
		*/

		final
		String url = otpHelper.toOtpAuthURL("Allogy", userAuthReveal.toString());

		final
		String finalImageFormat = "png";

		final
		QRCodeWriter qrCodeWriter = new QRCodeWriter();

		BarcodeFormat barcodeFormat = BarcodeFormat.QR_CODE;
		int width = 177;
		int height = 177;

		Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		hints.put(EncodeHintType.MARGIN, 0); /* default = 4, but we do padding with html (no need to include in bitmap) */

		final BitMatrix bitMatrix = qrCodeWriter.encode(url, barcodeFormat, width, height, hints);

		return new OutputStreamResponse()
		{
			public
			String getContentType()
			{
				return "image/" + finalImageFormat;
			}

			public
			void writeToStream(OutputStream outputStream) throws IOException
			{
				MatrixToImageWriter.writeToStream(bitMatrix, finalImageFormat, outputStream);
			}

			public
			void prepareResponse(Response response)
			{
				//no-op...
			}
		};
	}

	public
	String getOtpQrUrlTitle()
	{
		if (userAuthReveal == null)
		{
			return "This is where the QR code will appear, if requested.";
		}
		else
		{
			return "You should scan this QR code immediately.";
		}
	}

	public
	String getOtpSecretCode()
	{
		if (userAuthReveal == null)
		{
			//e.g. "J3HQJ4NT57KRMATN"
			//e.g. "YGCHDZHXYLF2BGG7"
			//turn "----------------";
			//turn "-R-E-A-D-Y-.-.--";
			return "----READY...----";
		}
		else
		{
			return otpHelper.getBase32Secret();
		}
	}

	Object onSelectedFromOtpDone()
	{
		if (userAuthReveal==null)
		{
			return Credentials.class;
		}
		else
		{
			return editCredentials.with(userAuthReveal);
		}
	}

	@CommitAfter
	Object onSelectedFromOtpReveal()
	{
		otpHelper.randomizeSecret();

		userAuthReveal=otpHelper.toDBUserAuth();
		userAuthReveal.user=user;
		userAuthReveal.millisGranted=(int)userAuthReveal.authMethod.getDefaultLoginLength();
		session.save(userAuthReveal);

		journal.addedUserAuthCredential(userAuthReveal);

		return this;
	}

	@Inject
	private
	Block totpBashScript;

	@Inject
	private
	Block hotpBashScript;

	@Inject
	private
	ComponentResources componentResources;

	@CommitAfter
	Object onSelectedFromOtpBash()
	{
		otpHelper.randomizeSecret();

		userAuthReveal=otpHelper.toDBUserAuth();
		userAuthReveal.user=user;
		userAuthReveal.millisGranted=(int)userAuthReveal.authMethod.getDefaultLoginLength();
		userAuthReveal.comment="BASH-Script Token: "+userAuthReveal.comment;
		session.save(userAuthReveal);

		journal.addedUserAuthCredential(userAuthReveal);

		final
		Block block=(userAuthReveal.authMethod==AuthMethod.TIME_OTP?totpBashScript:hotpBashScript);

		return new StreamResponse()
		{
			@Override
			public
			String getContentType()
			{
				return "text/plain";
			}

			@Override
			public
			InputStream getStream() throws IOException
			{
				return BlockHelper.toInputStream(componentResources, block);
			}

			@Override
			public
			void prepareResponse(Response response)
			{
				response.setHeader("Content-Disposition", "attachment; filename=otp-token-"+userAuthReveal.id+".sh");
			}
		};
	}

	/*
	Object onSelectedFromOtpReconfigure()
	{
		//TODO: all we have to do is flash-persist (or store in user policy) the otp helper
		return this;
	}
	*/

	@CommitAfter
	Object onSelectedFromOtpManual()
	{
		//TODO: !!!: we really need to verify that the provided seed is Base32
		otpHelper.setBase32Secret(password);

		userAuthReveal=otpHelper.toDBUserAuth();
		userAuthReveal.user=user;
		userAuthReveal.millisGranted=(int)userAuthReveal.authMethod.getDefaultLoginLength();
		userAuthReveal.comment="Manually-Provisioned Token: "+userAuthReveal.comment;
		session.save(userAuthReveal);

		journal.addedUserAuthCredential(userAuthReveal);

		return editCredentials.with(userAuthReveal);
	}

	/**
	 * That's when one knows you are stretching your template engine a bit beyond it's design goals.
	 */
	public
	String getLT()
	{
		return "<";
	}

	/**
	 * That's when one knows you are stretching your template engine a bit beyond it's design goals.
	 */
	public
	String getGT()
	{
		return ">";
	}



	/*
	-------------------------------------- PAPER_PASSWORDS -----------------------------------------
	 */

	@Inject
	private
	Block pppBlock;

	@Property
	private
	DBUserAuth existingPPP;

	void maybeLoadExistingPPP()
	{
		existingPPP = (DBUserAuth)
						  session.createCriteria(DBUserAuth.class)
							  .add(Restrictions.eq("user", user))
							  .add(Restrictions.eq("authMethod", AuthMethod.PAPER_PASSWORDS))
							  .uniqueResult()
		;
	}

	@Property
	private
	String pppComment;

	public
	String getNewOrExistingPPP()
	{
		if (existingPPP==null)
		{
			return "a printable sheet of passwords that you can use to access your account.";
		}
		else
		{
			return "new password sheets to replace the ones previously attached to your account.";
		}
	}

	@CommitAfter
	public
	Object onSelectedFromRenewPPP()
	{
		if (existingPPP==null)
		{
			if (comment==null || comment.isEmpty())
			{
				comment=Config.get().getBrandName();
			}

			existingPPP=new PPP_Helper(comment).toDBUserAuth(user);
			session.save(existingPPP);
			journal.addedUserAuthCredential(existingPPP);
		}
		else
		{
			new PPP_Helper(existingPPP).advanceVolley();

			if (Death.hathVisited(existingPPP))
			{
				existingPPP.deadline=null;
				existingPPP.deathMessage=null;
				journal.addedUserAuthCredential(existingPPP);
			}

			if (comment!=null && !comment.isEmpty())
			{
				existingPPP.comment=comment;
			}

			session.save(existingPPP);
		}

		return displayPPP.with(existingPPP);
	}

	@InjectPage
	private
	DisplayPPP displayPPP;

	@CommitAfter
	public
	Object onSelectedFromRevokePPP()
	{
		if (comment==null || comment.isEmpty())
		{
			existingPPP.deathMessage = "User opted to revoke paper passwords.";
			existingPPP.deadline=new Date();
			session.save(existingPPP);
		}
		else
		{
			existingPPP.deathMessage = comment;
			existingPPP.deadline=new Date();
			session.save(existingPPP);
		}

		return new TextStreamResponse("text/plain", "All previous paper passwords have now been disabled, you can now close this tab/window.");
	}

}
