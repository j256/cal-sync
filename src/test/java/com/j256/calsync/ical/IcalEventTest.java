package com.j256.calsync.ical;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.Test;

public class IcalEventTest {

	@Test
	public void testBasic() throws IOException {
		String uid = "zipper@google.com";
		String description = "\\n\\nGVPCal";
		String summary = "Stuff happening";
		String location = "Big Church";
		String filename = "Safari - Sep 6, 2019 at 13:53.pdf";
		String icalLines = "BEGIN:VEVENT\n" //
				+ "DTSTART:20191020T030000Z\n" //
				+ "DTEND:20191020T040000Z\n" //
				+ "UID:" + uid + "\n" //
				+ "DESCRIPTION:" + description + "\n" //
				+ "LOCATION:" + location + "\n" //
				+ "SUMMARY:" + summary + "\n" //
				+ "ATTACH;FILENAME=\"" + filename + "\";FMTTYPE=application/pdf\n" //
				+ " :https://drive.google.com/file/d/123/view?usp\n" //
				+ " =drive_web\n" //
				+ "END:VEVENT\n";
		try (BufferedReader reader = new BufferedReader(new StringReader(icalLines))) {
			IcalEvent event = IcalEvent.fromReader(reader);
			assertNotNull(event);
			assertEquals("Sat Oct 19 23:00:00 EDT 2019", event.getStartDate().toString());
			assertEquals("Sun Oct 20 00:00:00 EDT 2019", event.getEndDate().toString());
			assertEquals(uid, event.getUid());
			assertEquals(description, event.getDescription());
			assertEquals(summary, event.getSummary());
			assertEquals(location, event.getLocation());
			List<Attachment> attachments = event.getAttachments();
			assertNotNull(attachments);
			assertEquals(1, attachments.size());
			assertEquals(filename, attachments.get(0).getFilename());
		}
	}
}
