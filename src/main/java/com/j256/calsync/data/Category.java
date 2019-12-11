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
	@DatabaseField
	private String description;

	public Category() {
		// for ormlite
	}

	public Category(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return "Category [name=" + name + "]";
	}
}
