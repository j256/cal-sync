package com.j256.calsync.data;

import com.google.api.services.calendar.Calendar;

/**
 * Calendar that we are reading events from.
 */
public class DestCal {

	private String calendarId;
	private String calendarName;
	// type of the calendar if it is specific to a certain calendar group or null if misc or per-event type
	private String category;
	// organization that if specified (and the category is null) will get all organization events
	private String organization;

	// associated service
	private transient Calendar service;

	// contactEmail, contactPhone, lastUpdate

	public DestCal() {
		// for ormlite
	}

	public DestCal(String calendarId, String calendarName, String category, String organization) {
		this.calendarId = calendarId;
		this.calendarName = calendarName;
		this.category = category;
		this.organization = organization;
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

	public void setService(Calendar service) {
		this.service = service;
	}

	public Calendar getService() {
		return service;
	}
}
