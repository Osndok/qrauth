package com.allogy.qrauth.server.entities;

/**
 * Created by robert on 3/20/15.
 */
public
enum UsernameType
{
	EXPLICIT,
	IMPLICIT,
	USER_SEQUENCE("user"),
	EMAIL_ADDRESS("email"),
	PHONE_NUMBER("phone"),
	OPEN_ID("openid"),
	SKYPE("skype"),
	;

	private static final String UNIX_PREFIX_SEPARATOR="_";
	private static final String DATABASE_PREFIX_SEPARATOR="|";
	private static final String USER_INPUT_SEPARATOR=":";

	private final
	String prefixBase;

	private
	UsernameType()
	{
		prefixBase = null;
	}

	private
	UsernameType(String prefixBase)
	{
		this.prefixBase = prefixBase;
	}

	public static
	UsernameType fromUserInput(String userInput, UsernameType _default)
	{
		if (userInput==null)
		{
			return _default;
		}

		final
		int i=userInput.indexOf(USER_INPUT_SEPARATOR);

		if (i<=0)
		{
			return _default;
		}
		else
		{
			final
			String specifiedBase=userInput.substring(0, i);

			for (UsernameType usernameType : values())
			{
				final
				String thisBase=usernameType.prefixBase;

				if (specifiedBase.equals(thisBase))
				{
					return usernameType;
				}
			}

			return _default;
		}
	}

	public static
	String prefixRemovedFrom(String userInput)
	{
		final
		int i=userInput.indexOf(USER_INPUT_SEPARATOR);

		return userInput.substring(i+1);
	}

	public
	String withDatabasePrefixFollowedBy(String digestedString)
	{
		if (prefixBase==null)
		{
			return digestedString;
		}
		else
		{
			return prefixBase+DATABASE_PREFIX_SEPARATOR+digestedString;
		}
	}

	public
	void appendUnixPrefixAndSeparator(StringBuilder sb)
	{
		if (prefixBase!=null)
		{
			sb.append(prefixBase);
			sb.append(UNIX_PREFIX_SEPARATOR);
		}
	}
}
