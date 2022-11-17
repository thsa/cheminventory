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

import java.io.PrintStream;
import java.util.Properties;

public class ResultBuilder implements ConfigurationKeys {
	private static final int RESULT_STRUCTURE_COLUMNS = 3;
	private static final int RESULT_COLUMN_IDCODE = 0;
	private static final int RESULT_COLUMN_COORDS2D = 1;
	private static final int RESULT_COLUMN_FFP512 = 2;
	private static final String[] RESULT_STRUCTURE_COLUMN_TITLE = { "idcode", "idcoordinates2D", "FragFp" };

	private InMemoryData mData;
	private int mAlphaNumColumnCount;
	private AlphaNumTable[] mResultTable;
	private int[] mResultColumn,mForeignKeyIndex;

	/**
	 * The ResultBuilder takes a hitlist and creates a result table with the expected columns
	 * from the bottle table and other joined tables according to the result column structure
	 * defined in the config file.
	 * Potentially, there could be multiple result builders, each initialized from a different
	 * config file section, such that result table tables could contain different information
	 * depending on parameter passed to the search request.
	 * @param data
	 */
	public ResultBuilder(InMemoryData data) {
		mData = data;
	}

	/***
	 * Parses the config file and creates all internal variables for efficient result table creation.
	 * @param config
	 * @return
	 */
	public boolean initialize(Properties config) {
		String resultDef = config.getProperty(RESULT_TABLE);
		if (resultDef == null || resultDef.length() == 0) {
			System.out.println("'"+RESULT_TABLE+"' is missing in config file.");
			return false;
		}

		String[] columnDef = resultDef.split(",");
		mAlphaNumColumnCount = columnDef.length;
		mResultTable = new AlphaNumTable[mAlphaNumColumnCount];
		mResultColumn = new int[mAlphaNumColumnCount];
		mForeignKeyIndex = new int[mAlphaNumColumnCount];

		for (int i=0; i<mAlphaNumColumnCount; i++) {
			int index = columnDef[i].indexOf('.');
			if (index == -1) {
				System.out.println("Invalid '"+RESULT_TABLE+"' definition. Must be comma separated list of '<table_alias>.<column_name>'");
				return false;
			}
			String alias = columnDef[i].substring(0, index).trim();
			String columnName = columnDef[i].substring(index+1).trim();

			for (AlphaNumTable table:mData.getTables()) {
				if (alias.equals(table.getAliasName())) {
					mResultTable[i] = table;
					break;
				}
			}
			if (mResultTable[i] == null) {
				System.out.println("Invalid '"+RESULT_TABLE+"' definition. Could not find table alias '"+alias+"'.");
				return false;
			}

			mResultColumn[i] = mResultTable[i].getColumnIndex(columnName);
			if (mResultColumn[i] == -1) {
				System.out.println("Invalid '"+RESULT_TABLE+"' definition. Could not find table column '"+columnDef[i]+"'.");
				return false;
			}

			if (mResultTable[i] != mData.getBottleTable()) {
				mForeignKeyIndex[i] = mData.getBottleTable().getForeignKeyIndex(alias);
				if (mForeignKeyIndex[i] == -1) {
					System.out.println("Invalid '"+RESULT_TABLE+"' definition. Could not find FK in bottle table: '"+alias+"'.");
					return false;
				}
			}
		}

		return true;
	}

	public byte[][][] buildResult(int[] hitIndexes) {
		byte[][][] result = new byte[hitIndexes.length+1][RESULT_STRUCTURE_COLUMNS+mAlphaNumColumnCount][];

		// create header row
		for (int i=0; i<RESULT_STRUCTURE_COLUMNS; i++)
			result[0][i] = RESULT_STRUCTURE_COLUMN_TITLE[i].getBytes();

		for (int column=0; column<mAlphaNumColumnCount; column++)
			result[0][RESULT_STRUCTURE_COLUMNS+column] = mResultTable[column].getColumnTitle(mResultColumn[column]).getBytes();

		for (int i=0; i<hitIndexes.length; i++)
			result[i+1] = createResultRow(hitIndexes[i]);

		return result;
	}

