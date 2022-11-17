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

import com.actelion.research.util.ByteArrayComparator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.TreeMap;

public class AlphaNumTable implements ConfigurationKeys {
	private String mTableLongName, mTableAliasName;
	private String[] mColumnTitle;
	private String[] mColumnName;
	private ForeignKey[] mForeignKey;
	private int[] mColumnType;
	private int mPrimaryKeyColumn,mForeignKeyCount;
	private ArrayList<AlphaNumRow> mRowList;
	private TreeMap<byte[],AlphaNumRow> mRowMap;

	/**
	 * Parses specification and sets up columns and properties
	 * @param tableDef
	 * @return
	 */
	public boolean initialize(String tableDef) {
		if (tableDef == null || tableDef.length() == 0)
			return false;

		String[] entry = tableDef.split(",");
		for (int i=0; i<entry.length; i++)
			entry[i] = entry[i].trim();

		int index = entry[0].indexOf(' ');
		if (index == -1) {
			System.out.println("Missing table short name in: " + entry[0]);
			return false;
		}
		mTableLongName = entry[0].substring(0, index);
		mTableAliasName = entry[0].substring(index+1);
		if (mTableLongName.indexOf('.') == -1) {
			System.out.println("Missing database name as part of table name: " + mTableLongName);
			return false;
		}

		int columnCount = entry.length / 2;

		mPrimaryKeyColumn = -1;
		mColumnTitle = new String[columnCount];
		mColumnName = new String[columnCount];
		mColumnType = new int[columnCount];

		mForeignKeyCount = 0;
		for (int i=1; i<entry.length-1; i+=2)
			if (entry[i].startsWith(COLUMN_TYPES[COLUMN_TYPE_FK]))
				mForeignKeyCount++;

		mForeignKey = new ForeignKey[mForeignKeyCount];

		int currentForeignKeyIndex = 0;
		int currentColumnIndex = mForeignKeyCount;

		// Foreign key column come first!
		for (int i=1; i<entry.length-1; i+=2) {
			if (entry[i].charAt(0) != '[') {
				System.out.println("No column type defined: " + entry[i]);
				return false;
			}
			index = entry[i].indexOf(']');
			if (index == -1) {
				System.out.println("Invalid column type: " + entry[i]);
				return false;
			}

			boolean isForeignKey = entry[i].startsWith(COLUMN_TYPES[COLUMN_TYPE_FK]);
			int columnIndex;
			if (isForeignKey)
				columnIndex = currentForeignKeyIndex++;
			else
				columnIndex = currentColumnIndex++;

			String typeString = entry[i].substring(0, index+1);
			mColumnType[columnIndex] = -1;
			for (int type=0; type<COLUMN_TYPES.length; type++) {
				String code = COLUMN_TYPES[type];
				if (typeString.startsWith(code)) {
					mColumnType[columnIndex] = type;
					if (type == COLUMN_TYPE_FK)
						mForeignKey[columnIndex] = new ForeignKey(typeString);
					break;
				}
			}
			if (mColumnType[columnIndex] == -1) {
				System.out.println("Incorrect column type: "+entry[i]);
				return false;
			}

			if (mColumnType[columnIndex] == COLUMN_TYPE_PK)
				mPrimaryKeyColumn = columnIndex;

			mColumnTitle[columnIndex] = entry[i].substring(index+1).trim();
			mColumnName[columnIndex] = entry[i+1].trim();
		}

		if (mPrimaryKeyColumn == -1) {
			System.out.println("No primary key found in table: "+mTableLongName);
			return false;
		}

		return true;
	}

	public boolean validateForeignKeys(AlphaNumTable[] allTables) {
		for (ForeignKey fk: mForeignKey)
			if (fk != null)
				if (!fk.validate(allTables))
					return false;

		return true;
	}

	public int getColumnIndex(String columnName) {
		for (int column=0; column<mColumnName.length; column++)
			if (columnName.equals(mColumnName[column]))
				return column;

		return -1;
	}

	/**
	 * @return table name as '<database_name>.<table_name>'
	 */
	public String getLongName() {
		return mTableLongName;
	}

	/**
	 * @return undecorated database table name
	 */
	public String getName() {
		int index = mTableLongName.indexOf('.');
		return mTableLongName.substring(index+1);
	}

	public int getForeignKeyIndex(String tableAlias) {
		for (int i=0; i<mForeignKeyCount; i++)
			if (tableAlias.equals(mForeignKey[i].getReferencedTable().getAliasName()))
				return i;

		return -1;
	}

	public String getAliasName() {
		return mTableAliasName;
	}

	public String getColumnName(int i) {
		return mColumnName[i];
	}

	public String getColumnTitle(int i) {
		return mColumnTitle[i];
	}

