package com.allogy.qrauth.server.helpers;

import com.allogy.qrauth.server.entities.AuthMethod;
import com.allogy.qrauth.server.entities.DBUser;
import com.allogy.qrauth.server.entities.DBUserAuth;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by robert on 3/6/15.
 */
public
class PPP_Helper
{
	public static final  int  CARDS_TO_REVEAL           = 3;
	private static final long NUM_LEFT_TILL_RUNNING_LOW = 10;

	final
	byte[] seed;

	long volleyAdvanceDate;

	long nextVolleyToReveal;

	long nextPasswordCounter;

	String comment;

	public
	PPP_Helper(String comment)
	{
		nextPasswordCounter = 0;
		nextVolleyToReveal = CARDS_TO_REVEAL * PPP_Engine.NO_OF_PASSCODES_PER_CARD;
		volleyAdvanceDate = System.currentTimeMillis();
		seed = new byte[32];
		this.comment=comment;
		ThreadLocalRandom.current().nextBytes(seed);
	}

	private transient
	DBUserAuth userAuth;

	public
	PPP_Helper(DBUserAuth userAuth)
	{
		final
		String[] bits=userAuth.secret.split(":");

		this.userAuth=userAuth;
		this.comment=userAuth.comment;

		this.seed=Bytes.fromHex(bits[0]);
		this.volleyAdvanceDate=Long.parseLong(bits[1]);
		this.nextVolleyToReveal=Long.parseLong(bits[2]);
		this.nextPasswordCounter=Long.parseLong(bits[3]);
	}

	private
	String getDBSecret()
	{
		return Bytes.toHex(seed)+':'+volleyAdvanceDate+':'+nextVolleyToReveal+':'+nextPasswordCounter;
	}

	public
	DBUserAuth toDBUserAuth(DBUser user)
	{
		userAuth=new DBUserAuth();
		userAuth.user=user;
		userAuth.authMethod = AuthMethod.PAPER_PASSWORDS;
		userAuth.millisGranted = (int)AuthMethod.PAPER_PASSWORDS.getDefaultLoginLength();
		userAuth.secret=getDBSecret();
		userAuth.comment=comment;

		return userAuth;
	}

	public
	void advancePasscode()
	{
		nextPasswordCounter++;
		userAuth.secret = getDBSecret();
	}

	public
	void advanceVolley()
	{
		nextPasswordCounter = nextVolleyToReveal;
		nextVolleyToReveal += CARDS_TO_REVEAL*PPP_Engine.NO_OF_PASSCODES_PER_CARD;
		volleyAdvanceDate = System.currentTimeMillis();
		userAuth.secret = getDBSecret();
	}

	public
	boolean isRunningLowOnPasscodes()
	{
		return nextVolleyToReveal-NUM_LEFT_TILL_RUNNING_LOW >= nextPasswordCounter;
	}

	public
	String getChallenge()
	{
		return PPP_Engine.getChallenge(nextPasswordCounter);
	}

	public
	boolean hasRecentVolley(long window)
	{
		return System.currentTimeMillis() < volleyAdvanceDate +window;
	}

	private transient
	PPP_Engine pppEngine;

	public
	boolean testAndIncrement(String response)
	{
		if (response.equals(getPPP_Engine().getPasscode(nextPasswordCounter)))
		{
			advancePasscode();
			return true;
		}
		else
		{
			return false;
		}
	}

	public
	PPP_Engine getPPP_Engine()
	{
		if (pppEngine == null)
		{
			pppEngine = new PPP_Engine(seed);
		}

		return pppEngine;
	}

	public
	Integer[] getActivePageNumbers()
	{
		final
		long firstCounter=nextVolleyToReveal-CARDS_TO_REVEAL*PPP_Engine.NO_OF_PASSCODES_PER_CARD;

		final
		int firstPageNumber=PPP_Engine.getPageNumberOfCounter(firstCounter);

		final
		Integer[] retval=new Integer[CARDS_TO_REVEAL];

		for(int i=0; i<CARDS_TO_REVEAL; i++)
		{
			retval[i]=firstPageNumber+i;
		}

		return retval;
	}

	public
	String getComment()
	{
		return comment;
	}

	public
	String[] getRowData(int cardNumber, int rowNumber)
	{
		final
		String[] retval=new String[PPP_Engine.NO_OF_COLUMNS];

		for (int i=0; i<PPP_Engine.NO_OF_COLUMNS; i++)
		{
			long counter=(cardNumber-1)*PPP_Engine.NO_OF_PASSCODES_PER_CARD+(rowNumber-1)*PPP_Engine.NO_OF_COLUMNS+i;
			retval[i]=pppEngine.getPasscode(counter);
		}

		return retval;
	}
}