	private byte[][] createResultRow(int rowIndex) {
		byte[][] resultRow = new byte[RESULT_STRUCTURE_COLUMNS+mAlphaNumColumnCount][];

		AlphaNumTable bottleTable = mData.getBottleTable();
		AlphaNumRow bottleRow = bottleTable.getRow(rowIndex);

		CompoundTable compoundTable = mData.getCompoundTable();
		CompoundRow compoundRow = mData.getCompoundRow(bottleRow);

		resultRow[0] = compoundRow.getIDCode();
		resultRow[1] = compoundRow.getCoords();
		resultRow[2] = compoundRow.getFFPBytes();

		for (int column=0; column<mAlphaNumColumnCount; column++) {
			if (mResultTable[column] == bottleTable)
				resultRow[RESULT_STRUCTURE_COLUMNS + column] = bottleRow.getData(mResultColumn[column]);
			else if (mResultTable[column] == compoundTable)
				resultRow[RESULT_STRUCTURE_COLUMNS + column] = compoundRow.getData(mResultColumn[column]);
			else
				resultRow[RESULT_STRUCTURE_COLUMNS + column] = bottleRow.getReferencedRow(mForeignKeyIndex[column]).getData(mResultColumn[column]);
			}

		return resultRow;
	}

/*	public String buildResultString(int[] hitIndexes, boolean includeStructureColumns) {
		StringBuilder result = new StringBuilder();

		if (includeStructureColumns) {
			for (int i=0; i<RESULT_STRUCTURE_COLUMNS; i++) {
				result.append(RESULT_STRUCTURE_COLUMN_TITLE[i]);
				result.append("\t");
			}
		}
		for (int column=0; column<mAlphaNumColumnCount; column++) {
			result.append(mResultTable[column].getColumnTitle(mResultColumn[column]));
			result.append(column == mAlphaNumColumnCount-1 ? "\n" : "\t");
		}

		for (int i=0; i<hitIndexes.length; i++) {
			appendResultRowString(hitIndexes[i], includeStructureColumns, result);
		}

		return result.toString();
	}

	private void appendResultRowString(int rowIndex, boolean includeStructureColumns, StringBuilder result) {
		AlphaNumTable bottleTable = mData.getBottleTable();
		AlphaNumRow bottleRow = bottleTable.getRow(rowIndex);

		CompoundTable compoundTable = mData.getCompoundTable();
		CompoundRow compoundRow = mData.getCompoundRow(bottleRow);

		if (includeStructureColumns) {
			result.append(new String(compoundRow.getIDCode()));
			result.append("\t");
			result.append(new String(compoundRow.getCoords()));
			result.append("\t");
			result.append(new String(compoundRow.getFFPBytes()));
			result.append("\t");
		}

		for (int column=0; column<mAlphaNumColumnCount; column++) {
			byte[] data = (mResultTable[column] == bottleTable) ? bottleRow.getData(mResultColumn[column])
						: (mResultTable[column] == compoundTable) ? compoundRow.getData(mResultColumn[column])
						: bottleRow.getReferencedRow(mForeignKeyIndex[column]).getData(mResultColumn[column]);
			if (data != null)
				result.append(new String(data));
			result.append(column == mAlphaNumColumnCount-1 ? "\n" : "\t");
		}
	}*/

	public void printResult(int[] hitIndexes, PrintStream body, boolean includeStructureColumns) {
		if (includeStructureColumns) {
			for (int i=0; i<RESULT_STRUCTURE_COLUMNS; i++) {
				body.print(RESULT_STRUCTURE_COLUMN_TITLE[i]);
				body.print("\t");
			}
		}
		for (int column=0; column<mAlphaNumColumnCount; column++) {
			body.print(mResultTable[column].getColumnTitle(mResultColumn[column]));
			body.print(column == mAlphaNumColumnCount-1 ? "\n" : "\t");
		}

		for (int hitIndex:hitIndexes) {
			printResultRow(hitIndex, includeStructureColumns, body);
		}
	}

	private void printResultRow(int rowIndex, boolean includeStructureColumns, PrintStream body) {
		AlphaNumTable bottleTable = mData.getBottleTable();
		AlphaNumRow bottleRow = bottleTable.getRow(rowIndex);

		CompoundTable compoundTable = mData.getCompoundTable();
		CompoundRow compoundRow = mData.getCompoundRow(bottleRow);

		if (includeStructureColumns) {
			body.print(new String(compoundRow.getIDCode()));
			body.print("\t");
			body.print(new String(compoundRow.getCoords()));
			body.print("\t");
			body.print(new String(compoundRow.getFFPBytes()));
			body.print("\t");
		}

		for (int column=0; column<mAlphaNumColumnCount; column++) {
			byte[] data = (mResultTable[column] == bottleTable) ? bottleRow.getData(mResultColumn[column])
					: (mResultTable[column] == compoundTable) ? compoundRow.getData(mResultColumn[column])
					: bottleRow.getReferencedRow(mForeignKeyIndex[column]).getData(mResultColumn[column]);
			if (data != null)
				body.print(new String(data));
			body.print(column == mAlphaNumColumnCount-1 ? "\n" : "\t");
		}
	}
}
