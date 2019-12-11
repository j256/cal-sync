package com.j256.calsync.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Path that goes from one calendar to another.
 */
@DatabaseTable
public class SyncPath extends BaseGeneratedIdEntity {

	public static final String FIELD_SOURCE_CAL = "sourceCalendar_id";
	public static final String FIELD_DEST_CAL = "destCalendar_id";

	@DatabaseField(columnName = FIELD_SOURCE_CAL, foreign = true)
	private SyncedCalendar sourceCalendar;
	@DatabaseField(columnName = FIELD_DEST_CAL, foreign = true)
	private SyncedCalendar destCalendar;

	public SyncPath() {
		// for ormlite
	}

	public SyncPath(SyncedCalendar sourceCalendar, SyncedCalendar destCalendar) {
		this.sourceCalendar = sourceCalendar;
		this.destCalendar = destCalendar;
	}

	public SyncedCalendar getSourceCalendar() {
		return sourceCalendar;
	}

	public void setSourceCalendar(SyncedCalendar sourceCalendar) {
		this.sourceCalendar = sourceCalendar;
	}

	public SyncedCalendar getDestCalendar() {
		return destCalendar;
	}

	public void setDestCalendar(SyncedCalendar destCalendar) {
		this.destCalendar = destCalendar;
	}

	@Override
	public String toString() {
		return "SyncPath [src=" + sourceCalendar.getName() + ", dest=" + destCalendar.getName() + "]";
	}
}
