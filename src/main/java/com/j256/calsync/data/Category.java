package com.j256.calsync.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Category for calendars.
 */
@DatabaseTable
public class Category extends BaseGeneratedIdEntity {

	@DatabaseField
	private String name;

	public Category() {
		// for ormlite
	}

	public Category(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
