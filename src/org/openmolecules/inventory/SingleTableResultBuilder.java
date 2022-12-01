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

public class SingleTableResultBuilder implements ConfigurationKeys {
	private AlphaNumTable mTable;

	/**
	 * The ResultBuilder takes a hitlist and creates a result table with the expected columns
	 * from the bottle table and other joined tables according to the result column structure
	 * defined in the config file.
	 * Potentially, there could be multiple result builders, each initialized from a different
	 * config file section, such that result table tables could contain different information
	 * depending on parameter passed to the search request.
	 * @param table
	 */
	public SingleTableResultBuilder(AlphaNumTable table) {
		mTable = table;
	}

	public byte[][][] buildResult(int[] hitIndexes, boolean includeStructureColumns) {
		includeStructureColumns &= (mTable instanceof CompoundTable);
		int structureColumnCount = includeStructureColumns ? CompoundTable.STRUCTURE_COLUMN_TITLE.length : 0;

		byte[][][] result = new byte[hitIndexes.length+1][structureColumnCount + mTable.getColumnCount()][];

		// create header row
		for (int i=0; i<structureColumnCount; i++)
			result[0][i] = CompoundTable.STRUCTURE_COLUMN_TITLE[i].getBytes();
		for (int column=0; column<mTable.getColumnCount(); column++)
			result[0][structureColumnCount + column] = mTable.getColumnTitle(column).getBytes();

		for (int i=0; i<hitIndexes.length; i++) {
			if (includeStructureColumns) {
				CompoundRow row = (CompoundRow)mTable.getRow(hitIndexes[i]);
				result[i+1][0] = row.getIDCode();
				result[i+1][1] = row.getCoords();
				result[i+1][2] = row.getFFPBytes();
			}
			byte[][] row = mTable.getRow(i).getRowData();
			for (int j=0; j<row.length; j++)
				result[i+1][structureColumnCount+j] = row[j];
			}

		return result;
	}

	public void printResult(int[] hitIndexes, PrintStream body, boolean includeStructureColumns) {
		includeStructureColumns &= (mTable instanceof CompoundTable);

		if (includeStructureColumns) {
			for (String title:CompoundTable.STRUCTURE_COLUMN_TITLE) {
				body.print(title);
				body.print("\t");
			}
		}

		for (int column=0; column<mTable.getColumnCount(); column++) {
			body.print(mTable.getColumnTitle(column));
			body.print(column == mTable.getColumnCount()-1 ? "\n" : "\t");
		}

		for (int hitIndex:hitIndexes) {
			if (includeStructureColumns) {
				CompoundRow row = (CompoundRow)mTable.getRow(hitIndex);
				body.print(new String(row.getIDCode()));
				body.print("\t");
				body.print(new String(row.getCoords()));
				body.print("\t");
				body.print(new String(row.getFFPBytes()));
				body.print("\t");
			}
			byte[][] rowData = mTable.getRow(hitIndex).getRowData();
			for (int column=0; column<rowData.length; column++) {
				if (rowData[column] != null)
					body.print(new String(rowData[column]));
				body.print(column == rowData.length-1 ? "\n" : "\t");
			}
		}
	}
}
