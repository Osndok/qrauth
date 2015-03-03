package com.allogy.qrauth.server.components;

import com.allogy.qrauth.server.entities.DBUserAuth;
import com.allogy.qrauth.server.helpers.Death;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.dom.Element;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;

/**
 * Created by robert on 3/2/15.
 */
public
class UserAuthLink
{
	@Inject
	private
	PageRenderLinkSource pageRenderLinkSource;

	@Parameter
	private
	DBUserAuth value;

	boolean beginRender(MarkupWriter markupWriter)
	{
		if (value==null) return false;

		final
		String link=pageRenderLinkSource.createPageRenderLinkWithContext("user/credentials/edit", value.id).toURI();

		final
		String cssClass=(Death.hathVisited(value)?"dead":"alive");

		final
		Element element=markupWriter.element("a", "href", link, "class", cssClass);

		//TODO: use a user-specific (and type-specific?) counters, rather than the sequence number.
		markupWriter.write(value.authMethod+"-"+value.id);
		markupWriter.end();

		return false;
	}
}
