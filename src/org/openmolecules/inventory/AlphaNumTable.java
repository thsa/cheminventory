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

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.TreeMap;

public class AlphaNumTable implements ConfigurationKeys {
	private String mSpecification, mTableDisplayName, mTableLongName, mTableAliasName;
	private String[] mColumnTitle;
	private String[] mColumnName;
	private ForeignKey[] mForeignKey;
	private int[] mColumnType;
	private int mPrimaryKeyColumn,mIDColumn,mForeignKeyCount;
	private ArrayList<AlphaNumRow> mRowList;
	private TreeMap<byte[],AlphaNumRow> mPKToRowMap;
	private TreeMap<byte[],byte[]> mIDToPKMap;

	/**
	 * Parses specification and sets up columns and properties
	 * @param tableDef
	 * @return
	 */
	public boolean initialize(String tableDef) {
		if (tableDef == null || tableDef.isEmpty())
			return false;

		mSpecification = tableDef;

		String[] entry = tableDef.split(",");
		for (int i=0; i<entry.length; i++)
			entry[i] = entry[i].trim();

		int index = entry[1].indexOf(' ');
		if (index == -1) {
			System.out.println("Missing table short name in: " + entry[1]);
			return false;
		}
		mTableDisplayName = entry[0];
		mTableLongName = entry[1].substring(0, index);
		mTableAliasName = entry[1].substring(index+1);
		if (mTableLongName.indexOf('.') == -1) {
			System.out.println("Missing database name as part of table name: " + mTableLongName);
			return false;
		}

		int columnCount = entry.length / 2 - 1;

		mPrimaryKeyColumn = -1;
		mIDColumn = -1;
		mColumnTitle = new String[columnCount];
		mColumnName = new String[columnCount];
		mColumnType = new int[columnCount];

		mForeignKeyCount = 0;
		for (int i=2; i<entry.length; i+=2)
			if (entry[i].startsWith(COLUMN_TYPES[COLUMN_TYPE_FK]))
				mForeignKeyCount++;

		mForeignKey = new ForeignKey[mForeignKeyCount];

		int currentForeignKeyIndex = 0;
		int currentColumnIndex = mForeignKeyCount;

		// Foreign key column come first!
		for (int i=2; i<entry.length; i+=2) {
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
			if (!setColumnType(columnIndex, typeString)) {
				System.out.println("Incorrect column type: "+entry[i]);
				return false;
			}

			if (mColumnType[columnIndex] == COLUMN_TYPE_PK)
				mPrimaryKeyColumn = columnIndex;
			if (mColumnType[columnIndex] == COLUMN_TYPE_ID)
				mIDColumn = columnIndex;

			mColumnTitle[columnIndex] = entry[i].substring(index+1).trim();
			mColumnName[columnIndex] = entry[i+1].trim();
		}

		if (mPrimaryKeyColumn == -1) {
			System.out.println("No primary key found in table: "+mTableLongName);
			return false;
		}

		return true;
	}

	protected boolean setColumnType(int columnIndex, String typeString) {
		mColumnType[columnIndex] = -1;
		for (int type=0; type<COLUMN_TYPES.length; type++) {
			String code = COLUMN_TYPES[type];
			if (typeString.startsWith(code)) {
				mColumnType[columnIndex] = type;
				if (type == COLUMN_TYPE_FK)
					mForeignKey[columnIndex] = new ForeignKey(typeString);
				return true;
			}
		}
		return false;
	}

