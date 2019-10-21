package com.j256.calsync;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.j256.calsync.dao.KeywordCategoryDao;
import com.j256.calsync.dao.KeywordCategoryDaoImpl;
import com.j256.calsync.dao.SyncedCalendarDao;
import com.j256.calsync.dao.SyncedCalendarDaoImpl;
import com.j256.calsync.data.KeywordCategory;
import com.j256.calsync.data.SyncedCalendar;
import com.j256.ormlite.jdbc.JdbcConnectionSource;

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

	public static void main(String... args) throws Exception {
		final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		new CalSyncMain().doMain(httpTransport);
	}

	private void doMain(NetHttpTransport httpTransport) throws IOException, SQLException {

		InputStream in = CalSyncMain.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		GoogleCredential readOnlyCredential = GoogleCredential.fromStream(in).createScoped(READ_ONLY_SCOPE);
		in = CalSyncMain.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		GoogleCredential readWriteCredential = GoogleCredential.fromStream(in).createScoped(READ_WRITE_SCOPE);

		// build a new authorized API client service
		Calendar readWriteCalendarService = new Calendar.Builder(httpTransport, jsonFactory, readWriteCredential)
				.setApplicationName(APPLICATION_NAME)
				.build();
		Calendar readOnlyCalendarService = new Calendar.Builder(httpTransport, jsonFactory, readOnlyCredential)
				.setApplicationName(APPLICATION_NAME)
				.build();

		// create our database objects
		JdbcConnectionSource connectionSource =
				new JdbcConnectionSource("jdbc:h2:file:/var/cal-sync/cal-sync.db;USER=cal;PASSWORD=syncer");

		KeywordCategoryDao keywordCategoryDao = new KeywordCategoryDaoImpl(connectionSource);
		SyncedCalendarDao syncedCalendarDao = new SyncedCalendarDaoImpl(connectionSource);

		List<KeywordCategory> keywordCategories = keywordCategoryDao.queryForAll();
		List<SyncedCalendar> syncedCalendars = syncedCalendarDao.queryForAll();

		Map<SyncedCalendar, Map<String, Event>> destCalEventMap = new HashMap<>();

		System.out.println("Loading destination calendars:");
		// open each destination calendar and load all of its events into our map
		for (SyncedCalendar syncedCal : syncedCalendars) {
			if (syncedCal.isSource()) {
				continue;
			}
			Map<String, Event> eventMap = loadCalendarEntries(readOnlyCalendarService, syncedCal);
			destCalEventMap.put(syncedCal, eventMap);
		}
		System.out.println("-------------------------------------------------------");

		// open each destination calendar and load all of its events into our map
		for (SyncedCalendar syncedCal : syncedCalendars) {
			if (!syncedCal.isSource()) {
				continue;
			}
			System.out.println("Processing calendar: " + syncedCal.getCalendarName());
			Map<String, Event> eventMap = loadCalendarEntries(readOnlyCalendarService, syncedCal);
			resolveSourceCalendar(readWriteCalendarService, keywordCategories, syncedCal, eventMap.values(),
					destCalEventMap);
			System.out.println("-------------------------");
		}
		System.out.println("-------------------------------------------------------");

		// remove out-of-date entries that are still left around in the destination evernt map
		for (Entry<SyncedCalendar, Map<String, Event>> entry : destCalEventMap.entrySet()) {
			SyncedCalendar destCal = entry.getKey();
			Map<String, Event> destEvents = entry.getValue();

			for (Event event : destEvents.values()) {
				System.out.println(
						"Removing event '" + event.getSummary() + "' from calendar: " + destCal.getCalendarName());
				readWriteCalendarService.events().delete(destCal.getCalendarId(), event.getId()).execute();
			}
		}
	}

	private void resolveSourceCalendar(Calendar readWriteService, List<KeywordCategory> keywordCategories,
			SyncedCalendar sourceCal, Collection<Event> sourceEvents,
			Map<SyncedCalendar, Map<String, Event>> destCalEventMap) throws IOException {

		List<String> categories = new ArrayList<>();
		for (Event event : sourceEvents) {
			// check on event organization
			String sourceOrg = sourceCal.getOrganization();
			String description = event.getDescription().trim();

			// determine the event categories
			categories.clear();
			if (sourceCal.getCategory() != null) {
				// if there is a default category on the calendar then add it in
				categories.add(sourceCal.getCategory());
			}

			// look for category keywords
			boolean hasCategory = false;
			if (description != null) {
				for (KeywordCategory keyCat : keywordCategories) {
					String keyword = keyCat.getKeyword();
					int index = description.indexOf(keyword);
					if (index < 0) {
						continue;
					}
					// cut out the keyword itself out of the description
					StringBuilder sb = new StringBuilder();
					sb.append(description, 0, index);
					sb.append(description, index + keyword.length(), description.length());
					categories.add(keyCat.getCategory());
					event.setDescription(sb.toString().trim());
					hasCategory = true;
				}
			}

			StringBuilder sb = new StringBuilder();
			String normalDescription = event.getDescription();
			if (normalDescription != null) {
				sb.append(normalDescription).append("\n\n");
			}
			sb.append("From ").append(sourceCal.getOrganization()).append(" calendar.");
			String orgDescription = sb.toString();

			// for each dest-cal which matches org or category, see if it has the event
			for (Entry<SyncedCalendar, Map<String, Event>> entry : destCalEventMap.entrySet()) {
				SyncedCalendar destCal = entry.getKey();
				Map<String, Event> destEvents = entry.getValue();

				String destOrg = destCal.getOrganization();
				String destCat = destCal.getCategory();
				// is it an organization roll-up calendar?
				boolean match = false;
				if (destOrg == null && destCat == null && (!sourceCal.isRequireCategory() || hasCategory)) {
					// master roll-up but only if one of the calendar matches
					match = true;
				} else if (destOrg != null) {
					if (destOrg.equals(sourceOrg)) {
						// we know that the org must be null here because org/category for a dest-cal doesn't make sense
						match = true;
					}
				} else if (categories.contains(destCat)) {
					match = true;
				}
				if (!match) {
					continue;
				}

				/*
				 * If our destination-org is not set then this is a roll-up cross organization calendar and display the
				 * source org.
				 */
				if (destOrg == null) {
					event.setDescription(orgDescription);
				} else {
					event.setDescription(normalDescription);
				}

				// now remove the event from detination calendar list to see if it exists
				Event destEvent = destEvents.remove(event.getId());
				if (destEvent == null) {
					destEvent = new Event();
					assignEventFields(destEvent, event);
					System.out.println(
							"Adding event '" + destEvent.getSummary() + "' to calendar: " + destCal.getCalendarName());
					readWriteService.events().insert(destCal.getCalendarId(), destEvent).execute();
				} else if (eventEquals(event, destEvent)) {
					// no changes need to be made
				} else {
					// need to update this event
					System.out.println(
							"Updating event '" + event.getSummary() + "' in calendar: " + destCal.getCalendarName());
					assignEventFields(destEvent, event);
					readWriteService.events().update(destCal.getCalendarId(), destEvent.getId(), destEvent).execute();
				}
			}
		}
	}

	private void assignEventFields(Event destEvent, Event sourceEvent) {
		destEvent.setSummary(sourceEvent.getSummary());
		destEvent.setId(sourceEvent.getId());
		destEvent.setStart(sourceEvent.getStart());
		destEvent.setEnd(sourceEvent.getEnd());
		destEvent.setLocation(sourceEvent.getLocation());
		destEvent.setDescription(sourceEvent.getDescription());
		destEvent.setAttachments(sourceEvent.getAttachments());
	}

	private boolean eventEquals(Event sourceEvent, Event destEvent) {
		return (Objects.equals(sourceEvent.getSummary(), destEvent.getSummary())
				&& Objects.equals(sourceEvent.getStart(), destEvent.getStart())
				&& Objects.equals(sourceEvent.getEnd(), destEvent.getEnd())
				&& Objects.equals(sourceEvent.getLocation(), destEvent.getLocation())
				&& Objects.equals(sourceEvent.getDescription(), destEvent.getDescription())
				&& Objects.equals(sourceEvent.getAttachments(), destEvent.getAttachments()));
	}

	private Map<String, Event> loadCalendarEntries(Calendar calendarService, SyncedCalendar calendar)
			throws IOException {
		Map<String, Event> eventMap = new HashMap<>();
		String nextPageToken = null;
		do {
			Events events = calendarService.events()
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
		return eventMap;
	}
}
