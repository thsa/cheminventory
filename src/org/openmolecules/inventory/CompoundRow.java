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

import com.actelion.research.chem.descriptor.DescriptorHandlerSkeletonSpheres;

public class CompoundRow extends AlphaNumRow {
	private byte[] mIDCode,mCoords,mFFPBytes;
	private long[] mFFP;
	private byte[] mSkelSpheres;

	public CompoundRow(int columnCount) {
		super(columnCount);
	}

	public byte[] getIDCode() {
		return mIDCode;
	}

	public byte[] getCoords() {
		return mCoords;
	}

	public byte[] getFFPBytes() {
		return mFFPBytes;
	}

	public long[] getFFP() {
		return mFFP;
	}

	public byte[] getSkelSpheres() {
		return mSkelSpheres;
	}

	public void setStructure(String idcode, String coords, long[] ffp, String encodedFFP, String encodedSkelSpheres) {
		mIDCode = (idcode == null) ? null : idcode.getBytes();
		mCoords = (idcode == null || coords == null) ? null : coords.getBytes();
		mFFPBytes = (idcode == null || encodedFFP == null) ? null : encodedFFP.getBytes();
		mFFP = ffp;
		mSkelSpheres = (idcode == null || encodedSkelSpheres == null) ? null : DescriptorHandlerSkeletonSpheres.getDefaultInstance().decode(encodedSkelSpheres);
	}
}