	public int getColumnType(int i) {
		return mColumnType[i];
	}

	public int getRowCount() {
		return mRowList.size();
	}

	public AlphaNumRow getRow(byte[] primaryKey) {
		return mRowMap.get(primaryKey);
	}

	public ArrayList<AlphaNumRow> getRowList() {
		return mRowList;
	}

	public int getColumnCount() {
		return mColumnType.length;
	}

	public int getPrimaryKeyColumn() {
		return mPrimaryKeyColumn;
	}

	public ForeignKey[] getForeignKeys() {
		return mForeignKey;
	}

	public void addTableCreationSQL(StringBuilder script, AlphaNumTable[] allTables) {
		script.append("CREATE TABLE ");
		script.append(getName());
		script.append(" (\n");
		for (int column=0; column<mColumnType.length; column++) {
			script.append("    ");
			script.append(mColumnName[column]);
			script.append(" ");
			script.append(SQL_TYPE[mColumnType[column]]);
			script.append(",\n");
		}
		addTableCreationSQLStructureColumns(script);
		script.append("    PRIMARY KEY (");
		script.append(getColumnName(mPrimaryKeyColumn));
		script.append("),\n");
		for (int column=0; column<mColumnType.length; column++) {
			if (mColumnType[column] == COLUMN_TYPE_FK) {
				AlphaNumTable foreignTable = mForeignKey[column].getReferencedTable();
				script.append("    FOREIGN KEY (");
				script.append(getColumnName(column));
				script.append(") REFERENCES ");
				script.append(foreignTable.getName());
				script.append("(");
				script.append(foreignTable.getColumnName(foreignTable.getPrimaryKeyColumn()));
				script.append(")");
				script.append(",\n");
			}
		}

		// get rid of last line's comma
		script.setLength(script.length()-2);
		script.append("\n");
		script.append(");\n");
	}

	private String buildSelectSQL() {
		StringBuilder sql = new StringBuilder("SELECT");

		for (int i = 0; i<mColumnName.length; i++) {
			sql.append(i == 0 ? " " : ",");
			sql.append(mColumnName[i]);
			}

		addOptionalTableNamesToSQL(sql);

		sql.append(" FROM ");
		sql.append(mTableLongName);

		return sql.toString();
	}

	protected void addOptionalTableNamesToSQL(StringBuilder sql) {
	}

	protected void addTableCreationSQLStructureColumns(StringBuilder script) {}

	public boolean loadData(DatabaseConnector connector) {
		if (!connector.ensureConnection())
			return false;

//		String time = getString(DATE_SQL);
//		if (time == null)
//			return false;

		try {
			mRowList = new ArrayList<>();

			Statement stmt = connector.getConnection().createStatement();
			ResultSet rset = stmt.executeQuery(buildSelectSQL());

			while (rset.next())
				mRowList.add(createRow(rset));

			rset.close();
			stmt.close();

//			mRecentUpdate = time;
			}
		catch (SQLException e) {
			System.out.println("Exception when reading table data: "+ e.getMessage());
			return false;
		}

		mRowMap = new TreeMap<>(new ByteArrayComparator());
		for (AlphaNumRow row:mRowList)
			mRowMap.put(row.getData(mPrimaryKeyColumn), row);

		System.out.println("Loaded "+mRowList.size()+" rows from "+getName());

		return true;
		}

	public boolean buildForeignKeyReferences() {
		for (AlphaNumRow row : mRowList) {
			AlphaNumRow[] referencedRows = new AlphaNumRow[mForeignKeyCount];
			for (int column = 0; column<mForeignKeyCount; column++) {
				byte[] fk = row.getData(column);
				if (fk != null) {
					referencedRows[column] = mForeignKey[column].getReferencedTable().getRow(fk);
					if (referencedRows[column] == null) {
						System.out.println("Could not find primary key '"+new String(fk)+"' in table '"+ mForeignKey[column].getReferencedTable().getName()+"'");
						return false;
					}
				}
			}
			row.setReferencedRows(referencedRows);
		}
		return true;
	}

	public AlphaNumRow getRow(int i) {
		return mRowList.get(i);
	}

	protected AlphaNumRow createRow(ResultSet rset) throws SQLException {
		AlphaNumRow row = new AlphaNumRow(getColumnCount());

		for (int column=0; column<getColumnCount(); column++) {
			String s = rset.getString(column+1);
			if (s != null) {
				row.setData(column, s.getBytes());
				if (mColumnType[column] == COLUMN_TYPE_NUM) {
					try {
						row.setFloat(Float.parseFloat(s), column);
					}
					catch (NumberFormatException nfe) {}
				}
			}
		}

		return row;
	}
}