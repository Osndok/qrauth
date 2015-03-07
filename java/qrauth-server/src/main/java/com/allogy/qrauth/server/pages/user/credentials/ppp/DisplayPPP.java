package com.allogy.qrauth.server.pages.user.credentials.ppp;

import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.helpers.PPP_Engine;
import com.allogy.qrauth.server.helpers.PPP_Helper;
import com.allogy.qrauth.server.pages.user.credentials.AbstractCredentialsPage;
import com.allogy.qrauth.server.services.Policy;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.slf4j.Logger;

/**
 * Created by robert on 3/6/15.
 */
public
class DisplayPPP extends AbstractCredentialsPage
{
	@Property
	private
	PPP_Helper pppHelper;

	@Property
	private
	PPP_Engine pppEngine;

	@Inject
	private
	Policy policy;

	@Inject
	private
	Logger log;

	@Inject
	@Symbol(SymbolConstants.PRODUCTION_MODE)
	private
	boolean productionMode;

	Object onActivate()
	{
		pppHelper = new PPP_Helper(userAuth);

		if (pppHelper.hasRecentVolley(policy.longestReasonableAddCredentialTaskLength()) || !productionMode)
		{
			log.info("revealing {}", userAuth);
			pppEngine=pppHelper.getPPP_Engine();
			return null;
		}
		else
		{
			return new ErrorResponse(400, "sorry, only recently-generate password sheets can be viewed");
		}
	}

	/**
	 * Output the HTML5 doctype, as a work-around to https://issues.apache.org/jira/browse/TAP5-1040
	 */
	@SetupRender
	final
	void renderDocType(final MarkupWriter writer)
	{
		writer.getDocument().raw("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">");
	}

	@Property
	private
	Integer pageNumber;

	private static final int[] ROWS=new int[]{1,2,3,4,5,6,7,8,9,10};

	public
	int[] getRowNumbers()
	{
		return ROWS;
	}

	@Property
	private
	int rowNumber;

	public
	String getRowPrefix()
	{
		if (rowNumber < 10)
		{
			return "&nbsp;" + rowNumber;
		}
		else
		{
			return Integer.toString(rowNumber);
		}
	}

	public
	String getRowData()
	{
		StringBuilder sb=null;

		for (String passcode : pppHelper.getRowData(pageNumber, rowNumber))
		{
			if (sb==null)
			{
				sb=new StringBuilder();
			}
			else
			{
				sb.append("&nbsp;");
			}
			sb.append(passcode);
		}

		return sb.toString();
	}
}
