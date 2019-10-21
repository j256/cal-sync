package com.j256.calsync.dao;

import java.sql.SQLException;

import com.j256.calsync.data.SyncPath;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

/**
 * DAO implementation for our SyncedCalendar entity.
 */
public class SyncPathDaoImpl extends BaseDaoImpl<SyncPath, Void> implements SyncPathDao {

	public SyncPathDaoImpl(ConnectionSource connectionSource) throws SQLException {
		super(connectionSource, SyncPath.class);
	}
}
