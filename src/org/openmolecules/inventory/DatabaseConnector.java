/*
 * Copyright 2022, Thomas Sander, openmolecules.org
 *
 * This file is part of the Chemical-Inventory-Server.
 *
 * Chemical-Inventory-Server is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Chemical-Inventory-Server is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Chemical-Inventory-Server.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package org.openmolecules.inventory;

import java.sql.*;

public class DatabaseConnector {
	private Connection mConnection;
	private String mConnectString,mUser,mPassword;
	private boolean mDriverRegistered;
	private String mRecentUpdate;

	public DatabaseConnector(String connectString, String user, String password) {
		mConnectString = connectString;
		if (mConnectString.startsWith("jdbc:"))
			mConnectString = mConnectString.substring(5);
		mUser = user;
		mPassword = password;
	}

	public boolean ensureConnection() {
		try {
			if (mConnection != null
			 && mConnection.isClosed())
				mConnection = null;
			}
		catch (SQLException sqle) {
			mConnection = null;
			}

		if (mConnection == null) {
			if (mConnectString.startsWith("mysql:")) {
				try {
					if (registerMySQLDriver())
						mConnection = DriverManager.getConnection("jdbc:"+mConnectString, mUser, mPassword);
				}
				catch (Exception e) {
					System.out.println("Exception when connecting to MySQL database: "+ e.getMessage());
				}
			}

			if (mConnectString.startsWith("postgresql:")) {
				try {
					if (registerPostgreSQLDriver())
						mConnection = DriverManager.getConnection("jdbc:"+mConnectString, mUser, mPassword);
				}
				catch (Exception e) {
					System.out.println("Exception when connecting to PostgreSQL database: "+ e.getMessage());
				}
			}
		}
		return true;
	}

	private boolean registerMySQLDriver() {
		return true;    // no driver registration needed anymore
/*		if (!mDriverRegistered) {
			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				mDriverRegistered = true;
			}
			catch (Exception e) {
				System.out.println("Exception when registering MySQL driver: "+ e.getMessage());
			}
		}
		return mDriverRegistered;
*/	}

	private boolean registerPostgreSQLDriver() {
		if (!mDriverRegistered) {
			try {
				Class.forName("org.postgresql.Driver").newInstance();
				mDriverRegistered = true;
			}
			catch (Exception e) {
				System.out.println("Exception when registering PostgreSQL driver: "+ e.getMessage());
			}
		}
		return mDriverRegistered;
	}

	public Connection getConnection() {
		return mConnection;
	}

	public String getString(String sql) {
		if (!ensureConnection())
			return null;

		String result = null;
		Statement stmt = null;
		ResultSet rset = null;
		try {
			stmt = mConnection.createStatement();
			rset = stmt.executeQuery(sql);
			if (rset.next())
				result = rset.getString(1);
		}
		catch (Exception e) {
			System.out.println("Exception when executing SQL: "+ e.getMessage());
		}

		try {
			if (rset != null)
				rset.close();
			if (stmt != null)
				stmt.close();
		}
		catch (SQLException e) {}

		return result;
	}
}
