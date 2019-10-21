package com.j256.calsync.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Calendar that we are reading events from.
 */
@DatabaseTable
public class SyncedCalendar {

	@DatabaseField(generatedId = true)
	private int id;
	@DatabaseField(unique = true)
	private String calendarId;
	@DatabaseField(unique = true)
	private String calendarName;
	// type of the calendar if it is specific to a certain calendar group or null if misc or per-event type
	@DatabaseField
	private String category;
	// organization that if specified (and the category is null) will get all organization events
	@DatabaseField
	private String organization;
	// whether or not a category is required before the entry will be copied into another calendar
	@DatabaseField
	private boolean requireCategory;
	// whether or not a category is required before the entry will be copied into another calendar
	@DatabaseField
	private boolean source;

	// contactEmail, contactPhone, lastUpdate

	public SyncedCalendar() {
		// for ormlite
	}

	public SyncedCalendar(String calendarId, String calendarName, String category, String organization,
			boolean requireCategory, boolean source) {
		this.calendarId = calendarId;
		this.calendarName = calendarName;
		this.category = category;
		this.organization = organization;
		this.requireCategory = requireCategory;
		this.source = source;
	}

	public int getId() {
		return id;
	}

	public String getCalendarId() {
		return calendarId;
	}

	public String getCalendarName() {
		return calendarName;
	}

	public String getCategory() {
		return category;
	}

	public String getOrganization() {
		return organization;
	}

	public boolean isRequireCategory() {
		return requireCategory;
	}

	public boolean isSource() {
		return source;
	}

	@Override
	public int hashCode() {
		return calendarId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return calendarId.equals(((SyncedCalendar) obj).calendarId);
	}

	@Override
	public String toString() {
		return "DestCal [calendarName=" + calendarName + "]";
	}
}
