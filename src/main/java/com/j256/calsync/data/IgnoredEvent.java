package com.j256.calsync.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Organization for calendars.
 */
@DatabaseTable
public class IgnoredEvent extends BaseGeneratedIdEntity {

	@DatabaseField
	private String eventId;
	@DatabaseField
	private String reason;

	public IgnoredEvent() {
		// for ormlite
	}

	public String getEventId() {
		return eventId;
	}

	public String getReason() {
		return reason;
	}

	@Override
	public String toString() {
		return "IgnoredEvent [eventId=" + eventId + "]";
	}
}
