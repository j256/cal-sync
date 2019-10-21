package com.j256.calsync.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Path that goes from one calendar to another.
 */
@DatabaseTable
public class SyncPath {

	@DatabaseField(generatedId = true)
	private int id;
	@DatabaseField(foreign = true)
	private SyncedCalendar sourceCalendar;
	@DatabaseField(foreign = true)
	private SyncedCalendar destCalendar;

	public SyncPath() {
		// for ormlite
	}

	public SyncPath(SyncedCalendar sourceCalendar, SyncedCalendar destCalendar) {
		this.sourceCalendar = sourceCalendar;
		this.destCalendar = destCalendar;
	}

	public int getId() {
		return id;
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
}
