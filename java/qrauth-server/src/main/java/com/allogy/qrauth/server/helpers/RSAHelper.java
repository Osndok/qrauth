package com.allogy.qrauth.server.helpers;

import com.allogy.qrauth.server.entities.AuthMethod;
import com.allogy.qrauth.server.entities.DBUserAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * In an attempt to support multiple common formats, this utility will use shell commands
 * to convert between various public key formats.
 */
public
class RSAHelper implements Closeable
{
	private static final
	File SSH_KEYGEN = new File(System.getProperty("SSH_KEYGEN", "/usr/bin/ssh-keygen"));

	private static final
	File PUBKEY2SSH = new File(System.getProperty("PUBKEY2SSH", "/usr/bin/qrauth-pubkey2ssh"));

	/**
	 * @url http://rt.openssl.org/Ticket/Display.html?user=guest&pass=guest&id=2618
	 */
	private static final
	boolean OPENSSL_BUG_2618_IS_STILL_WIDESPREAD = true;

	public
	RSAHelper(String pubKey) throws IOException
	{
		if (pubKey.contains("ssh-rsa"))
		{
			//ssh-rsa format does not have ascii armor, and we have a lame parser, so it's a bit more sensitive
			sshFormat = stripPromptAndCommentsFromSshRsaKey(pubKey);
		}
		else
		if (pubKey.contains("BEGIN SSH2 PUBLIC KEY"))
		{
			final
			File file = File.createTempFile("qrauth-rsa-", ".ssh2");

			try
			{
				writeBytesToFile(pubKey.getBytes(), file);
				sshFormat = Exec.toString(SSH_KEYGEN.toString(), "-i", "-f", file.toString());
			}
			finally
			{
				file.delete();
			}
		}
		else
		if (pubKey.contains("BEGIN RSA PUBLIC KEY"))
		{
			final
			File file = File.createTempFile("qrauth-rsa-", ".ssh2");

			try
			{
				writeBytesToFile(pubKey.getBytes(), file);
				sshFormat = Exec.toString(SSH_KEYGEN.toString(), "-i", "-m", "PEM", "-f", file.toString());
			}
			finally
			{
				file.delete();
			}
		}
		else
		if (pubKey.contains("BEGIN PUBLIC KEY"))
		{
			pemFormat=pubKey;
		}
		else
		{
			throw new UnsupportedEncodingException("unknown public key format, or not an rsa key");
		}
	}

	/**
	 * Goal is to convert noisy input: "\n#comment\noption=value ssh-rsa X Y\n\n#aft noise" into a usable key
	 * @param sshPublicKey
	 * @return
	 */
	public static
	String stripPromptAndCommentsFromSshRsaKey(String sshPublicKey)
	{
		if (sshPublicKey.indexOf('\n')>=0)
		{
			final
			String[] bits=sshPublicKey.split("\n");

			for (String bit : bits)
			{
				if (!bit.isEmpty() && bit.charAt(0)!='#' && bit.contains("ssh-rsa"))
				{
					return maybeStripPrefixOptions(bit);
				}
			}

			throw new IllegalArgumentException("does not appear to be an ssh-rsa key");
		}
		else
		if (sshPublicKey.contains("ssh-rsa"))
		{
			return maybeStripPrefixOptions(sshPublicKey);
		}
		else
		{
			throw new IllegalArgumentException("does not appear to be an ssh-rsa key");
		}
	}

	private static
	String maybeStripPrefixOptions(String key)
	{
		final
		int i=key.indexOf("ssh-rsa");

		if (i<=0)
		{
			return key;
		}
		else
		{
			return key.substring(i);
		}
	}

	private
	String sshFormat;

	private
	File sshFile;

	private
	String pemFormat;

	private
	File pemFile;

	public
	File getSshFile() throws IOException
	{
		if (sshFile==null)
		{
			sshFile = File.createTempFile("qrauth-rsa-", ".pem");
			writeBytesToFile(getSshFormat().getBytes(), sshFile);
		}
		return sshFile;
	}

	public
	String getSshFormat() throws IOException
	{
		if (sshFormat==null)
		{
			if (!PUBKEY2SSH.canExecute())
			{
				throw new UnsupportedOperationException("sorry, unable to support PEM -> SSH public key conversion at this time");
			}

			sshFormat = Exec.toString(PUBKEY2SSH.toString(), getPemFile().getAbsolutePath(), "pem-no-comment");
		}

		return sshFormat;
	}

