
package com.allogy.qrauth.common;

import java.io.PrintStream;
//import com.allogy.infra.wiring.entities.ServiceId;

public final
class Version
{
	public static final String GROUP = "${project.groupId}";

	public static final String FULL = "${project.version}";

	private static final int divider = FULL.indexOf('.');

	public static final int    MAJOR;
	public static final String MINOR;

	static
	{
		if (divider>0)
		{
			MAJOR = Integer.parseInt(FULL.substring(0, divider));
			MINOR = FULL.substring(divider + 1);
		}
		else
		{
			MAJOR = 0;
			MINOR = FULL;
		}
	}

	public static final boolean IS_SNAPSHOT = MINOR.toLowerCase().contains("snapshot");

	public static final Integer FOR_WIRING = (IS_SNAPSHOT ? null : MAJOR);

	/**
	 * The development port number is not marked as final because it may be possible/desired
	 * to run end-to-end unit tests, where the tested server's port number can be injected
	 * into this field. There is currently no other known reason to change this field at
	 * runtime.
	 */
	public static int DEVELOPMENT_PORT_NUMBER = Integer.parseInt("${com.allogy.web.port}");

	//public static final ServiceId SERVICE_ID=new ServiceId("qrauth", FOR_WIRING, DEVELOPMENT_PORT_NUMBER);

	public static final String GIT_BRANCH="${git.branch}";
	public static final String GIT_HASH="${git.commit.id}";

	public static final String BUILD_TIME="${build.timestamp}";
	//public static final String BUILD_TIME="${git.build.time}";
	public static final String COMMIT_TIME="${git.commit.time}";

	private
	Version()
	{
		throw new UnsupportedOperationException();
	}

	public static
	void main(String[] args)
	{
		if (args.length > 0)
		{
			System.err.println("usage: "+Version.class+"\n\tprints version information for "+GROUP);
			System.exit(1);
		}

		final
		PrintStream ps=System.out;

		ps.print("GROUP="     ); ps.println(GROUP);
		ps.print("VERSION="   ); ps.println(FULL );
		ps.print("MAJOR="     ); ps.println(MAJOR);
		ps.print("MINOR="     ); ps.println(MINOR);
		ps.print("MODE="      ); ps.println(IS_SNAPSHOT?"snapshot":"release");
		ps.print("DEV_PORT="  ); ps.println(DEVELOPMENT_PORT_NUMBER);
		ps.print("GIT_BRANCH="); ps.println(GIT_BRANCH);
		ps.print("GIT_HASH="  ); ps.println(GIT_HASH);

		//The timestamps may have a space, so lets try and make them 'nice' (i.e. bash-parsable string quotes)
		ps.print("COMMIT_TIME=\""); ps.print(COMMIT_TIME); ps.println('"');
		ps.print("BUILD_TIME=\"" ); ps.print(BUILD_TIME ); ps.println('"');

		System.exit(0);
	}
}
