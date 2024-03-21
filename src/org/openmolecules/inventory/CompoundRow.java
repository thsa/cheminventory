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

import com.actelion.research.chem.descriptor.DescriptorHandlerLongFFP512;
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
		mIDCode = (idcode == null || idcode.isEmpty()) ? null : idcode.getBytes();
		mCoords = (idcode == null || coords == null || coords.isEmpty()) ? null : coords.getBytes();
		mFFPBytes = (idcode == null || encodedFFP == null || encodedFFP.isEmpty()) ? null : encodedFFP.getBytes();
		mFFP = (idcode == null) ? null : ffp != null ? ffp : encodedFFP != null && !encodedFFP.isEmpty() ? DescriptorHandlerLongFFP512.getDefaultInstance().decode(encodedFFP) : null;
		mSkelSpheres = (idcode == null || encodedSkelSpheres == null || encodedSkelSpheres.isEmpty()) ? null : DescriptorHandlerSkeletonSpheres.getDefaultInstance().decode(encodedSkelSpheres);
	}
}
