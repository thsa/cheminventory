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

public class QueryColumn {
	private AlphaNumTable mTable;
	private int mColumnIndex,mColumnType;

	public QueryColumn(AlphaNumTable table, int columnIndex, int columnType) {
		mTable = table;
		mColumnIndex = columnIndex;
		mColumnType = columnType;
	}

	public AlphaNumTable getTable() {
		return mTable;
	}

	public int getColumnType() {
		return mColumnType;
	}

	public int getColumnIndex() {
		return mColumnIndex;
	}
}
