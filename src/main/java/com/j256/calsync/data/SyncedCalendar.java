package com.j256.calsync.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Calendar that we are reading events from.
 */
@DatabaseTable
public class SyncedCalendar extends BaseGeneratedIdEntity {

	// google calendar-id from google.com calendar settings page
	@DatabaseField(unique = true)
	private String googleId;
	@DatabaseField(unique = true)
	private String name;
	// type of the calendar if it is specific to a certain calendar group or null if misc or per-event type
	@DatabaseField(foreign = true, foreignAutoRefresh =  true)
	private Category category;
	// organization that if specified (and the category is null) will get all organization events
	@DatabaseField(foreign = true, foreignAutoRefresh =  true)
	private Organization organization;
	// whether or not a category is required before the entry will be copied into another calendar
	@DatabaseField
	private boolean requireCategory;
	// whether this is a source calendar versus a destination calendar
	@DatabaseField
	private boolean source;

	// contactEmail, contactPhone, lastUpdate

	public SyncedCalendar() {
		// for ormlite
	}

	public SyncedCalendar(String googleId, String name, Category category, Organization organization,
			boolean requireCategory, boolean source) {
		this.googleId = googleId;
		this.name = name;
		this.category = category;
		this.organization = organization;
		this.requireCategory = requireCategory;
		this.source = source;
	}

	public String getGoogleId() {
		return googleId;
	}

	public String getName() {
		return name;
	}

	public Category getCategory() {
		return category;
	}
	
	public void setCategory(Category category) {
		this.category = category;
	}

	public Organization getOrganization() {
		return organization;
	}
	
	public void setOrganization(Organization organization) {
		this.organization = organization;
	}

	public boolean isRequireCategory() {
		return requireCategory;
	}

	public boolean isSource() {
		return source;
	}

	@Override
	public String toString() {
		return "SyncedCalendar [name=" + name + "]";
	}
}
