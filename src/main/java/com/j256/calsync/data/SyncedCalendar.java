package com.j256.calsync.data;

/**
 * Calendar that we are reading events from.
 */
public class SyncedCalendar {

	private String calendarId;
	private String calendarName;
	// type of the calendar if it is specific to a certain calendar group or null if misc or per-event type
	private String category;
	// organization that if specified (and the category is null) will get all organization events
	private String organization;
	// whether or not a calegory is required before the entry will be copied into another calendar
	private boolean requireCategory;

	// contactEmail, contactPhone, lastUpdate

	public SyncedCalendar() {
		// for ormlite
	}

	public SyncedCalendar(String calendarId, String calendarName, String category, String organization,
			boolean requireCategory) {
		this.calendarId = calendarId;
		this.calendarName = calendarName;
		this.category = category;
		this.organization = organization;
		this.requireCategory = requireCategory;
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
