package com.allogy.qrauth.server.pages.api.sqrl;

import com.allogy.qrauth.server.entities.Nut;
import com.allogy.qrauth.server.entities.OutputStreamResponse;
import com.allogy.qrauth.server.helpers.ErrorResponse;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.services.Response;

import java.io.IOException;
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

	@InjectPage
	private
	DoSqrl doSqrl;

	Object onActivate(String nutStringValue, String fileName) throws UnknownHostException, WriterException
	{
		final
		String url=doSqrl.with(nutStringValue).getUrl();

		final
		String finalImageFormat="png";

		final
		QRCodeWriter qrCodeWriter=new QRCodeWriter();

		BarcodeFormat barcodeFormat=BarcodeFormat.QR_CODE;
		int width =177;
		int height=177;

		Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		hints.put(EncodeHintType.MARGIN, 0); /* default = 4, but we do padding with html (no need to include in bitmap) */

		final BitMatrix bitMatrix = qrCodeWriter.encode(url, barcodeFormat, width, height, hints);

		return new OutputStreamResponse()
		{
			public String getContentType()
			{
				return "image/"+finalImageFormat;
			}

			public void writeToStream(OutputStream outputStream) throws IOException
			{
				MatrixToImageWriter.writeToStream(bitMatrix, finalImageFormat, outputStream);
			}

			public void prepareResponse(Response response)
			{
				//no-op...
			}
		};
	}
}
