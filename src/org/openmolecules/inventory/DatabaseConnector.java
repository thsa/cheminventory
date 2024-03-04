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
	private static final long CONNECTION_CHECK_DELAY = 60000;

	private static boolean sDriverRegistered;
	private static String sConnectString;
	private static DatabaseConnector sInstance;

	private Connection mConnection;
	private final String mUser,mPassword;
	private long mLastKnownValidConnection;

	public static void setConnectString(String connectString) {
		sConnectString = connectString;
		if (sConnectString.startsWith("jdbc:"))
			sConnectString = sConnectString.substring(5);
	}

	public static boolean isAuthorized(String user, String password) {
		if (sConnectString != null && sDriverRegistered) {
			try {
				Connection c = DriverManager.getConnection("jdbc:"+sConnectString, user, password);
				c.close();
				return true;
			}
			catch (Exception e) {}
		}
		return false;
	}

	public static DatabaseConnector getInstance(String user, String password) {
		if (sInstance == null)
			sInstance = new DatabaseConnector(user, password);

		return sInstance;
	}

	public static DatabaseConnector getInstance() {
		return sInstance;
	}

	private DatabaseConnector(String user, String password) {
		mUser = user;
		mPassword = password;
	}

	public boolean ensureConnection() {
		try {
			if (mConnection != null) {
				long now = System.currentTimeMillis();
				if (mLastKnownValidConnection< now - CONNECTION_CHECK_DELAY) {
					if (mConnection.isValid(5))
						mLastKnownValidConnection = now;
					else
						mConnection = null;
					}
				}
			}
		catch (SQLException sqle) {
			mConnection = null;
			}

		if (mConnection == null) {
			if (sConnectString.startsWith("mysql:")) {
				try {
					if (registerMySQLDriver()) {
						mConnection = DriverManager.getConnection("jdbc:" + sConnectString, mUser, mPassword);
						mLastKnownValidConnection = System.currentTimeMillis();
					}
				}
				catch (Exception e) {
					System.out.println("Exception when connecting to MySQL database: "+ e.getMessage());
				}
			}

			if (sConnectString.startsWith("postgresql:")) {
				try {
					if (registerPostgreSQLDriver()) {
						mConnection = DriverManager.getConnection("jdbc:" + sConnectString, mUser, mPassword);
						mLastKnownValidConnection = System.currentTimeMillis();
					}
				}
				catch (Exception e) {
					System.out.println("Exception when connecting to PostgreSQL database: "+ e.getMessage());
				}
			}
		}
		return mConnection != null;
	}

	private boolean registerMySQLDriver() {
		sDriverRegistered = true;
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
		if (!sDriverRegistered) {
			try {
				Class.forName("org.postgresql.Driver").newInstance();
				sDriverRegistered = true;
			}
			catch (Exception e) {
				System.out.println("Exception when registering PostgreSQL driver: "+ e.getMessage());
			}
		}
		return sDriverRegistered;
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
