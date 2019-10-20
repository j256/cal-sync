package com.j256.calsync.data;

import com.google.api.services.calendar.Calendar;

/**
 * Calendar that we are reading events from.
 */
public class SourceCal {

	private String calendarId;
	// unique calendar-name that is added to the copied event to identify the source
	private String calendarName;
	// type of the calendar if it is specific to a certain calendar group or null if misc or per-event type
	private String defaultCategory;
	private String organization;

	// contactEmail, contactPhone, lastUpdate, description

	// associated service
	private transient Calendar service;

	public SourceCal() {
		// for ormlite
	}

	public SourceCal(String calendarId, String calendarName, String defaultCategory, String organization) {
		this.calendarId = calendarId;
		this.calendarName = calendarName;
		this.defaultCategory = defaultCategory;
		this.organization = organization;
	}

	public String getCalendarId() {
		return calendarId;
	}

	public String getCalendarName() {
		return calendarName;
	}

	public String getDefaultCategory() {
		return defaultCategory;
	}

	public String getOrganization() {
		return organization;
	}

	public void setService(Calendar service) {
		this.service = service;
	}

	public Calendar getService() {
		return service;
	}
}
