package com.j256.calsync.ical;

import static org.junit.Assert.*;

import org.junit.Test;

public class AttachmentTest {

	@Test
	public void testBasicF() {
		assertNull(Attachment.fromLine("bad"));

		String filename = "name";
		String mimetype = "type";
		String url = "url";
		Attachment attach = Attachment.fromLine("ATTACH;FILENAME=\"" + filename + "\";FMTTYPE=" + mimetype + ":" + url);
		assertNotNull(attach);
		assertEquals(filename, attach.getFilename());
		assertEquals(mimetype, attach.getMimetype());
		assertEquals(url, attach.getUrl());

		filename = "2";
		url = "foo";
		attach = Attachment.fromLine("ATTACH;FILENAME=" + filename + ":" + url);
		assertNotNull(attach);
		assertEquals(filename, attach.getFilename());
		assertNull(attach.getMimetype());
		assertEquals(url, attach.getUrl());
	}
}