	public String getSpecification() {
		return mSpecification;
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

	public byte[] getPKFromID(byte[] id) {
		return mIDToPKMap.get(id);
	}

	public AlphaNumRow getRow(byte[] primaryKey) {
		return mPKToRowMap.get(primaryKey);
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

	public int getIDColumn() {
		return mIDColumn;
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

		addDerivedColumnsToSQL(sql);

		sql.append(" FROM ");
		sql.append(mTableLongName);

		return sql.toString();
	}

	protected String insertRow(TreeMap<String,String> columnValueMap, byte[][] newPrimaryKeyHolder) {
		StringBuilder sql = new StringBuilder("INSERT INTO ");
		sql.append(mTableLongName);
		sql.append(' ');

		int count = 0;
		for (int i=0; i<mColumnName.length; i++) {
			String value = columnValueMap.get(mColumnName[i]);
			if (value != null) {
				sql.append(count == 0 ? '(' : ",");
				sql.append(mColumnName[i]);
				count++;
			}
		}

		if (count == 0)
			return "No column data found.";

		sql.append(") VALUES ");

		count = 0;
		for (int column=0; column<mColumnName.length; column++) {
			String value = columnValueMap.get(mColumnName[column]);
			if (value != null) {
				String errorMsg = checkValue(value, column);
				if (errorMsg != null)
					return errorMsg;
				if (column == mIDColumn && mIDToPKMap.containsKey(value.getBytes(StandardCharsets.UTF_8)))
					return mColumnName[column].concat(" '".concat(value).concat("' does already exist."));

				sql.append(count == 0 ? '(' : ",");
				if (value.isEmpty()) {
					sql.append("NULL");
				}
				else {
					if (mColumnType[column] == COLUMN_TYPE_TEXT
					 || mColumnType[column] == COLUMN_TYPE_ID
					 || mColumnType[column] == COLUMN_TYPE_DATE) {
						sql.append('\'');
						sql.append(value);
						sql.append('\'');
					}
					else {
						sql.append(value);
					}
				}
				count++;
			}
		}

		sql.append(")");

		String errorMsg = runUpdateSQL(sql.toString(), newPrimaryKeyHolder);
		if (errorMsg != null)
			return errorMsg;

		AlphaNumRow row = new AlphaNumRow(getColumnCount());
		byte[] primaryKey = newPrimaryKeyHolder[0];
		row.setData(mPrimaryKeyColumn, primaryKey);
		for (String columnName:columnValueMap.keySet()) {
			int column = getColumnIndex(columnName);
			String value = columnValueMap.get(columnName);
			if (value != null) {
				row.setData(column, value.isEmpty() ? null : value.getBytes(StandardCharsets.UTF_8));
				if (mColumnType[column] == COLUMN_TYPE_NUM) {
					try {
						row.setFloat(Float.parseFloat(value), column);
					}
					catch (NumberFormatException nfe) {
						row.setFloat(Float.NaN, column);
					}
				}
			}
		}
		mPKToRowMap.put(primaryKey, row);
		if (mIDColumn != -1)
			mIDToPKMap.put(row.getData(mIDColumn), primaryKey);
		mRowList.add(row);
		return null;
	}

	protected String updateRow(TreeMap<String,String> columnValueMap, byte[] primaryKey, boolean issueErrorIfNoChange) {
		AlphaNumRow row = mPKToRowMap.get(primaryKey);

		StringBuilder sql = new StringBuilder("UPDATE ");
		sql.append(mTableLongName);
		sql.append(" SET");

		int count = 0;
		for (int column=0; column<mColumnName.length; column++) {
			String value = columnValueMap.get(mColumnName[column]);
			if (value != null) {
				String errorMsg = checkValue(value, column);
				if (errorMsg != null)
					return errorMsg;

				byte[] currentValue = row.getData(getColumnIndex(mColumnName[column]));
				if ((currentValue == null && !value.isEmpty())
				 || (currentValue != null && !value.equals(new String(currentValue)))) {
					if (column == mIDColumn && mIDToPKMap.containsKey(value.getBytes(StandardCharsets.UTF_8)))
						return mColumnName[column].concat(" '".concat(value).concat("' does already exist."));

					sql.append(count == 0 ? ' ' : ',');
					sql.append(mColumnName[column]);
					sql.append('=');
					if (value.isEmpty()) {
						sql.append("NULL");
					}
					else {
						if (mColumnType[column] == COLUMN_TYPE_TEXT
						 || mColumnType[column] == COLUMN_TYPE_ID
						 || mColumnType[column] == COLUMN_TYPE_DATE) {
							sql.append('\'');
							sql.append(value);
							sql.append('\'');
						}
						else {
							sql.append(value);
						}
					}
					count++;
				}
			}
		}

		if (count == 0)
			return issueErrorIfNoChange ? "No changes found in update request for table '"+mTableDisplayName+"'." : null;

		sql.append(" WHERE ");
		sql.append(mColumnName[mPrimaryKeyColumn]);
		sql.append('=');
		sql.append(new String(primaryKey, StandardCharsets.UTF_8));

		String errorMsg = runUpdateSQL(sql.toString(), null);
		if (errorMsg != null)
			return errorMsg;

		for (int column=0; column<mColumnName.length; column++) {
			String newValue = columnValueMap.get(mColumnName[column]);
			if (newValue != null) {
				byte[] value = newValue.isEmpty() ? null : newValue.getBytes(StandardCharsets.UTF_8);
				if (column == mIDColumn) {
					byte[] pk = mIDToPKMap.remove(row.getData(mIDColumn));
					if (pk != null) // shouldn't be null
						mIDToPKMap.put(value, pk);
				}
				row.setData(column, value);
				if (mColumnType[column] == COLUMN_TYPE_NUM) {
					try {
						row.setFloat(Float.parseFloat(newValue), column);
					}
					catch (NumberFormatException nfe) {
						row.setFloat(Float.NaN, column);
					}
				}
			}
		}
		return null;
	}

	private String checkValue(String value, int column) {
		if (column == mIDColumn && value.isEmpty())
			return mColumnName[column].concat(" must not be empty.");
		if (mColumnType[column] == COLUMN_TYPE_NUM && !value.isEmpty()) {
			try {
				Float.parseFloat(value);
			}
			catch (NumberFormatException nfe) {
				return mColumnName[column].concat(" '").concat(value).concat("' is not numerical.");
			}
		}
		if (mColumnType[column] == COLUMN_TYPE_DATE && !value.isEmpty()) {
			if (!value.matches("\\d\\d\\d\\d-\\d\\d-\\d\\d"))
				return mColumnName[column].concat(" '").concat(value).concat("' must be given as 'YYYY-MM-DD'.");
		}
		return null;
	}

	protected String deleteRow(byte[] primaryKey) {
		StringBuilder sql = new StringBuilder("DELETE FROM ");
		sql.append(mTableLongName);
		sql.append(" WHERE ");
		sql.append(mColumnName[mPrimaryKeyColumn]);
		sql.append('=');
		sql.append(new String(primaryKey, StandardCharsets.UTF_8));
		String errorMsg = runUpdateSQL(sql.toString(), null);
		if (errorMsg != null)
			return errorMsg;

		AlphaNumRow row = mPKToRowMap.remove(primaryKey);
		if (row != null)
			mRowList.remove(row);
		if (mIDColumn != -1)
			mIDToPKMap.remove(row.getData(mIDColumn));
		return null;
	}

	protected String runUpdateSQL(String sql, byte[][] newPrimaryKeyHolder) {
		try {
			DatabaseConnector connector = DatabaseConnector.getInstance();
			if (connector.ensureConnection()) {
				Statement stmt = connector.getConnection().createStatement();
				stmt.executeUpdate(sql, newPrimaryKeyHolder == null ? Statement.NO_GENERATED_KEYS : Statement.RETURN_GENERATED_KEYS);
				if (newPrimaryKeyHolder != null) {
					ResultSet rs = stmt.getGeneratedKeys();
					if (rs.next())
						newPrimaryKeyHolder[0] = Integer.toString(rs.getInt(1)).getBytes(StandardCharsets.UTF_8);
				}
				stmt.close();
				return null;
			}
		}
		catch (SQLException e) {
			return "SQL exception: "+ e.getMessage();
		}
		return "Error: Server engine cannot connect to database.";
	}

	protected void addDerivedColumnsToSQL(StringBuilder sql) {
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

		mPKToRowMap = new TreeMap<>(new ByteArrayComparator());
		for (AlphaNumRow row:mRowList)
			mPKToRowMap.put(row.getData(mPrimaryKeyColumn), row);

		if (mIDColumn != -1) {
			mIDToPKMap = new TreeMap<>(new ByteArrayComparator());
			for (AlphaNumRow row:mRowList)
				mIDToPKMap.put(row.getData(mIDColumn), row.getData(mPrimaryKeyColumn));
		}

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

	public AlphaNumRow createRow() {
		return new AlphaNumRow(getColumnCount());
	}

	protected AlphaNumRow createRow(ResultSet rset) throws SQLException {
		AlphaNumRow row = createRow();

		for (int column=0; column<getColumnCount(); column++) {
			String s = rset.getString(column+1);
			if (s != null) {
				row.setData(column, s.getBytes(StandardCharsets.UTF_8));
				if (mColumnType[column] == COLUMN_TYPE_NUM) {
					try {
						row.setFloat(Float.parseFloat(s), column);
					}
					catch (NumberFormatException nfe) {
						row.setFloat(Float.NaN, column);
					}
				}
			}
		}

		return row;
	}
}