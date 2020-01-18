package com.j256.calsync.dao;

import java.sql.SQLException;

import com.j256.calsync.data.Color;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

/**
 * DAO implementation for our Color entity.
 */
public class ColorDaoImpl extends BaseDaoImpl<Color, Integer> implements ColorDao {

	public ColorDaoImpl(ConnectionSource connectionSource) throws SQLException {
		super(connectionSource, Color.class);
	}
}
