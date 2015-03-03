package com.allogy.qrauth.server.helpers;

import com.allogy.qrauth.server.entities.AuthMethod;
import com.allogy.qrauth.server.entities.DBUserAuth;
import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;

import static com.allogy.qrauth.server.entities.AuthMethod.HMAC_OTP;
import static com.allogy.qrauth.server.entities.AuthMethod.TIME_OTP;

/**
 * Created by robert on 3/3/15.
 */
public
class OTPHelper
{
	public static
	final int NUM_BYTES = 10;

	public static
	boolean fitsAuthMethod(AuthMethod authMethod)
	{
		return authMethod== TIME_OTP || authMethod== HMAC_OTP;
	}

	public
	enum Type
	{
		TOTP,
		HOTP,
	}

	public
	enum Format
	{
		Six_Digits(6),
		Eight_Digits(8),
		;

		private final
		int intValue;

		Format(int intValue)
		{
			this.intValue = intValue;
		}

		public
		int getIntValue()
		{
			return intValue;
		}
	}

	public
	enum Algo
	{
		MD5,
		SHA1,
		SHA256,
		SHA512,
	}

	public Type    type;
	public Format  format;
	public Algo    algo;
	public Integer period;
	public Integer counter;

	private String base32Secret;

	private static final Logger log = LoggerFactory.getLogger(OTPHelper.class);

	public
	OTPHelper(AuthMethod authMethod)
	{
		log.debug("generating new {} helper", authMethod);

		if (authMethod == TIME_OTP)
		{
			type = Type.TOTP;
		}
		else
		{
			type = Type.TOTP;
		}

		format = Format.Eight_Digits;
		algo = Algo.SHA1;
		period = 30;
		counter = 1;
	}

	public
	void randomizeSecret()
	{
		final
		byte[] bytes = new byte[NUM_BYTES];

		ThreadLocalRandom.current().nextBytes(bytes);

		base32Secret = new Base32().encodeToString(bytes);
	}

	public
	DBUserAuth toDBUserAuth()
	{
		final
		DBUserAuth userAuth = new DBUserAuth();

		userAuth.authMethod = (type == Type.TOTP ? TIME_OTP : HMAC_OTP);
		userAuth.comment = toString();
		userAuth.secret = dbSecretString();

		return userAuth;
	}

	public
	OTPHelper(DBUserAuth userAuth)
	{
		final
		String dbSecret=userAuth.secret;

		final
		String[] bits=dbSecret.split(":");

		type=Type.valueOf(bits[0]);
		format=Format.valueOf(bits[1]);
		algo=Algo.valueOf(bits[2]);
		period=Integer.parseInt(bits[3]);
		base32Secret=bits[4];
		counter=Integer.parseInt(bits[5]);
	}

	private
	String dbSecretString()
	{
		return type.toString() + ':' + format + ':' + algo + ':' + period + ':' + base32Secret+':'+counter;
	}

	@Override
	public
	String toString()
	{
		return type.toString() + ':' + format + ':' + algo + ':' + period;
	}

	public
	String getBase32Secret()
	{
		return base32Secret;
	}

	public
	void setBase32Secret(String base32Secret)
	{
		this.base32Secret = base32Secret;
	}

	/**
	 * Based largely on:
	 * https://github.com/wstrange/GoogleAuth/blob/master/src/main/java/com/warrenstrange/googleauth/GoogleAuthenticatorQRGenerator.java#L136
	 */
	public
	String toOtpAuthURL(String issuer, String accountName)
	{
		StringBuilder sb=new StringBuilder("otpauth://").append(type.toString().toLowerCase());
		sb.append("/");

		if (issuer!=null)
		{
			assert(issuer.indexOf(':')<0);
			sb.append(issuer);
			sb.append(':');
		}

		assert(accountName!=null);

		sb.append(accountName);

		sb.append('?');
		sb.append("secret=").append(base32Secret);

		if (format!=Format.Six_Digits)
		{
			sb.append("&digits=").append(format.getIntValue());
		}

		if (algo!=Algo.SHA1)
		{
			sb.append("&algorithm=").append(algo.toString());
		}

		if (period!=null && period!=30)
		{
			sb.append("&period=").append(period);
		}

		return sb.toString();
	}

	public static
	boolean matchesUserInput(DBUserAuth userAuth, long now, String response)
	{
		final
		AuthMethod authMethod=userAuth.authMethod;

		if (authMethod==TIME_OTP)
		{
			return new OTPHelper(userAuth).totpMatches(now, response);
		}
		else
		if (authMethod==HMAC_OTP)
		{
			throw new UnsupportedOperationException();
			//TODO: increment counter for hmac
		}
		else
		{
			throw new UnsupportedOperationException(authMethod.toString());
		}
	}

	/**
	 * Based largely on:
	 * https://github.com/wstrange/GoogleAuth/blob/master/src/main/java/com/warrenstrange/googleauth/GoogleAuthenticator.java#L227
	 *
	 * @param now
	 * @param response
	 * @return
	 */
	private
	boolean totpMatches(long now, String response)
	{
		if (response.length()!=format.intValue)
		{
			log.debug("incorrect response length");
			return false;
		}

		final
		byte[] key=new Base32().decode(base32Secret);

		final
		long window=now/(period*1000);

		final
		int responseInteger=Integer.parseInt(response);

		try
		{
			if (responseInteger== calculateOtpHmacCode(key, window - 1)) return true;
			if (responseInteger== calculateOtpHmacCode(key, window)) return true;
			return (responseInteger== calculateOtpHmacCode(key, window + 1));
		}
		catch (Exception e)
		{
			log.error("unable to test TOTP match", e);
			return false;
		}
	}

	/**
	 * Based heavily on:
	 * https://github.com/pkelchner/otp/blob/master/src/main/java/net/cortexx/otp/HmacBasedOneTimePassword.java#L44
	 *
	 * @param key
	 * @param counter
	 * @return
	 */
	private
	int calculateOtpHmacCode(byte[] key, long counter) throws NoSuchAlgorithmException, InvalidKeyException
	{
		final byte[] counterBytes = ByteBuffer.allocate(8)
										.order(ByteOrder.BIG_ENDIAN)
										.putLong(counter)
										.array();

		final byte[] hash;

		final
		Mac mac=Mac.getInstance("hmac"+algo);

		mac.init(new SecretKeySpec(key, "raw"));

		//TODO: the upstream implementation had a lock here, it might be good to be triple-sure it is not needed.
		hash = mac.doFinal(counterBytes);

		final int offset = hash[19] & 0x0F;
		final int truncatedHash = ((ByteBuffer) ByteBuffer.wrap(hash)
													.order(ByteOrder.BIG_ENDIAN)
													.position(offset))
									  .getInt() & 0x7FFFFFFF;

		final
		int truncation=(int)Math.pow(10, format.intValue);

		return truncatedHash % truncation;
	}

}
