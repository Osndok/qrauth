package com.allogy.qrauth.server.pages.user.credentials;

import com.allogy.qrauth.server.entities.AuthMethod;
import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.entities.UnimplementedHashFunctionException;
import com.allogy.qrauth.server.helpers.*;
import com.allogy.qrauth.server.pages.user.AbstractUserPage;
import com.allogy.qrauth.server.pages.user.credentials.rsa.ReclaimRSA;
import com.allogy.qrauth.server.services.Hashing;
import com.allogy.qrauth.server.services.Journal;
import com.allogy.qrauth.server.services.Policy;
import com.allogy.qrauth.server.services.impl.Config;
import com.yubico.client.v2.VerificationResponse;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;
import com.yubico.client.v2.exceptions.YubicoVerificationException;
import org.apache.tapestry5.Block;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Date;

/**
 * Created by robert on 2/27/15.
 */
public
class AddCredentials extends AbstractUserPage
{
	@Property
	private
	AuthMethod authMethod;

	void onActivate(AuthMethod authMethod)
	{
		this.authMethod=authMethod;
	}

	Object onPassivate()
	{
		return authMethod;
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
			case STATIC_OTP:
			case STATIC_PASSWORD:
			case ROLLING_PASSWORD:
				return "Yes";

			case YUBIKEY_CUSTOM:
			case HMAC_OTP:
			case TIME_OTP:
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

			case SQRL:
			case YUBIKEY_CUSTOM:
			case HMAC_OTP:
			case TIME_OTP:
			case PAPER_PASSWORDS:
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

}
