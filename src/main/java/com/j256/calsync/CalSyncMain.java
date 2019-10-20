package com.j256.calsync;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import com.j256.calsync.data.KeywordCategory;
import com.j256.calsync.data.SyncedCalendar;

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

	private final SyncedCalendar[] sourceCals = new SyncedCalendar[] { //
			new SyncedCalendar("utkkh55so5qug8uhpobtbsuokg@group.calendar.google.com",
					"Follen Church Gun Violence Protection", "gvp", "Follen Church"), //
			new SyncedCalendar("s86t7a2usugb40vocnijsenumc@group.calendar.google.com",
					"Follen Church Food Insecurity Social Justice", "food", "Follen Church"), //
			new SyncedCalendar("ins6j0s8non558lam74ek17opk@group.calendar.google.com",
					"Follen Church Social Justice General", null /* category */, "Follen Church"), //
	};

	private final SyncedCalendar[] destCals = new SyncedCalendar[] { //
			new SyncedCalendar("bj6gac4eb0cmodtrvcvfmqkk3o@group.calendar.google.com", "Follen Church Social Justice",
					null /* category */, "Follen Church"), //
			new SyncedCalendar("ce8j0kfr11kg0t8mgj3054sm5g@group.calendar.google.com",
					"Lexington Gun Violence Social Action", "gvp", null /* no organization */), //
			new SyncedCalendar("p48r24isd9bbces9jfvvi9evk0@group.calendar.google.com",
					"Lexington Food Insecurity Social Action", "food", null /* no organization */), //
			new SyncedCalendar("ot2d2lis7hvatv7dcc6jlcf1rc@group.calendar.google.com", "Lexington Social Action",
					null /* category */, null /* no organization */), //
	};

	private final KeywordCategory[] keywordCategories = new KeywordCategory[] { //
			new KeywordCategory("foodcal", "food"), //
			new KeywordCategory("gvpcal", "gvp"), //
	};

	public static void main(String... args) throws IOException, GeneralSecurityException {
		final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		new CalSyncMain().doMain(httpTransport);
	}

	private void doMain(NetHttpTransport httpTransport) throws IOException {

		InputStream in = CalSyncMain.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		GoogleCredential readOnlyCredential = GoogleCredential.fromStream(in).createScoped(READ_ONLY_SCOPE);
		in = CalSyncMain.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		GoogleCredential readWriteCredential = GoogleCredential.fromStream(in).createScoped(READ_WRITE_SCOPE);

		// build a new authorized API client service
		Calendar readWriteService = new Calendar.Builder(httpTransport, jsonFactory, readWriteCredential)
				.setApplicationName(APPLICATION_NAME)
				.build();
		Calendar readOnlyService = new Calendar.Builder(httpTransport, jsonFactory, readOnlyCredential)
				.setApplicationName(APPLICATION_NAME)
				.build();

		Map<SyncedCalendar, Map<String, Event>> destCalEventMap = new HashMap<>();

		System.out.println("Loading destination calendars:");
		// open each destination calendar and load all of its events into our map
		for (SyncedCalendar destCal : destCals) {
			Map<String, Event> eventMap = new HashMap<>();
			destCalEventMap.put(destCal, eventMap);
			loadCalendarEntries(readOnlyService, destCal, eventMap);
		}
		System.out.println("-------------------------------------------------------");

		// open each destination calendar and load all of its events into our map
		for (SyncedCalendar sourceCal : sourceCals) {
			System.out.println("Processing calendar: " + sourceCal.getCalendarName());
			Map<String, Event> eventMap = new HashMap<>();
			loadCalendarEntries(readOnlyService, sourceCal, eventMap);
			resolveSourceCalendar(readWriteService, sourceCal, eventMap.values(), destCalEventMap);
			System.out.println("-------------------------");
		}
		System.out.println("-------------------------------------------------------");

		// remove out-of-date entries
		for (Entry<SyncedCalendar, Map<String, Event>> entry : destCalEventMap.entrySet()) {
			SyncedCalendar destCal = entry.getKey();
			Map<String, Event> destEvents = entry.getValue();

			for (Event event : destEvents.values()) {
				System.out.println(
						"Removing event '" + event.getSummary() + "' from calendar: " + destCal.getCalendarName());
				readWriteService.events().delete(destCal.getCalendarId(), event.getId()).execute();
			}
		}
	}

	public void foo(Calendar readOnlyService) throws IOException {
		// List the next 10 events from the primary calendar.
		DateTime now = new DateTime(System.currentTimeMillis() - 100000000);
		int maxResults = 10;
		// .setTimeMax(now)
		Events events = readOnlyService.events()
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

	private void resolveSourceCalendar(Calendar readWriteService, SyncedCalendar sourceCal,
			Collection<Event> sourceEvents, Map<SyncedCalendar, Map<String, Event>> destCalEventMap)
			throws IOException {

		List<String> categories = new ArrayList<>();
		for (Event event : sourceEvents) {
			// check on event organization
			String org = sourceCal.getOrganization();

			// determine the event categories
			categories.clear();
			if (sourceCal.getCategory() != null) {
				categories.add(sourceCal.getCategory());
			}
			String description = event.getDescription();
			if (description != null) {
				for (KeywordCategory keyCat : keywordCategories) {
					if (description.contains(keyCat.getKeyword())) {
						categories.add(keyCat.getCategory());
					}
				}
			}

			// for each dest-cal which matches org or category, see if it has the event
			for (Entry<SyncedCalendar, Map<String, Event>> entry : destCalEventMap.entrySet()) {
				SyncedCalendar destCal = entry.getKey();
				Map<String, Event> destEvents = entry.getValue();

				String destOrg = destCal.getOrganization();
				String destCat = destCal.getCategory();
				// is it an organization roll-up calendar?
				boolean match = false;
				if (destOrg == null && destCat == null) {
					// master roll-up
					match = true;
				} else if (destOrg != null) {
					if (destOrg.equals(org)) {
						// we know that the org must be null here because org/category for a dest-cal doesn't make sense
						match = true;
					}
				} else if (categories.contains(destCat)) {
					match = true;
				}
				if (!match) {
					continue;
				}

				if (destOrg == null) {
					StringBuilder sb = new StringBuilder();
					if (event.getDescription() != null) {
						sb.append(event.getDescription()).append('\n');
					}
					sb.append("Source: ").append(sourceCal.getOrganization());
					event.setDescription(sb.toString());
				}

				System.out.println("Event '" + event.getSummary() + "' matched calendar: " + destCal.getCalendarName());

				Event destEvent = destEvents.remove(event.getId());
				if (destEvent == null) {
					Event newEvent = new Event().setSummary(event.getSummary())
							.setId(event.getId())
							.setStart(event.getStart())
							.setEnd(event.getEnd())
							.setLocation(event.getLocation())
							.setDescription(event.getDescription())
							.setHtmlLink(event.getHtmlLink())
							.setAttachments(event.getAttachments());
					System.out.println(
							"Adding new event '" + event.getSummary() + "' to calendar: " + destCal.getCalendarName());
					readWriteService.events().insert(destCal.getCalendarId(), newEvent).execute();
				} else if (!eventEquals(event, destEvent)) {
					System.out.println(
							"Updating event '" + event.getSummary() + "' in calendar: " + destCal.getCalendarName());
					destEvent.setSummary(event.getSummary());
					destEvent.setStart(event.getStart());
					destEvent.setEnd(event.getEnd());
					destEvent.setLocation(event.getLocation());
					destEvent.setDescription(event.getDescription());
					destEvent.setHtmlLink(event.getHtmlLink());
					destEvent.setAttachments(event.getAttachments());
					readWriteService.events().update(destCal.getCalendarId(), destEvent.getId(), destEvent).execute();
				}
			}
		}
	}

	private boolean eventEquals(Event sourceEvent, Event destEvent) {
		return (fieldEquals(sourceEvent.getSummary(), destEvent.getSummary())
				&& fieldEquals(sourceEvent.getStart(), destEvent.getStart())
				&& fieldEquals(sourceEvent.getEnd(), destEvent.getEnd())
				&& fieldEquals(sourceEvent.getLocation(), destEvent.getLocation())
				&& fieldEquals(sourceEvent.getDescription(), destEvent.getDescription())
				&& fieldEquals(sourceEvent.getHtmlLink(), destEvent.getHtmlLink())
				&& fieldEquals(sourceEvent.getAttachments(), destEvent.getAttachments()));
	}

	private boolean fieldEquals(Object field1, Object field2) {
		if (field1 == null) {
			return (field2 == null);
		} else {
			return field1.equals(field2);
		}
	}

	private void loadCalendarEntries(Calendar service, SyncedCalendar calendar, Map<String, Event> eventMap)
			throws IOException {
		String nextPageToken = null;
		do {
			Events events = service.events()
					.list("primary")
					.setPageToken(nextPageToken)
					.setSingleEvents(true)
					.setCalendarId(calendar.getCalendarId())
					.execute();
			List<Event> eventList = events.getItems();
			for (Event event : eventList) {
				eventMap.put(event.getId(), event);
			}
			nextPageToken = events.getNextPageToken();
		} while (nextPageToken != null);
		System.out.println("Loaded " + eventMap.size() + " events from " + calendar.getCalendarName());
	}
}
