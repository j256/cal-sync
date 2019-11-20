package com.j256.calsync.data;

import com.j256.ormlite.field.DatabaseField;

/**
 * Base class for generated id elements.
 */
public abstract class BaseGeneratedIdEntity {

	@DatabaseField(generatedId = true)
	protected int id;

	public BaseGeneratedIdEntity() {
		// for ormlite
	}

	public int getId() {
		return id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		BaseGeneratedIdEntity other = (BaseGeneratedIdEntity) obj;
		return (id == other.id);
	}
}
