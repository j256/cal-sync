package com.j256.calsync;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.j256.calsync.data.DestCal;
import com.j256.calsync.data.KeywordCategory;
import com.j256.calsync.data.SourceCal;

public class CalSyncMain {

	private static final String APPLICATION_NAME = "Lexington Calendar Sync";

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these scopes, delete your previously
	 * saved tokens/ folder.
	 */
	private static final List<String> READ_ONLY_SCOPE = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
	private static final List<String> READ_WRITE_SCOPE = Collections.singletonList(CalendarScopes.CALENDAR);

	private static final String CREDENTIALS_FILE_PATH = "/Lexington Calendar Sync-56846a9d7f0d.json";

	private static final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

	private final SourceCal[] sourceCalendars = new SourceCal[] { //
			new SourceCal("utkkh55so5qug8uhpobtbsuokg@group.calendar.google.com",
					"Follen Church Gun Violence Protection", "gvp", "Follen Church"), //
			new SourceCal("s86t7a2usugb40vocnijsenumc@group.calendar.google.com",
					"Follen Church Food Insecurity Social Justice", "food", "Follen Church"), //
			new SourceCal("s86t7a2usugb40vocnijsenumc@group.calendar.google.com",
					"Follen Church Social Justice General", null /* category */, "Follen Church"), //
	};

	private final DestCal[] destinationCalendars = new DestCal[] { //
			new DestCal("bj6gac4eb0cmodtrvcvfmqkk3o@group.calendar.google.com", "Follen Church Social Justice",
					null /* category */, "Follen Church"), //
			new DestCal("ce8j0kfr11kg0t8mgj3054sm5g@group.calendar.google.com", "Lexington Gun Violence Social Action",
					"gvp", null /* no organization */), //
			new DestCal("p48r24isd9bbces9jfvvi9evk0@group.calendar.google.com",
					"Lexington Food Insecurity Social Action", "food", null /* no organization */), //
			new DestCal("ot2d2lis7hvatv7dcc6jlcf1rc@group.calendar.google.com", "Lexington Social Action",
					null /* category */, null /* no organization */), //
	};

	private final KeywordCategory[] keywordCategories = new KeywordCategory[] { //
			new KeywordCategory("foodcal", "food"), //
			new KeywordCategory("gvpcal", "gvp"), //
	};

	public static void main(String... args) throws IOException, GeneralSecurityException {

		final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		InputStream in = CalSyncMain.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		GoogleCredential credential = GoogleCredential.fromStream(in).createScoped(READ_WRITE_SCOPE);

		// build a new authorized API client service
		Calendar service =
				new Calendar.Builder(httpTransport, jsonFactory, credential).setApplicationName(APPLICATION_NAME)
						.build();

		// List the next 10 events from the primary calendar.
		DateTime now = new DateTime(System.currentTimeMillis() - 100000000);
		int maxResults = 10;
		// .setTimeMax(now)
		Events events = service.events()
				.list("primary")
				.setMaxResults(maxResults)
				.setTimeMin(now)
				.setOrderBy("startTime")
				.setSingleEvents(true)
				.setCalendarId("256.com@gmail.com")
				.execute();
		List<Event> items = events.getItems();
		if (items.isEmpty()) {
			System.out.println("No upcoming events found.");
		} else {
			System.out.println("Upcoming events");
			for (Event event : items) {
				DateTime start = event.getStart().getDateTime();
				if (start == null) {
					start = event.getStart().getDate();
				}
				System.out.printf("%s (%s)\n", event.getSummary(), start);
				// if (event.getSummary().equals("Stuff happening")) {
				// event.setSummary("Stuff happening again");
				// Event newEvent = new Event().setStart(event.getStart())
				// .setSummary("Stuff happening again")
				// .setEnd(event.getEnd())
				// .setAttachments(event.getAttachments());
				// service.events().insert("256.com@gmail.com", newEvent).execute();
				// }
			}
		}
	}
}
