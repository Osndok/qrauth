package com.allogy.qrauth.server.crypto;

import junit.framework.TestCase;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * Created by robert on 3/4/15.
 */
@Test
public
class Ed25519Test extends TestCase
{
	private static final
	int ITERATIONS = 10;


	/*
	Test data from:
	http://ed25519.herokuapp.com/
	 */
	final String seed       = "seed";
	final String privateKey = "GbJYVuHBUMqDTP/ItZsjrb0OwDieWOsis7ZHaAmNACvxZeHl98KQ5S8u3vP7q2DLrnS/0ydPjl7h3jNFyVShZg==";
	final String publicKey  = "8WXh5ffCkOUvLt7z+6tgy650v9MnT45e4d4zRclUoWY=";
	final String message    = "clientserver";
	final String signature  = "uO6C9X0prThkxP9xJw9ZN+j6V/uhyVEKHeUYaQaXTtC2mHvruuHnHuIgHLyAOR25e5JLcNCdwkNU000NUDxyCQ==";


	@Test
	public
	void testSignatureVerification1() throws Exception
	{
		System.err.println("\n\n**\n** CRYPTO TEST\n**\n");

		byte[] publicKey = Base64.decode(this.publicKey);
		byte[] message = this.message.getBytes("UTF-8");
		byte[] signatureValue = Base64.decode(this.signature);

		//Warm up the crypto code a bit..
		timedCheck(signatureValue, message, publicKey);
		timedCheck(signatureValue, message, publicKey);
		timedCheck(signatureValue, message, publicKey);
		timedCheck(signatureValue, message, publicKey);
		timedCheck(signatureValue, message, publicKey);

		long startTime=System.currentTimeMillis();

		for (int i=ITERATIONS; i>0; i--)
		{
			Ed25519.checkvalid(signatureValue, message, publicKey);
		}

		long duration=System.currentTimeMillis()-startTime;
		long perVerify=duration/ITERATIONS;
		double rps=1000.0*ITERATIONS/duration;
		System.err.println(String.format("%d verifies in %dms would be about %.3f requests per second per core with %dms latency", ITERATIONS, duration, rps, perVerify));
	}

	private
	void timedCheck(byte[] signatureValue, byte[] message, byte[] publicKey) throws Exception
	{
		long startTime=System.currentTimeMillis();
		assertTrue(Ed25519.checkvalid(signatureValue, message, publicKey));
		long duration=System.currentTimeMillis()-startTime;
		System.err.println("ed25519 signature verifies in "+duration+"ms");
	}

	private static final Logger log = LoggerFactory.getLogger(Ed25519Test.class);

	/*
	public
	void testSignatureVerification2() throws Exception
	{
		byte[] publicKey = Base64.decode(this.publicKey);
		byte[] message = this.message.getBytes("UTF-8");
		byte[] messageHash = hash(message);
		byte[] signatureValue = ???;

		log.info("message input: {}", bytesToHex(message));
		//log.info("message  hash: {}", bytesToHex(messageHash));
		log.info("signing public key: {}", bytesToHex(publicKey));
		log.info("   signature value: {}", bytesToHex(signatureValue));

		byte[] signaturePublicKeyOut = new byte[publicKey.length];

		Curve25519.verify(signaturePublicKeyOut, signatureValue, messageHash, publicKey);
		//log.info(" verify2a returned: {}", bytesToHex(signaturePublicKeyOut));

		//Curve25519_sahn0.verify(signaturePublicKeyOut, message, signatureValue, publicKey);
		//log.info(" verify2b returned: {}", bytesToHex(signaturePublicKeyOut));
	}

	private
	byte[] hash(byte[] message) throws NoSuchAlgorithmException
	{
		MessageDigest md=MessageDigest.getInstance("sha-512");
		//MessageDigest md=MessageDigest.getInstance("sha-256");
		md.reset();
		return md.digest(message);
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	/**
	 * @url http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
	 * /
	public static
	String bytesToHex(byte[] bytes)
	{
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ )
		{
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	*/

}
