package com.j256.calsync.dao;

import java.sql.SQLException;

import com.j256.calsync.data.KeywordCategory;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

/**
 * DAO implementation for our KeywordCategory entity.
 */
public class KeywordCategoryDaoImpl extends BaseDaoImpl<KeywordCategory, Void> implements KeywordCategoryDao {

	public KeywordCategoryDaoImpl(ConnectionSource connectionSource) throws SQLException {
		super(connectionSource, KeywordCategory.class);
	}
}
