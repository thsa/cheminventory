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

public class AlphaNumRow {
	private final byte[][] mData;
	private final float[] mFloat;
	private AlphaNumRow[] mReferencedRow;

	public AlphaNumRow(int columnCount) {
		mData = new byte[columnCount][];
		mFloat = new float[columnCount];
	}

	public void setData(int column, byte[] data) {
		mData[column] = (data != null && data.length == 0) ? null : data;
	}

	public byte[] getData(int column) {
		return mData[column];
	}

	public byte[][] getRowData() {
		return mData;
	}

	public void setFloat(float f, int column) {
		mFloat[column] = f;
	}

	public float getFloat(int column) {
		return mFloat[column];
	}

	public void setReferencedRows(AlphaNumRow[] referencedRow) {
		mReferencedRow = referencedRow;
	}

	public AlphaNumRow getReferencedRow(int column) {
		return mReferencedRow[column];
	}
}
