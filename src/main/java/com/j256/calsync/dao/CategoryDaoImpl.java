package com.j256.calsync.dao;

import java.sql.SQLException;

import com.j256.calsync.data.Category;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

/**
 * DAO implementation for our Category entity.
 */
public class CategoryDaoImpl extends BaseDaoImpl<Category, Integer> implements CategoryDao {

	public CategoryDaoImpl(ConnectionSource connectionSource) throws SQLException {
		super(connectionSource, Category.class);
	}
}
