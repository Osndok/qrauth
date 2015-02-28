package com.allogy.qrauth.server.pages.user.credentials;

import com.allogy.qrauth.server.entities.AuthMethod;
import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.helpers.DateHelper;
import com.allogy.qrauth.server.helpers.Death;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.pages.user.AbstractUserPage;
import com.allogy.qrauth.server.services.Journal;
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
			case RSA:
				return "create-and-login/not add";

			case SQRL:
			case SALTED_PASSWORD:
				return "almost";

			case YUBIKEY_PUBLIC:
				return "Yes";

			case YUBIKEY_CUSTOM:
			case HMAC_OTP:
			case TIME_OTP:
			case PAPER_PASSWORDS:
			case OPEN_ID:
			case STATIC_OTP:
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

	@Inject
	private
	Block yubiPublic;

	public
	Block getAuthMethodBlock()
	{
		if (authMethod==null)
		{
			return selectAuthMethod;
		}

		switch (authMethod)
		{
			case YUBIKEY_PUBLIC: return yubiPublic;

			case SQRL:
			case RSA:
			case YUBIKEY_CUSTOM:
			case HMAC_OTP:
			case TIME_OTP:
			case PAPER_PASSWORDS:
			case OPEN_ID:
			case STATIC_OTP:
			case EMAILED_SECRET:
			case SALTED_PASSWORD:
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


}
