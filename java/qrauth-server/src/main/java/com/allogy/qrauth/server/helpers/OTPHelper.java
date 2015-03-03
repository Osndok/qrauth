package com.allogy.qrauth.server.helpers;

import com.allogy.qrauth.server.entities.AuthMethod;
import com.allogy.qrauth.server.entities.DBUserAuth;
import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	}

	private
	String dbSecretString()
	{
		return type.toString() + ':' + format + ':' + algo + ':' + period + ':' + base32Secret;
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
}
