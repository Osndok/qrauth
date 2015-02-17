package com.allogy.qrauth.server.services;

/**
 * Created by robert on 2/16/15.
 */
public
interface Nuts
{
	byte[] generateBytes();

	String toStringValue(byte[] bytes);

	/**
	 * @return the byte-value equivalent of the given nut string value, or null if it cannot be a nut
	 */
	byte[] fromStringValue(String value);
}
