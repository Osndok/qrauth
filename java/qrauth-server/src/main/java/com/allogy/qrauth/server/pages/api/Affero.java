package com.allogy.qrauth.server.pages.api;

import com.allogy.qrauth.common.Version;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

/**
 * WARNING: This file carries legal implications, as does the mechanism it represents and those it depends on.
 *
 * In particular, if you modify or add to this project in any way, you are required to make sure this mechanism
 * actually works when deployed on a computer network.
 *
 * For details, please consult the license that came with this software.
 *
 * Created by robert on 2015-02-12.
 */
public
class Affero
{
	Object onActivate()
	{
		return new ErrorResponse(404, "affero function not specified");
	}

	Object onActivate(String mode)
	{
		if (mode.equals("hash"))
		{
			return new TextStreamResponse("text/plain", Version.GIT_HASH);
		}
		else
		if (mode.equals("source"))
		{
			if (sourceCodeResponse==null)
			{
				generateStaticSourceCodeResponse();
			}
			return sourceCodeResponse;
		}
		else
		{
			return new ErrorResponse(404, "invalid function");
		}
	}

	private static
	Object sourceCodeResponse;

	private static synchronized
	void generateStaticSourceCodeResponse()
	{
		if (sourceCodeResponse!=null) return;

		try
		{
			//If the deployment manager specified a URL, that has the lowest per-request overhead...
			{
				/**
				 * WARNING: when specifying AFFERO_URL, you must be *SURE* that it points directly to the actual file,
				 * anything else (including a "downloading soon" bounce page, or "registration required first" page)
				 * is *NOT* in compliance.
				 */
				String redirectUrl=systemPropertyOrEnvironmentVariable("AFFERO_URL");

				if (redirectUrl!=null)
				{
					sourceCodeResponse=new URL(redirectUrl);
					return;
				}
			}

			//If the deployment manager as specified a file, we can serve that up without much trouble...
			{
				final
				File file;
				{
					String filePath = systemPropertyOrEnvironmentVariable("AFFERO_FILE");

					if (filePath == null)
					{
						file = locateExistingSourceArchiveBasedOnAvailableVersionInformation();
					}
					else
					{
						file=new File(filePath);
					}
				}

				if (file!=null)
				{
					final
					String mimeType= Files.probeContentType(file.toPath());

					sourceCodeResponse=new StreamResponse()
					{
						@Override
						public
						String getContentType()
						{
							return mimeType;
						}

						@Override
						public
						InputStream getStream() throws IOException
						{
							return new FileInputStream(file);
						}

						@Override
						public
						void prepareResponse(Response response)
						{
							response.setHeader("Pragma", "public");
							response.setHeader("Cache-Control", "public");
							response.setHeader("Content-Disposition", "attachment; filename="+file.getName());
						}
					};

					return;
				}
			}

			sourceCodeResponse=new ErrorResponse(500, "non-compliant");
		}
		catch (Throwable t)
		{
			LoggerFactory.getLogger(Affero.class)
				.error("unable to generate affero compliance response", t);
			sourceCodeResponse=new ErrorResponse(500, "non-compliant:error:"+t);
		}
	}

	private static
	File locateExistingSourceArchiveBasedOnAvailableVersionInformation()
	{
		String baseName=systemPropertyOrEnvironmentVariable("AFFERO_BASE");

		if (baseName==null)
		{
			baseName="qrauth-";
		}

		String extension=systemPropertyOrEnvironmentVariable("AFFERO_EXT");

		if (extension==null)
		{
			extension=".tar.gz";
		}

		String afferoDir=systemPropertyOrEnvironmentVariable("AFFERO_DIR");

		if (afferoDir==null)
		{
			final
			String fullBase=baseName+Version.FULL+extension;

			File guess=new File("/usr/src", fullBase);

			if (guess.exists())
			{
				return guess;
			}

			guess=new File("/usr/share/src", fullBase);

			if (guess.exists())
			{
				return guess;
			}

			guess=new File("/usr/share", fullBase);

			if (guess.exists())
			{
				return guess;
			}

			guess=new File("/", fullBase);

			if (guess.exists())
			{
				return guess;
			}

			//Be lenient, as there is really not enough information specified to locate the source code, and we can't really be sure where to look.
			//If we can't find something obvious (and real) return null (which ends up meaning 'non-compliant').
			return null;
		}
		else
		{
			//Try the uncommon 'by-commit-hash' first...
			{
				File byHash = new File(afferoDir, baseName + Version.GIT_HASH + extension);

				if (byHash.exists())
				{
					return byHash;
				}
			}

			//Otherwise, we will assume that the file is at the expected spot (or will soon appear there).
			return new File(afferoDir, baseName + Version.FULL + extension);
		}
	}

	private static
	String systemPropertyOrEnvironmentVariable(String key)
	{
		String retval=System.getProperty(key);

		if (retval==null)
		{
			retval=System.getenv(key);
		}

		return retval;
	}
}