	private
	File getPemFile() throws IOException
	{
		if (pemFile==null)
		{
			pemFile = File.createTempFile("qrauth-rsa-", ".pem");
			writeBytesToFile(getPemFormat().getBytes(), pemFile);
		}

		return pemFile;
	}

	public
	String getPemFormat() throws IOException
	{
		if (pemFormat==null)
		{
			if (!SSH_KEYGEN.canExecute())
			{
				throw new UnsupportedOperationException("sorry, unable to support SSH -> PEM public key conversion at this time");
			}

			pemFormat = Exec.toString(SSH_KEYGEN.toString(), "-f", getSshFile().getAbsolutePath(), "-e", "-m", "PKCS8");
		}

		return pemFormat;
	}

	/*
	Base64.decodeBase64(base64Response)
	 */
	public
	boolean signatureIsValid(String data, byte[] signature) throws IOException
	{
		final
		File signatureFile = File.createTempFile("qrauth-rsa-", ".sig");

		try
		{
			writeBytesToFile(signature, signatureFile);

			//Uggh... it is very inconvenient to use pkeyutl due to a widespread exit-status bug...
			if (OPENSSL_BUG_2618_IS_STILL_WIDESPREAD)
			{
				final
				String signedData = Exec.toString("/usr/bin/openssl", "rsautl", "-verify", "-in",
													 signatureFile.getAbsolutePath(), "-pubin", "-inkey",
													 getPemFile().getAbsolutePath());

				return signedData.equals(data);
			}
			else
			{
				//echo -n ${NUT} | openssl pkeyutl -verify -sigfile ${SIGNED} -pubin -inkey ${PEM}

				Exec.withInput(data.getBytes(),
								  "/usr/bin/openssl", "pkeyutl", "-verify", "-sigfile",
								  signatureFile.getAbsolutePath(), "-pubin", "-inkey",
								  getPemFile().getAbsolutePath());

				return true;
			}
		}
		catch (IOException e)
		{
			//This usually means authentication failed, but can also mean the binaries aren't available, etc.
			log.warn("rsa authentication failed", e);
			return false;
		}
		finally
		{
			signatureFile.delete();
		}
	}

	private static final Logger log = LoggerFactory.getLogger(RSAHelper.class);

	private
	void writeBytesToFile(byte[] bytes, File file) throws IOException
	{
		final
		OutputStream out = new FileOutputStream(file);

		try
		{
			out.write(bytes);
		}
		finally
		{
			out.close();
		}
	}

	@Override
	public
	void close() throws IOException
	{
		if (pemFile != null)
		{
			pemFile.delete();
			pemFile = null;
		}

		if (sshFile != null)
		{
			sshFile.delete();
			sshFile = null;
		}
	}

	private transient
	String sshKeyBlob;

	private transient
	String sshComment;

	public
	String getSshKeyBlob() throws IOException
	{
		if (sshKeyBlob == null)
		{
			splitSshFormat();
		}

		return sshKeyBlob;
	}

	private
	void splitSshFormat() throws IOException
	{
		final
		String raw = getSshFormat().trim();

		//TODO: strip out comments, etc.
		if (raw.indexOf('\n') >= 0)
		{
			throw new IllegalArgumentException("cannot handle multi-line ssh-rsa keys");
		}

		final
		String[] bits = raw.split(" ");

		if (bits.length < 3)
		{
			for (int i = 0; i < bits.length; i++)
			{
				System.err.println("bit[" + i + "] = '" + bits[i] + "'");
			}

			throw new IllegalArgumentException("expecting only three key segments (type,key,comment), maybe remove spaces from comment?");
		}

		sshKeyBlob = bits[1];
		sshComment = bits[2];
	}

	public
	DBUserAuth toDBUserAuth() throws IOException
	{
		splitSshFormat();

		final
		DBUserAuth a = new DBUserAuth();

		a.authMethod = AuthMethod.RSA;
		a.pubKey = getSshKeyBlob().trim();
		a.comment = sshComment.trim();

		return a;
	}

	public
	RSAHelper(DBUserAuth a)
	{
		sshKeyBlob = a.pubKey;
		sshComment = a.comment;
		sshFormat = "ssh-rsa " + sshKeyBlob + " " + sshComment;
	}

}
