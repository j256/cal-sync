package com.j256.calsync.ical;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Event attachments represented in ical format.
 *
 * <pre>
 *  ATTACH;FILENAME="Safari - Sep 6, 2019 at 13:53.pdf";FMTTYPE=application/pdf
 *   :https://drive.google.com/file/d/1i9Zi3LXx5AvhqWehLRvLgB9YkcIEvGps/view?usp
 *   =drive_web
 *  ATTACH;FILENAME=Attach1quote.pdf:https://drive.google.com/file/d/1Z5F6rebqX
 *   tl1-vNZ6z486IJ7mCFFfI5f/view?usp=drive_web
 * </pre>
 */
public class Attachment {

	private static final Pattern ATTACHMENT_PATTERN =
			Pattern.compile("ATTACH;FILENAME=(\"([^\"]*)\"|([^;:]+))(;FMTTYPE=([^:]+))?:(.*)");

	private final String filename;
	// optional mime-type if in google-docs
	private final String mimetype;
	private final String url;

	public Attachment(String filename, String mimetype, String url) {
		this.filename = filename;
		this.mimetype = mimetype;
		this.url = url;
	}

	public String getFilename() {
		return filename;
	}

	public String getMimetype() {
		return mimetype;
	}

	public String getUrl() {
		return url;
	}

	/**
	 * Process an attachment line and return it or null on error.
	 */
	public static Attachment fromLine(String line) {
		Matcher matcher = ATTACHMENT_PATTERN.matcher(line);
		if (!matcher.matches()) {
			return null;
		}

		String filename = matcher.group(2);
		if (filename == null) {
			filename = matcher.group(3);
		}
		// NOTE: can be null
		String mimetype = matcher.group(5);
		String uril = matcher.group(6);
		return new Attachment(filename, mimetype, uril);
	}
}
