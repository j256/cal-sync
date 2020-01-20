package com.j256.calsync.dao;

import java.sql.SQLException;

import com.j256.calsync.data.IgnoredEvent;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

/**
 * DAO implementation for our IgnoredEvent entity.
 */
public class IgnoredEventDaoImpl extends BaseDaoImpl<IgnoredEvent, Integer> implements IgnoredEventDao {

	public IgnoredEventDaoImpl(ConnectionSource connectionSource) throws SQLException {
		super(connectionSource, IgnoredEvent.class);
	}
}
