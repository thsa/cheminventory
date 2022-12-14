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

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorHandlerLongFFP512;
import com.actelion.research.chem.descriptor.DescriptorHandlerSkeletonSpheres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;

public class CompoundTable extends AlphaNumTable {
	public static final String[] STRUCTURE_COLUMN_TITLE = { "ID-Code", "ID-Coords", "FragFp" };
	private static final String SELECT_COLUMNS = "idcode,idcoords,fragfp,skelspheres";
	private static final String[] CREATE_COLUMNS = {
			"idcode varchar(255)", "idcoords varchar(255)", "fragfp varchar(255)", "skelspheres varchar(1023)" };

	public CompoundTable() {
		super();
	}

	@Override
	protected void addOptionalTableNamesToSQL(StringBuilder sql) {
		sql.append(",");
		sql.append(SELECT_COLUMNS);
	}

	@Override
	protected void addTableCreationSQLStructureColumns(StringBuilder script) {
		for (String columndef:CREATE_COLUMNS) {
			script.append("    ");
			script.append(columndef);
			script.append(",\n");
		}
	}

	@Override
	protected AlphaNumRow createRow(ResultSet rset) throws SQLException {
		CompoundRow row = new CompoundRow(getColumnCount());

		int column = 0;
		for (int i=0; i<getColumnCount(); i++) {
			String s = rset.getString(++column);
			row.setData(column-1, s == null ? null : s.getBytes());
		}

		String idcode = rset.getString(++column);
		String coords = rset.getString(++column);
		String ffp = rset.getString(++column);
		String skelspheres = rset.getString(++column);
		row.setStructure(idcode, coords, null, ffp, skelspheres);

		return row;
	}

	protected boolean updateRow(TreeMap<String,String> columnValueMap, String primaryKey) {
		if (super.updateRow(columnValueMap, primaryKey)) {
			String idcode = columnValueMap.get("idcode");
			String coords = columnValueMap.get("idcoords");
			if (idcode != null)
				return updateDescriptors(idcode, coords, primaryKey);
		}
		return true;
	}

	protected boolean insertRow(TreeMap<String,String> columnValueMap, int[] newPrimaryKeyHolder) {
		if (super.insertRow(columnValueMap, newPrimaryKeyHolder)) {
			String idcode = columnValueMap.get("idcode");
			String coords = columnValueMap.get("idcoords");
			if (idcode != null)
				return updateDescriptors(idcode, coords, Integer.toString(newPrimaryKeyHolder[0]));
		}
		return true;
	}

	private boolean updateDescriptors(String idcode, String coords, String primaryKey) {
		StereoMolecule mol = new IDCodeParser().getCompactMolecule(idcode, coords);
		long[] ffp = DescriptorHandlerLongFFP512.getDefaultInstance().createDescriptor(mol);
		String encodedFFP = DescriptorHandlerLongFFP512.getDefaultInstance().encode(ffp);
		byte[] skelSpheres = DescriptorHandlerSkeletonSpheres.getDefaultInstance().createDescriptor(mol);
		String encodedSkelSpheres = DescriptorHandlerSkeletonSpheres.getDefaultInstance().encode(skelSpheres);

		StringBuilder sql = new StringBuilder("UPDATE ");
		sql.append(getLongName());
		sql.append(" SET ffp='");
		sql.append(encodedFFP);
		sql.append("',skelspheres='");
		sql.append(encodedSkelSpheres);
		sql.append("' WHERE ");
		sql.append(getColumnName(getPrimaryKeyColumn()));
		sql.append('=');
		sql.append(primaryKey);

		if (runUpdateSQL(sql.toString(), null)) {
			CompoundRow row = (CompoundRow)getRow(primaryKey.getBytes());
			row.setStructure(idcode, coords, ffp, encodedFFP, encodedFFP);
			return true;
		}
		return false;
	}
}
