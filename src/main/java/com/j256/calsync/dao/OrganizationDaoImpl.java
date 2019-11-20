package com.j256.calsync.dao;

import java.sql.SQLException;

import com.j256.calsync.data.Organization;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

/**
 * DAO implementation for our Organization entity.
 */
public class OrganizationDaoImpl extends BaseDaoImpl<Organization, Integer> implements OrganizationDao {

	public OrganizationDaoImpl(ConnectionSource connectionSource) throws SQLException {
		super(connectionSource, Organization.class);
	}
}
