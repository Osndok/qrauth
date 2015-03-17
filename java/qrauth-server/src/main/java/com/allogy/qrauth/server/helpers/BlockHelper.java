package com.allogy.qrauth.server.helpers;

import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.internal.services.MarkupWriterImpl;
import org.apache.tapestry5.internal.services.RenderQueueImpl;
import org.apache.tapestry5.internal.structure.BlockImpl;
import org.apache.tapestry5.runtime.RenderQueue;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by robert on 3/3/15.
 */
public
class BlockHelper
{
	private static final
	Logger log = LoggerFactory.getLogger(BlockHelper.class);

	private static final
	boolean REMOVE_DOCTYPE = true;

	public static
	byte[] toBytes(ComponentResources componentResources, Block block)
	{
		final
		RenderQueueImpl renderQueue = new RenderQueueImpl(log);

		final
		MarkupWriter markupWriter = new MarkupWriterImpl();

		((BlockImpl) block).render(markupWriter, renderQueue);

		renderQueue.startComponent(componentResources);
		renderQueue.run(markupWriter);
		renderQueue.endComponent();

		final
		String stringValue;
		{
			final
			String rendered = markupWriter.toString();

			if (!REMOVE_DOCTYPE)
			{
				stringValue = rendered;
			}
			else if (rendered.startsWith("<?"))
			{
				log.debug("doctype-1");
				stringValue = rendered.substring(rendered.indexOf("?>") + 2);
			}
			else if (rendered.startsWith("<!"))
			{
				log.debug("doctype-2");
				stringValue = rendered.substring(rendered.indexOf(">") + 1);
			}
			else
			{
				log.debug("unknown (or missing) doctype");
				stringValue = rendered;
			}
		}

		return stringValue.getBytes();
	}

	public static
	InputStream toInputStream(ComponentResources componentResources, Block block)
	{
		return new ByteArrayInputStream(toBytes(componentResources, block));
	}

	public static
	StreamResponse toResponse(final String contentType, ComponentResources componentResources, Block block)
	{
		//Render it now, as the stream response *might* be delivered off-thread.
		final
		byte[] bytes=toBytes(componentResources, block);

		return new StreamResponse()
		{
			@Override
			public
			String getContentType()
			{
				return contentType;
			}

			@Override
			public
			InputStream getStream() throws IOException
			{
				return new ByteArrayInputStream(bytes);
			}

			@Override
			public
			void prepareResponse(Response response)
			{
				response.setContentLength(bytes.length);
			}
		};
	}
}
