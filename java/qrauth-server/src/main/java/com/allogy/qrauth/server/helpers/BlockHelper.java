package com.allogy.qrauth.server.helpers;

import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.internal.services.MarkupWriterImpl;
import org.apache.tapestry5.internal.services.RenderQueueImpl;
import org.apache.tapestry5.internal.structure.BlockImpl;
import org.apache.tapestry5.runtime.RenderQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by robert on 3/3/15.
 */
public
class BlockHelper
{
	private static final
	Logger log = LoggerFactory.getLogger(BlockHelper.class);

	public static
	InputStream toInputStream(ComponentResources componentResources, Block block)
	{
		final
		RenderQueueImpl renderQueue = new RenderQueueImpl(log);

		final
		MarkupWriter markupWriter = new MarkupWriterImpl();

		((BlockImpl)block).render(markupWriter, renderQueue);

		renderQueue.startComponent(componentResources);
		renderQueue.run(markupWriter);
		renderQueue.endComponent();

		return new ByteArrayInputStream(markupWriter.toString().getBytes());
	}
}
