package com.allogy.qrauth.server.helpers;

/**
 * Created by robert on 3/6/15.
 */
public
class Bytes
{
	private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

	/**
	 * @url http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
	 */
	public static
	String toHex(byte[] bytes)
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

	public static
	byte[] concat(byte[] a, byte[] b)
	{
		int aLen = a.length;
		int bLen = b.length;
		byte[] c= new byte[aLen+bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		return c;
	}

	/**
	 * @url http://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
	 * @param s
	 * @return
	 */
	public static
	byte[] fromHex(String s)
	{
		final
		int len = s.length();

		final
		byte[] data = new byte[len / 2];

		for (int i = 0; i < len; i += 2)
		{
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
									  + Character.digit(s.charAt(i+1), 16));
		}

		return data;
	}
}
