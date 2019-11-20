package com.j256.calsync.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Organization for calendars.
 */
@DatabaseTable
public class Organization extends BaseGeneratedIdEntity {

	@DatabaseField
	private String name;

	public Organization() {
		// for ormlite
	}

	public Organization(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
