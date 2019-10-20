package com.j256.calsync.ical;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Event in ical format.
 *
 * <pre>
 *  BEGIN:VEVENT
 *  DTSTART:20191020T030000Z
 *  DTEND:20191020T040000Z
 *  UID:6kob21j7sjos3ml4g6bujul9vi@google.com
 *  DESCRIPTION:\n\nGVPCal
 *  LOCATION:
 *  SUMMARY:Stuff happening
 *  ATTACH;FILENAME="Safari - Sep 6, 2019 at 13:53.pdf";FMTTYPE=application/pdf
 *   :https://drive.google.com/file/d/1i9Zi3LXx5AvhqWehLRvLgB9YkcIEvGps/view?usp
 *   =drive_web
 *  END:VEVENT
 * </pre>
 */
public class IcalEvent {

	private static final String EVENT_END_LINE = "END:VEVENT";
	private static final String ATTACHMENT_LINE_START = "ATTACH;";
	private static final DateTimeFormatter TIME_FORMAT =
			DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'").withZone(DateTimeZone.forID("UTC"));
	private static final DateTimeZone EASTERN_TIME_ZONE = DateTimeZone.forID("EST5EDT");

	private final String summary;
	private final String description;
	private final String uid;
	private final Date startDate;
	private final Date endDate;
	private final String location;
	private final List<Attachment> attachments;

	public IcalEvent(String summary, String description, String uid, Date startDate, Date endDate, String location,
			List<Attachment> attachments) {
		this.summary = summary;
		this.description = description;
		this.uid = uid;
		this.startDate = startDate;
		this.endDate = endDate;
		this.location = location;
		this.attachments = attachments;
	}

	/**
	 * Read lines until we see the end of an event and return our event instance.
	 */
	public static IcalEvent fromReader(BufferedReader reader) throws IOException {

		Map<String, String> fieldValMap = new HashMap<>();
		List<Attachment> attachments = new ArrayList<>();

		String previous = null;
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			// read until we see "END:VEVENT"
			if (line.startsWith(EVENT_END_LINE)) {
				break;
			}
			if (line.charAt(0) == ' ') {
				previous = previous + line.substring(1);
				continue;
			}

			// work on previous
			if (previous != null) {
				addLine(fieldValMap, attachments, previous);
			}

			previous = line;
		}

		// process the last line in the entry
		if (previous != null) {
			addLine(fieldValMap, attachments, previous);
		}
		return processLineMap(fieldValMap, attachments);
	}

	public String getSummary() {
		return summary;
	}

	public String getDescription() {
		return description;
	}

	public String getUid() {
		return uid;
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public String getLocation() {
		return location;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	private static void addLine(Map<String, String> fieldValMap, List<Attachment> attachments, String line) {

		// handle any attachments
		if (line.startsWith(ATTACHMENT_LINE_START)) {
			Attachment attachment = Attachment.fromLine(line);
			if (attachment != null) {
				attachments.add(attachment);
			}
			return;
		}

		int index = line.indexOf(':');
		// skip if no field:value format
		if (index <= 0 || index >= line.length() - 1) {
			return;
		}

		fieldValMap.put(line.substring(0, index), line.substring(index + 1));
	}

	private static IcalEvent processLineMap(Map<String, String> fieldValMap, List<Attachment> attachments) {

		if (fieldValMap.isEmpty()) {
			return null;
		}
		if (attachments.isEmpty()) {
			attachments = Collections.emptyList();
		}

		// DTSTART:20191020T030000Z
		Date startDate = null;
		String val = fieldValMap.get("DTSTART");
		if (val != null) {
			startDate = stringToDate(val);
		}
		// DTEND:20191020T040000Z
		Date endDate = null;
		val = fieldValMap.get("DTEND");
		if (val != null) {
			endDate = stringToDate(val);
		}
		// UID:6kob21j7sjos3ml4g6bujul9vi@google.com
		String uid = fieldValMap.get("UID");
		// DESCRIPTION:\n\nGVPCal
		// NOTE: we won't be processing the \n in the description because we just are creating new calendar entries
		String description = fieldValMap.get("DESCRIPTION");
		// SUMMARY:Stuff happening
		String summary = fieldValMap.get("SUMMARY");
		// LOCATION:Follen Community Church\, 755 Massachusetts Ave\, Lexington\, MA 0
		// 2420\, USA
		String location = fieldValMap.get("LOCATION");
		return new IcalEvent(summary, description, uid, startDate, endDate, location, attachments);
	}

	private static Date stringToDate(String str) {
		DateTime dateTime = TIME_FORMAT.parseDateTime(str).withZone(EASTERN_TIME_ZONE);
		return dateTime.toDate();
	}
}
