package com.allogy.qrauth.server.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

/**
 * Created by robert on 3/4/15.
 */
public
class Ed25519
{
	private static final
	File QRAUTH_VERIFY = new File("/usr/bin/qrauth-ed25519-verify");

	private static final
	Logger log = LoggerFactory.getLogger(Ed25519.class);

	private static final
	int NUM_PROCESSORS=Runtime.getRuntime().availableProcessors();

	private static final
	Semaphore processLimiter = new Semaphore(NUM_PROCESSORS, true);

	/*
	 * Parameters match pure-java implementation, for consistency.
	 */
	public static
	boolean checkvalid(byte[] signature, byte[] message, byte[] publicKey) throws Exception
	{
		if (QRAUTH_VERIFY.canExecute())
		{
			processLimiter.acquire();
			try
			{
				//TODO: if needed, we can probably get about 2x throughput by batching 64 verify requests together...
				//      noting that we would then need to return either individual responses, or in the rare case of a
				//      verify failure start a binary search to track down the culprit.
				return exec(signature, message, publicKey);
			}
			finally
			{
				processLimiter.release();
			}
		}
		else
		{
			return Ed25519_PureJava.checkvalid(signature, message, publicKey);
		}
	}

	private static
	boolean exec(byte[] signature, byte[] message, byte[] publicKey) throws IOException, InterruptedException
	{
		final
		Process process = Runtime.getRuntime().exec(QRAUTH_VERIFY.getAbsolutePath());

		final
		OutputStream out = process.getOutputStream();

		out.write(publicKey);
		out.write(signature);
		out.write(message);
		out.close();

		final
		int status=process.waitFor();

		return (status==0);
	}
}
