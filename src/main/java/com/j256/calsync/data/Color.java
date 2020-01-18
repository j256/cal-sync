package com.j256.calsync.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Category for calendars.
 */
@DatabaseTable
public class Color extends BaseGeneratedIdEntity {

	@DatabaseField
	private String name;

	public Color() {
		// for ormlite
	}

	public Color(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Color [name=" + name + "]";
	}
}
