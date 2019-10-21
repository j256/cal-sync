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
	private String googleId;
	@DatabaseField(unique = true)
	private String name;
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

	public SyncedCalendar(String googleId, String name, String category, String organization, boolean requireCategory,
			boolean source) {
		this.googleId = googleId;
		this.name = name;
		this.category = category;
		this.organization = organization;
		this.requireCategory = requireCategory;
		this.source = source;
	}

	public int getId() {
		return id;
	}

	public String getGoogleId() {
		return googleId;
	}

	public String getName() {
		return name;
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
		return googleId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.googleId.equals(((SyncedCalendar) obj).googleId);
	}

	@Override
	public String toString() {
		return "SyncedCalendar [name=" + name + "]";
	}
}
