package plugins.gzip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.utils.Utils;
import play.vfs.VirtualFile;

public class GzipCompressionPlugin extends PlayPlugin {

	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	public static final String IF_NONE_MATCH = "If-None-Match";

	@Override
	public void onApplicationReady() {
		System.out.println("GzipCompresionPlugin listo!!");
	}

	@Override
	public boolean serveStatic(VirtualFile file, Request request, Response response) {

		Request servletRequest = request;
		Response servletResponse = response;

		servletResponse.contentType = MimeTypes.getContentType(file.getName());

		try {
			if (Play.mode == Play.Mode.DEV) {
				servletResponse.setHeader("Cache-Control", "no-cache");
				servletResponse.setHeader("Content-Length", String.valueOf(file.length()));
				servletResponse.setHeader("Vary", "Accept-Encoding");
				// para que puedan servirse desde cloudfront por CORS
				servletResponse.setHeader("Access-Control-Allow-Origin", "*");
				if (!servletRequest.method.equals("HEAD")) {
					if (acceptsGzip(servletRequest)) {
						copyStreamAndGzip(servletResponse, file.inputstream());
					} else {
						copyStream(servletResponse, file.inputstream());
					}
				} else {
					copyStream(servletResponse, new ByteArrayInputStream(new byte[0]));
				}
			} else {
				long last = file.lastModified();
				String etag = "\"" + last + "-" + file.hashCode() + "\"";
				if (!isModified(etag, last, servletRequest)) {
					servletResponse.setHeader("Etag", etag);
					servletResponse.status = 304;
				} else {
					servletResponse.setHeader("Last-Modified", Utils.getHttpDateFormatter().format(new Date(last)));
					servletResponse.setHeader("Cache-Control",
							"max-age=" + Play.configuration.getProperty("http.cacheControl", "3600"));
					servletResponse.setHeader("Etag", etag);
					servletResponse.setHeader("Vary", "Accept-Encoding");
					// para que puedan servirse desde cloudfront por CORS
					servletResponse.setHeader("Access-Control-Allow-Origin", "*");
					if (acceptsGzip(servletRequest)) {
						copyStreamAndGzip(servletResponse, file.inputstream());
					} else {
						copyStream(servletResponse, file.inputstream());
					}
				}
			}
		} catch (IOException e) {
			Logger.warn(e, "Error al mostrar archivo estatico ");
			throw new RuntimeException("Error al mostrar archivo estatico ", e);
		}
		return true;
	}

	public static boolean acceptsGzip(Request servletRequest) {
		return getHeader(servletRequest, "accept-encoding").contains("gzip") && !"xls".equals(servletRequest.format);
	}

	public static String getHeader(Request servletRequest, String header) {
		if (servletRequest.headers.containsKey(header)) {
			return servletRequest.headers.get(header).value();
		}
		return "";
	}

	private void copyStream(Response servletResponse, InputStream is) throws IOException {
		OutputStream os = servletResponse.out;
		byte[] buffer = new byte[8096];
		int read = 0;
		while ((read = is.read(buffer)) > 0) {
			os.write(buffer, 0, read);
		}
		os.flush();
		is.close();
	}

	private void copyStreamAndGzip(Response servletResponse, InputStream is) throws IOException {

		final ByteArrayOutputStream stringOutputStream = new ByteArrayOutputStream((int) (is.available() * 0.75));
		final OutputStream gzipOutputStream = new GZIPOutputStream(stringOutputStream);

		final byte[] buf = new byte[5000];
		int len;
		while ((len = is.read(buf)) > 0) {
			gzipOutputStream.write(buf, 0, len);
		}

		is.close();
		gzipOutputStream.close();

		servletResponse.setHeader("Content-Encoding", "gzip");
		servletResponse.setHeader("Content-Length", stringOutputStream.size() + "");
		servletResponse.out = stringOutputStream;
	}

	public static boolean isModified(String etag, long last, Request request) {
		// See section 14.26 in rfc 2616 http://www.faqs.org/rfcs/rfc2616.html
		String browserEtag = request.headers.containsKey(IF_NONE_MATCH) ? request.headers.get(IF_NONE_MATCH).value()
				: null;
		String dateString = request.headers.containsKey(IF_MODIFIED_SINCE) ? request.headers.get(IF_MODIFIED_SINCE)
				.value() : null;
		if (browserEtag != null) {
			boolean etagMatches = browserEtag.equals(etag);
			if (!etagMatches) {
				return true;
			}
			if (dateString != null) {
				return !isValidTimeStamp(last, dateString);
			}
			return false;
		} else {
			if (dateString != null) {
				return !isValidTimeStamp(last, dateString);
			} else {
				return true;
			}
		}
	}

	private static boolean isValidTimeStamp(long last, String dateString) {
		try {
			long browserDate = Utils.getHttpDateFormatter().parse(dateString).getTime();
			return browserDate >= last;
		} catch (ParseException e) {
			Logger.error("Can't parse date", e);
			return false;
		}
	}

}
