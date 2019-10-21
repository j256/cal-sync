package com.j256.calsync.dao;

import java.sql.SQLException;

import com.j256.calsync.data.SyncedCalendar;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

/**
 * DAO implementation for our SyncedCalendar entity.
 */
public class SyncedCalendarDaoImpl extends BaseDaoImpl<SyncedCalendar, Integer> implements SyncedCalendarDao {

	public SyncedCalendarDaoImpl(ConnectionSource connectionSource) throws SQLException {
		super(connectionSource, SyncedCalendar.class);
	}
}
