package com.allogy.qrauth.server.pages.api.sqrl;

import com.allogy.qrauth.server.entities.Nut;
import com.allogy.qrauth.server.entities.OutputStreamResponse;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.allogy.qrauth.server.services.impl.Config;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.ioc.Resource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Created by robert on 2/19/15.
 */
public
class QrSqrl
{
	private
	String nutStringValue;

	private
	String filename;

	Object onActivate()
	{
		return new ErrorResponse(404, "missing nut string value or file name");
	}

	Object[] onPassivate()
	{
		return new Object[]{
			nutStringValue,
			filename
		};
	}

	public
	QrSqrl with(Nut nut)
	{
		nutStringValue=nut.stringValue;
		filename="qr.png";
		return this;
	}

	@Inject
	@Path("context:images/qr-failure.png")
	private
	Asset qrFailure;

	@Inject
	private
	Response response;

	@InjectPage
	private
	DoSqrl doSqrl;

	Object onActivate(String nutStringValue, String fileName) throws UnknownHostException, WriterException
	{
		//TODO: is it likely that someone would want to intentionally offload qr-generation to a different domain?
		if (requestedDomainDoesNotMatchExpectedDomain())
		{
			response.setStatus(500);
			return assetResponse("image/png", qrFailure);
		}

		final
		String url = doSqrl.with(nutStringValue).getUrl();

		final
		String finalImageFormat = "png";

		final
		QRCodeWriter qrCodeWriter = new QRCodeWriter();

		BarcodeFormat barcodeFormat = BarcodeFormat.QR_CODE;
		int width = 177;
		int height = 177;

		Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		hints.put(EncodeHintType.MARGIN, 0); /* default = 4, but we do padding with html (no need to include in bitmap) */

		final BitMatrix bitMatrix = qrCodeWriter.encode(url, barcodeFormat, width, height, hints);

		return new OutputStreamResponse()
		{
			public
			String getContentType()
			{
				return "image/" + finalImageFormat;
			}

			public
			void writeToStream(OutputStream outputStream) throws IOException
			{
				MatrixToImageWriter.writeToStream(bitMatrix, finalImageFormat, outputStream);
			}

			public
			void prepareResponse(Response response)
			{
				//no-op...
			}
		};
	}

	private
	StreamResponse assetResponse(final String mimeType, final Asset asset)
	{
		final
		Resource resource = asset.getResource();

		return new StreamResponse()
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
				return resource.openStream();
			}

			@Override
			public
			void prepareResponse(Response response)
			{
				//no-op...
			}
		};
	}

	@Inject
	private
	Logger log;

	@Inject
	private
	Request request;

	private
	boolean requestedDomainDoesNotMatchExpectedDomain()
	{
		final
		String expectedDomain = Config.get().getSqrlDomain();

		final
		String requestedDomain = request.getHeader("Host");

		if (expectedDomain != null && requestedDomain != null)
		{
			//We use 'startsWith' to avoid the port number...
			//NB: this is just to detect misconfigurations, not a security barrier.
			if (requestedDomain.startsWith(expectedDomain))
			{
				return false;
			}
			else
			{
				log.error("expectedDomain: '{}', requestedDomain: '{}'", expectedDomain, requestedDomain);
				return true;
			}
		}
		else
		{
			return false;
		}
	}
}
