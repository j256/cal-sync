package com.j256.calsync;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

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
import com.j256.calsync.dao.OrganizationDao;
import com.j256.calsync.dao.OrganizationDaoImpl;
import com.j256.calsync.dao.SyncPathDao;
import com.j256.calsync.dao.SyncPathDaoImpl;
import com.j256.calsync.dao.SyncedCalendarDao;
import com.j256.calsync.dao.SyncedCalendarDaoImpl;
import com.j256.calsync.data.Category;
import com.j256.calsync.data.KeywordCategory;
import com.j256.calsync.data.Organization;
import com.j256.calsync.data.SyncPath;
import com.j256.calsync.data.SyncedCalendar;
import com.j256.ormlite.jdbc.JdbcConnectionSource;

/**
 * Main class.
 * 
 * @author graywatson
 */
public class CalSyncMain {

	private static final String APPLICATION_NAME = "Lexington Calendar Sync";
	private static final String SQL_URL = "jdbc:postgresql://localhost/cal";
	private static final String SQL_USERNAME = "cal";
	private static final String SQL_PASSWORD = "dates";

	private static final List<String> READ_ONLY_SCOPE = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
	private static final List<String> READ_WRITE_SCOPE = Collections.singletonList(CalendarScopes.CALENDAR);

	private static final String CREDENTIALS_FILE_PATH = "/Lexington_Calendar_Sync-56846a9d7f0d.json";

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
		JdbcConnectionSource connectionSource = new JdbcConnectionSource(SQL_URL, SQL_USERNAME, SQL_PASSWORD);

		KeywordCategoryDao keywordCategoryDao = new KeywordCategoryDaoImpl(connectionSource);
		SyncedCalendarDao syncedCalendarDao = new SyncedCalendarDaoImpl(connectionSource);
		SyncPathDao syncPathDao = new SyncPathDaoImpl(connectionSource);
		OrganizationDao organizationDao = new OrganizationDaoImpl(connectionSource);

		List<KeywordCategory> keywordCategories = keywordCategoryDao.queryForAll();
		List<SyncedCalendar> syncedCalendars = syncedCalendarDao.queryForAll();
		List<SyncPath> syncPaths = syncPathDao.queryForAll();
		List<Organization> organizations = organizationDao.queryForAll();
		connectionSource.close();

		Map<Integer, SyncedCalendar> calIdMap = new HashMap<>();
		for (SyncedCalendar syncedCalendar : syncedCalendars) {
			calIdMap.put(syncedCalendar.getId(), syncedCalendar);
		}
		Map<Integer, Organization> orgIdMap = new HashMap<>();
		for (Organization organization : organizations) {
			orgIdMap.put(organization.getId(), organization);
		}

		Map<SyncedCalendar, List<SyncedCalendar>> sourceCalToDestCalMap = new HashMap<>();
		for (SyncPath syncPath : syncPaths) {
			SyncedCalendar sourceCal = calIdMap.get(syncPath.getSourceCalendar().getId());
			if (sourceCal == null) {
				System.err.println("Unknown source-cal id: " + syncPath.getSourceCalendar().getId());
				continue;
			}
			if (!sourceCal.isSource()) {
				System.err.println("SyncPath source-cal is not marked as source: " + sourceCal);
				continue;
			}
			List<SyncedCalendar> destCals = sourceCalToDestCalMap.get(sourceCal);
			if (destCals == null) {
				destCals = new ArrayList<>();
				sourceCalToDestCalMap.put(sourceCal, destCals);
			}
			SyncedCalendar destCal = calIdMap.get(syncPath.getDestCalendar().getId());
			if (destCal == null) {
				System.err.println("Unknown dest-cal id: " + syncPath.getDestCalendar().getId());
			} else if (destCal.isSource()) {
				System.err.println("SyncPath dest-cal is marked as source: " + destCal);
			} else {
				destCals.add(destCal);
			}
		}

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

		// open each source-cal calendar and sync it
		for (Entry<SyncedCalendar, List<SyncedCalendar>> entry : sourceCalToDestCalMap.entrySet()) {
			SyncedCalendar sourceCal = entry.getKey();
			List<SyncedCalendar> destCals = entry.getValue();
			System.out.println("Processing calendar: " + sourceCal.getName());
			// System.out.println(" destination calendars: " + destCals);
			Map<String, Event> eventMap = loadCalendarEntries(readOnlyCalendarService, sourceCal);
			syncSourceCalendar(readWriteCalendarService, keywordCategories, sourceCal, eventMap.values(), destCals,
					destCalEventMap, orgIdMap);
			System.out.println("-------------------------");
		}
		System.out.println("-------------------------------------------------------");

