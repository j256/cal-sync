package com.j256.calsync;

import java.io.IOException;
import java.io.InputStream;
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

import org.joda.time.Period;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Event.ExtendedProperties;
import com.google.api.services.calendar.model.EventAttachment;
import com.google.api.services.calendar.model.Events;
import com.j256.calsync.dao.IgnoredEventDao;
import com.j256.calsync.dao.IgnoredEventDaoImpl;
import com.j256.calsync.dao.KeywordCategoryDao;
import com.j256.calsync.dao.KeywordCategoryDaoImpl;
import com.j256.calsync.dao.SyncPathDao;
import com.j256.calsync.dao.SyncPathDaoImpl;
import com.j256.calsync.dao.SyncedCalendarDao;
import com.j256.calsync.dao.SyncedCalendarDaoImpl;
import com.j256.calsync.data.Category;
import com.j256.calsync.data.IgnoredEvent;
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

	private static final String FROM_CALENDAR_PREFIX = "<p>From calendar: ";
	private static final String FROM_CALENDAR_SUFFIX = "</p>";
	private static final String APPLICATION_NAME = "Calendar Sync";
	private static final boolean CUT_KEYWORD_FROM_DESCRIPTION = false;
	private static final boolean MAKE_CHANGES = true;

	private static final List<String> READ_ONLY_SCOPE = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
	private static final List<String> READ_WRITE_SCOPE = Collections.singletonList(CalendarScopes.CALENDAR);

	private static final long MAX_IN_PAST_MILLIS = Period.days(90).toStandardDuration().getMillis();
	private static final long MAX_IN_FUTURE_MILLIS = Period.days(90).toStandardDuration().getMillis();

	private static final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

	public static void main(String... args) throws Exception {
		if (args.length != 4) {
			System.err.println("Usage: java -jar XXX.jar sql-url username password cred-path");
			System.exit(1);
		}
		String sqlUrl = args[0];
		String sqlUsername = args[1];
		String sqlPassword = args[2];
		String credentialsFilePath = args[3];
		new CalSyncMain().doMain(sqlUrl, sqlUsername, sqlPassword, credentialsFilePath);
	}

	private void doMain(String sqlUrl, String sqlUsername, String sqlPassword, String credentialsFilePath)
			throws Exception {

		final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

		InputStream in = CalSyncMain.class.getResourceAsStream(credentialsFilePath);
		if (in == null) {
			System.err.println("creds file not found: " + credentialsFilePath);
			System.exit(1);
			return;
		}
		GoogleCredential readOnlyCredential = GoogleCredential.fromStream(in).createScoped(READ_ONLY_SCOPE);
		in = CalSyncMain.class.getResourceAsStream(credentialsFilePath);
		GoogleCredential readWriteCredential = GoogleCredential.fromStream(in).createScoped(READ_WRITE_SCOPE);

		// build a new authorized API client service
		Calendar readWriteCalendarService = new Calendar.Builder(httpTransport, jsonFactory, readWriteCredential)
				.setApplicationName(APPLICATION_NAME)
				.build();
		Calendar readOnlyCalendarService = new Calendar.Builder(httpTransport, jsonFactory, readOnlyCredential)
				.setApplicationName(APPLICATION_NAME)
				.build();

		// create our database objects
		JdbcConnectionSource connectionSource = new JdbcConnectionSource(sqlUrl, sqlUsername, sqlPassword);

		KeywordCategoryDao keywordCategoryDao = new KeywordCategoryDaoImpl(connectionSource);
		SyncedCalendarDao syncedCalendarDao = new SyncedCalendarDaoImpl(connectionSource);
		SyncPathDao syncPathDao = new SyncPathDaoImpl(connectionSource);
		IgnoredEventDao ignoredEventDao = new IgnoredEventDaoImpl(connectionSource);

		List<SyncedCalendar> syncedCalendars = syncedCalendarDao.queryForAll();
		List<SyncPath> syncPaths = syncPathDao.queryForAll();
		List<KeywordCategory> keywordCategories = keywordCategoryDao.queryForAll();
		List<IgnoredEvent> ignoredEvents = ignoredEventDao.queryForAll();
		connectionSource.close();

		Map<Integer, SyncedCalendar> calIdMap = new HashMap<>();
		for (SyncedCalendar syncedCalendar : syncedCalendars) {
			calIdMap.put(syncedCalendar.getId(), syncedCalendar);
		}
		Set<String> ignoredEventIdSet = new HashSet<>();
		for (IgnoredEvent ignoredEvent : ignoredEvents) {
			ignoredEventIdSet.add(ignoredEvent.getEventId());
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

		System.out.println("Loading source calendars:");

		Map<SyncedCalendar, Collection<WrappedEvent>> sourceCalendarEntryMap = new HashMap<>();
		for (SyncedCalendar sourceCal : sourceCalToDestCalMap.keySet()) {
			Map<String, WrappedEvent> eventMap =
					loadCalendarEntries(readOnlyCalendarService, sourceCal, ignoredEventIdSet, true);
			sourceCalendarEntryMap.put(sourceCal, eventMap.values());
		}

		System.out.println("-------------------------------------------------------");
		System.out.println("Loading destination calendars:");

		Map<SyncedCalendar, Map<String, WrappedEvent>> destCalEventMap = new HashMap<>();
		// open each destination calendar and load all of its events into our map
		for (SyncedCalendar syncedCal : syncedCalendars) {
			if (syncedCal.isSource()) {
				continue;
			}
			Map<String, WrappedEvent> eventMap =
					loadCalendarEntries(readOnlyCalendarService, syncedCal, ignoredEventIdSet, false);
			destCalEventMap.put(syncedCal, eventMap);
		}
		System.out.println("-------------------------------------------------------");

		// open each source-cal calendar and sync it
		for (Entry<SyncedCalendar, List<SyncedCalendar>> entry : sourceCalToDestCalMap.entrySet()) {
			SyncedCalendar sourceCal = entry.getKey();
			List<SyncedCalendar> destCals = entry.getValue();
			System.out.println("Processing calendar: " + sourceCal.getName());
			// System.out.println(" destination calendars: " + destCals);
			Collection<WrappedEvent> events = sourceCalendarEntryMap.get(sourceCal);
			if (events == null) {
				System.err.println("Source-calendar has no entries in map: " + sourceCal);
				continue;
			}
			syncSourceCalendar(readWriteCalendarService, keywordCategories, sourceCal, events, destCals,
					destCalEventMap);
			System.out.println("-------------------------");
		}
		System.out.println("-------------------------------------------------------");

		// remove out-of-date entries that are still left around in the destination event map
		for (Entry<SyncedCalendar, Map<String, WrappedEvent>> entry : destCalEventMap.entrySet()) {
			SyncedCalendar destCal = entry.getKey();
			Map<String, WrappedEvent> destEvents = entry.getValue();

			for (WrappedEvent wrappedEvent : destEvents.values()) {
				System.out.println(
						"Removing event '" + wrappedEvent.getSummary() + "' from calendar: " + destCal.getName());
				if (MAKE_CHANGES) {
					readWriteCalendarService.events()
							.delete(destCal.getGoogleId(), wrappedEvent.getEventId())
							.execute();
				}
			}
		}
		System.out.println("Done.");
	}

	private void syncSourceCalendar(Calendar readWriteService, List<KeywordCategory> keywordCategories,
			SyncedCalendar sourceCal, Collection<WrappedEvent> sourceEvents, List<SyncedCalendar> destCals,
			Map<SyncedCalendar, Map<String, WrappedEvent>> destCalEventMap) throws IOException {

		Set<Category> categories = new HashSet<>();

		long maxStartTimeMillis = System.currentTimeMillis() + MAX_IN_FUTURE_MILLIS;

		for (WrappedEvent wrappedSourceEvent : sourceEvents) {

			// skip events with no start time or too far in the future
			if (wrappedSourceEvent.getStartTimeMillis() == 0) {
				// System.out.println("Skipping event with no start date-time");
				continue;
			} else if (wrappedSourceEvent.getStartTimeMillis() > maxStartTimeMillis) {
				System.out.println("Skipping event with bad start time: " + wrappedSourceEvent.getStartTimeMillis());
				continue;
			}
			// System.out.println("Got event with start time: " + startDateTme);

			// check on event organization
			Organization sourceOrg = sourceCal.getOrganization();

			String description = wrappedSourceEvent.getDescription();
			if (description != null) {
				description = description.trim();
			}
			if (description == null || description.isEmpty()) {
				// System.out.println("Skipping event with no description: " + event);
				continue;
			}
			if (description.contains(FROM_CALENDAR_PREFIX)) {
				System.out.println("Event was copied from another calendar: " + wrappedSourceEvent);
				continue;
			}

			// determine the event categories
			categories.clear();
			if (sourceCal.getCategory() != null) {
				// if there is a default category on the calendar then add it in
				categories.add(sourceCal.getCategory());
			}

			// look for category keywords
			boolean hasCategory = findCategories(keywordCategories, categories, wrappedSourceEvent, description);

			// enforce our require-category boolean
			if (sourceCal.isRequireCategory() && !hasCategory) {
				// System.out.println("Skipping event with no categories: " + event);
				continue;
			}

			// XXX: need to add event to the master list here maybe

			System.out.println("Event " + wrappedSourceEvent.getSummary() + " found category(s): " + categories);

			String normalizedDescription = normalizedDescription(description);

			StringBuilder sb = new StringBuilder();
			sb.append(normalizedDescription);
			if (!normalizedDescription.endsWith(">")) {
				sb.append(' ');
			}
			sb.append(FROM_CALENDAR_PREFIX).append(sourceOrg.getName()).append(".").append(FROM_CALENDAR_SUFFIX);
			String orgDescription = sb.toString();

			// for each dest-cal which matches org or category, see if it has the event
			for (SyncedCalendar destCal : destCals) {
				Map<String, WrappedEvent> destEvents = destCalEventMap.get(destCal);

				Category destCat = destCal.getCategory();
				if (destCat != null && !categories.contains(destCat)) {
					continue;
				}

				// see if this event overlaps one in the dest calendar
				WrappedEvent dup = findDuplicate(wrappedSourceEvent, destEvents.values());
				if (dup == wrappedSourceEvent) {
					continue;
				}

				/*
				 * If our destination-org is not set or is different from the source-calendar organization then we need
				 * to append the source org to the description.
				 */
				Organization destOrg = destCal.getOrganization();
				if (destOrg == null || !destOrg.equals(sourceOrg)) {
					wrappedSourceEvent.setDescription(orgDescription);
				} else {
					wrappedSourceEvent.setDescription(normalizedDescription);
				}
				if (sourceOrg.getColor() != null) {
					wrappedSourceEvent.getEvent().setColorId(Integer.toString(sourceOrg.getColor().getId()));
				}

				/*
				 * Now remove the event from destination calendar list. If it doesn't exist then we need to add it. If
				 * it does exist then we may need to update it. The ones that are left in the map need to be removed at
				 * the end.
				 */
				String syncId = wrappedSourceEvent.getSyncId();
				WrappedEvent destWrappedEvent = destEvents.remove(syncId);
				if (destWrappedEvent == null) {
					Event destEvent = new Event();
					assignEventFields(destEvent, wrappedSourceEvent.getEvent(), syncId);
					System.out.println("Adding event '" + destEvent.getSummary() + "' (id " + syncId + ") to calendar: "
							+ destCal.getName());
					if (MAKE_CHANGES) {
						readWriteService.events()
								.insert(destCal.getGoogleId(), destEvent)
								.setSupportsAttachments(true)
								.execute();
					}
				} else if (eventEquals(wrappedSourceEvent.getEvent(), destWrappedEvent.getEvent())) {
					// no changes need to be made to the event
					System.out.println(
							"No changes to '" + destWrappedEvent.getSummary() + "' in calendar: " + destCal.getName());
				} else {
					// need to update this event
					System.out.println("Updating event '" + wrappedSourceEvent.getSummary() + "' in calendar: "
							+ destCal.getName());
					assignEventFields(destWrappedEvent.getEvent(), wrappedSourceEvent.getEvent(), syncId);
					if (MAKE_CHANGES) {
						// we update by event-id
						readWriteService.events()
								.update(destCal.getGoogleId(), destWrappedEvent.getEventId(),
										destWrappedEvent.getEvent())
								.setSupportsAttachments(true)
								.execute();
					}
				}
				// NOTE: deletes are done at the end outside of this method
			}
		}
	}

	private WrappedEvent findDuplicate(WrappedEvent sourceWrappedEvent, Collection<WrappedEvent> destWrappedEvents) {
		// go through using n^2 logic (~1000 entries tops) to see if any are overlapping
		for (WrappedEvent destWrappedEvent : destWrappedEvents) {
			if (sourceWrappedEvent == destWrappedEvent
					|| sourceWrappedEvent.getSyncId().equals(destWrappedEvent.getSyncId())) {
				// same event so skip dup checking
				continue;
			}
			if (sourceWrappedEvent.isSimilar(destWrappedEvent)) {
				if (sourceWrappedEvent.compareTo(destWrappedEvent) >= 0) {
					System.out.println("Dest event is dup, scores s-" + sourceWrappedEvent.calcScore() + ",d-"
							+ destWrappedEvent.calcScore() + ": s-" + sourceWrappedEvent + ", d-" + destWrappedEvent);
					return destWrappedEvent;
				} else {
					System.out.println("Source event is dup, scores s-" + sourceWrappedEvent.calcScore() + ",d-"
							+ destWrappedEvent.calcScore() + ": s-" + sourceWrappedEvent + ", d-" + destWrappedEvent);
					return sourceWrappedEvent;
				}
			}
		}
		return null;
	}

	private String normalizedDescription(String desc) {
		// change all <br> into \n
		// remove any <span>\n</span>
		desc = desc.replace("<br>", "\n");
		desc = desc.replace("<br/>", "\n");
		desc = desc.replace("<span>\n</span>", "\n");
		desc = desc.replace("\n</p>", "</p>");
		desc = desc.replace("<span></span>", "");
		desc = desc.trim();
		StringBuilder sb = new StringBuilder();
		char prev = '\0';
		char prevPrev = '\0';
		for (char ch : desc.toCharArray()) {
			if (ch == '\r') {
				// remove all \r
				continue;
			} else if (ch == '\t') {
				// XXX convert \t to space, need to see if this looks good
				sb.append(' ');
			} else if (ch == '\n') {
				if (prevPrev == '\n' && prev == '\n') {
					// only take 2 \n's in a row
				} else {
					sb.append(ch);
				}
			} else {
				sb.append(ch);
			}
			prevPrev = prev;
			prev = ch;
		}
		String result = sb.toString();
		return result.trim();
	}

	private boolean findCategories(List<KeywordCategory> keywordCategories, Set<Category> categories,
			WrappedEvent wrappedEvent, String description) {
		if (description == null) {
			return false;
		}
		boolean hasCategory = false;
		for (KeywordCategory keyCat : keywordCategories) {
			String prefix = keyCat.getPrefix();
			int index = description.indexOf(prefix);
			if (index < 0) {
				continue;
			}
			// cut out the keyword itself out of the description
			if (CUT_KEYWORD_FROM_DESCRIPTION) {
				StringBuilder sb = new StringBuilder();
				sb.append(description, 0, index);
				// find the next whitespace character so that we support #lbgt*
				for (index += prefix.length(); index < description.length(); index++) {
					if (!Character.isAlphabetic(description.charAt(index))) {
						break;
					}
				}
				// NOTE: we make assumptions that there is whitespace around this word already
				if (index <= description.length()) {
					sb.append(description, index, description.length());
				}
				wrappedEvent.setDescription(sb.toString().trim());
			}
			categories.add(keyCat.getCategory());
			hasCategory = true;
		}
		return hasCategory;
	}

	private void assignEventFields(Event destEvent, Event sourceEvent, String syncId) {
		// NOTE: id is encoded into the Source below
		destEvent.setSummary(sourceEvent.getSummary());
		destEvent.setStart(sourceEvent.getStart());
		destEvent.setEnd(sourceEvent.getEnd());
		destEvent.setLocation(sourceEvent.getLocation());
		destEvent.setDescription(sourceEvent.getDescription());
		destEvent.setAttachments(sourceEvent.getAttachments());
		destEvent.setRecurrence(sourceEvent.getRecurrence());
		destEvent.setColorId(sourceEvent.getColorId());
		// we store the id of the source-event as an "extended property" so we can find it an update it later
		ExtendedProperties extendedProperties = new ExtendedProperties();
		extendedProperties.setShared(Collections.singletonMap(WrappedEvent.SOURCE_ID_PROPERTY_NAME, syncId));
		destEvent.setExtendedProperties(extendedProperties);
	}

	private boolean eventEquals(Event sourceEvent, Event destEvent) {
		/*
		 * NOTE: we don't compare id here because it is going to be assigned and different. Also we don't compare
		 * htmlLink because it too will be assigned.
		 */
		if (!Objects.equals(sourceEvent.getSummary(), destEvent.getSummary())) {
			System.out.println("  Summary different");
			return false;
		}
		if (!Objects.equals(sourceEvent.getStart(), destEvent.getStart())) {
			System.out.println("  Time-start different: " + sourceEvent.getStart() + " vs " + destEvent.getStart());
			return false;
		}
		if (!Objects.equals(sourceEvent.getEnd(), destEvent.getEnd())) {
			System.out.println("  Time-End different: " + sourceEvent.getEnd() + " vs " + destEvent.getEnd());
			return false;
		}
		if (!Objects.equals(sourceEvent.getLocation(), destEvent.getLocation())) {
			System.out.println("  Location different");
			return false;
		}
		if (!Objects.equals(sourceEvent.getDescription(), destEvent.getDescription())) {
			System.out.println("  Description different");
			return false;
		}
		if (!Objects.equals(sourceEvent.getRecurrence(), destEvent.getRecurrence())) {
			System.out.println("  Recurrence different");
			return false;
		}
		if (!Objects.equals(sourceEvent.getColorId(), destEvent.getColorId())) {
			System.out.println("  Color different: " + sourceEvent.getColorId() + " vs " + destEvent.getColorId());
			return false;
		}
		if (!attachmentsEquals(sourceEvent, destEvent)) {
			System.out.println("  Attachments different");
			return false;
		}
		return true;
	}

	private boolean attachmentsEquals(Event sourceEvent, Event destEvent) {
		// we have to do the attachments specifically
		List<EventAttachment> sourceAttachments = sourceEvent.getAttachments();
		List<EventAttachment> destAttachments = destEvent.getAttachments();
		if (sourceAttachments == null) {
			if (destAttachments == null) {
				return true;
			} else {
				System.out.println("  Dest attachments are not null, need to be cleared");
				return false;
			}
		} else if (destAttachments == null) {
			System.out.println("  Dest attachments should not be null, attachments need to be added");
			return false;
		} else if (sourceAttachments.size() != destAttachments.size()) {
			System.out.println("  Dest attachments size wrong");
			return false;
		}

		// test each attachment in turn
		for (int i = 0; i < sourceAttachments.size(); i++) {
			EventAttachment sourceAttachment = sourceAttachments.get(i);
			EventAttachment destAttachment = destAttachments.get(i);
			if (!Objects.equals(sourceAttachment.getFileUrl(), destAttachment.getFileUrl())) {
				System.out.println("  Attachment file-url different: " + sourceAttachment + " != " + destAttachment);
				return false;
			}
			// if (!Objects.equals(sourceAttachment.getIconLink(), destAttachment.getIconLink())) {
			// System.out.println(" Attachment icon-link different: " + sourceAttachment + " != " + destAttachment);
			// return false;
			// }
			if (!Objects.equals(sourceAttachment.getMimeType(), destAttachment.getMimeType())) {
				System.out.println("  Attachment mime-type different: " + sourceAttachment + " != " + destAttachment);
				return false;
			}
			if (!Objects.equals(sourceAttachment.getTitle(), destAttachment.getTitle())) {
				System.out.println("  Attachment title different: " + sourceAttachment + " != " + destAttachment);
				return false;
			}
			if (!Objects.equals(sourceAttachment.getFileId(), destAttachment.getFileId())) {
				System.out.println("  Attachment file-id different: " + sourceAttachment + " != " + destAttachment);
				return false;
			}
		}

		return true;
	}

	private Map<String, WrappedEvent> loadCalendarEntries(Calendar calendarService, SyncedCalendar calendar,
			Set<String> ignoredEventIdSet, boolean sourceCal) throws IOException {
		Map<String, WrappedEvent> eventMap = new HashMap<>();
		String nextPageToken = null;
		DateTime minDateTime = new DateTime(System.currentTimeMillis() - MAX_IN_PAST_MILLIS);
		DateTime maxDateTime = new DateTime(System.currentTimeMillis() + MAX_IN_FUTURE_MILLIS);
		do {
			Events events = calendarService.events()
					.list("primary")
					// could be null or could be to continue large request
					.setPageToken(nextPageToken)
					// set our min/max times to limit the query
					.setTimeMin(minDateTime)
					.setTimeMax(maxDateTime)
					// tried false here but it returns recurring events as multiples with the same id
					// we want repeating entries to be expanded so we can check overlaps with different repeat starts
					.setSingleEvents(true)
					.setCalendarId(calendar.getGoogleId())
					.execute();
			List<Event> eventList = events.getItems();
			for (Event event : eventList) {
				WrappedEvent wrappedEvent = new WrappedEvent(event);
				String syncId = wrappedEvent.getSyncId();
				// add it to our event map only if it's not in the ignored list
				if (calendar.isSource() && ignoredEventIdSet.contains(syncId)) {
					/*
					 * We ignored events from source calendars that are in our ignored list but we need to remove those
					 * entries from destination calendars.
					 */
					System.out.println("ignoring database event-id: " + syncId);
					continue;
				} else if (eventMap.containsKey(syncId)) {
					// ignore duplicates
					System.out.println("ignoring duplicate event-id: " + syncId);
					continue;
				} else if (sourceCal && (event.getSummary() == null || event.getDescription() == null)) {
					// skipping event with no summary or description
					continue;
				}
				String summary = event.getSummary().trim();
				String desc = event.getDescription();
				// convert html to newlines
				desc = desc.replace("<br>", "\n");
				desc = desc.replace("<br/>", "\n");
				desc = desc.replace("<span>\n</span>", "\n");
				desc = desc.trim();
				if (sourceCal && (summary.isEmpty() || desc.isEmpty())) {
					// skipping event with empty summary or description
					continue;
				}
				event.setSummary(summary);
				event.setDescription(desc);
				eventMap.put(syncId, wrappedEvent);
			}
			nextPageToken = events.getNextPageToken();
		} while (nextPageToken != null);
		System.out.println("Loaded " + eventMap.size() + " events from calendar " + calendar.getName());
		return eventMap;
	}
}