		// remove out-of-date entries that are still left around in the destination evernt map
		for (Entry<SyncedCalendar, Map<String, Event>> entry : destCalEventMap.entrySet()) {
			SyncedCalendar destCal = entry.getKey();
			Map<String, Event> destEvents = entry.getValue();

			for (Event event : destEvents.values()) {
				System.out.println("Removing event '" + event.getSummary() + "' from calendar: " + destCal.getName());
				readWriteCalendarService.events().delete(destCal.getGoogleId(), event.getId()).execute();
			}
		}
	}

	private void syncSourceCalendar(Calendar readWriteService, List<KeywordCategory> keywordCategories,
			SyncedCalendar sourceCal, Collection<Event> sourceEvents, List<SyncedCalendar> destCals,
			Map<SyncedCalendar, Map<String, Event>> destCalEventMap, Map<Integer, Organization> orgIdMap)
			throws IOException {

		Set<Category> categories = new HashSet<>();
		for (Event event : sourceEvents) {
			// check on event organization
			Organization sourceOrg = sourceCal.getOrganization();
			int sourceOrgId = sourceOrg.getId();
			sourceOrg = orgIdMap.get(sourceOrgId);
			if (sourceOrg == null) {
				System.err.println("Unknown org id: " + sourceOrgId + " in calendar: " + sourceCal.getName());
				continue;
			}

			String description = event.getDescription();
			if (description != null) {
				description = description.trim();
			}

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
					// NOTE: we make assumptions that there is whitespace around this word already
					sb.append(description, index + keyword.length(), description.length());
					categories.add(keyCat.getCategory());
					event.setDescription(sb.toString().trim());
					hasCategory = true;
				}
			}

			// enforce our require-category boolean
			if (sourceCal.isRequireCategory() && !hasCategory) {
				continue;
			}

			StringBuilder sb = new StringBuilder();
			String normalDescription = event.getDescription();
			if (normalDescription != null) {
				sb.append(normalDescription).append("\n\n");
			}
			sb.append("From calendar: ").append(sourceOrg.getName()).append(".");
			String orgDescription = sb.toString();

			// for each dest-cal which matches org or category, see if it has the event
			for (SyncedCalendar destCal : destCals) {
				// System.out.println(" Copying from '" + sourceCal + "' to destCal: " + destCal);
				Map<String, Event> destEvents = destCalEventMap.get(destCal);

				Category destCat = destCal.getCategory();
				if (destCat != null && !categories.contains(destCat)) {
					continue;
				}

				/*
				 * If our destination-org is not set or is different from the source-calendar organization then we need
				 * to append the source org to the description.
				 */
				Organization destOrg = destCal.getOrganization();
				if (destOrg == null || !destOrg.equals(sourceOrg)) {
					event.setDescription(orgDescription);
				} else {
					event.setDescription(normalDescription);
				}

				/*
				 * Now remove the event from destination calendar list to see if it exists which will leave the ones
				 * that we need to remove later.
				 */
				Event destEvent = destEvents.remove(event.getId());
				if (destEvent == null) {
					destEvent = new Event();
					assignEventFields(destEvent, event);
					System.out
							.println("Adding event '" + destEvent.getSummary() + "' to calendar: " + destCal.getName());
					readWriteService.events().insert(destCal.getGoogleId(), destEvent).execute();
				} else if (eventEquals(event, destEvent)) {
					// no changes need to be made to the event
				} else {
					// need to update this event
					System.out.println("Updating event '" + event.getSummary() + "' in calendar: " + destCal.getName());
					assignEventFields(destEvent, event);
					readWriteService.events().update(destCal.getGoogleId(), destEvent.getId(), destEvent).execute();
				}
				// NOTE: deletes are done at the end outside of this method
			}
		}
	}

	private void assignEventFields(Event destEvent, Event sourceEvent) {
		// NOTE: id might be null if it is new
		destEvent.setId(sourceEvent.getId());
		destEvent.setSummary(sourceEvent.getSummary());
		destEvent.setStart(sourceEvent.getStart());
		destEvent.setEnd(sourceEvent.getEnd());
		destEvent.setLocation(sourceEvent.getLocation());
		destEvent.setDescription(sourceEvent.getDescription());
		destEvent.setAttachments(sourceEvent.getAttachments());
	}

	private boolean eventEquals(Event sourceEvent, Event destEvent) {
		/*
		 * NOTE: we don't compare id here because it is going to be assigned and different. Also we don't compare
		 * htmlLink because it too will be assigned.
		 */
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
					// is this necessary?
					.list("primary")
					// TODO: need to test next-page-token
					.setPageToken(nextPageToken)
					// is this right?
					.setSingleEvents(true)
					.setCalendarId(calendar.getGoogleId())
					.execute();
			List<Event> eventList = events.getItems();
			for (Event event : eventList) {
				eventMap.put(event.getId(), event);
			}
			nextPageToken = events.getNextPageToken();
		} while (nextPageToken != null);
		System.out.println("Loaded " + eventMap.size() + " events from " + calendar.getName());
		return eventMap;
	}
}
