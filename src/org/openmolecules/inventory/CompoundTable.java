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
import com.actelion.research.chem.MolecularFormula;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorHandlerLongFFP512;
import com.actelion.research.chem.descriptor.DescriptorHandlerSkeletonSpheres;
import com.actelion.research.util.DoubleFormat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;

public class CompoundTable extends AlphaNumTable {
	public static final String[] STRUCTURE_COLUMN_TITLE = { "Structure", "ID-Coords", "FragFp" };
	private static final String SELECT_COLUMNS = "idcode,idcoords,fragfp,skelspheres";
	private static final String[] CREATE_COLUMNS = {
			"idcode varchar(255)", "idcoords varchar(255)", "fragfp varchar(255)", "skelspheres varchar(1023)" };

	private int mMWColumn, mMFColumn;

	public CompoundTable() {
		super();
		mMWColumn = -1;
		mMFColumn = -1;
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

	@Override protected boolean setColumnType(int columnIndex, String typeString) {
		if (typeString.equals("[mw]")) {
			mMWColumn = columnIndex;
			typeString = "[num]";
		}
		if (typeString.equals("[mf]")) {
			mMFColumn = columnIndex;
			typeString = "[text]";
		}
		return super.setColumnType(columnIndex, typeString);
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

	@Override
	protected String updateRow(TreeMap<String,String> columnValueMap, byte[] primaryKey, boolean issueErrorIfNoChange) {
		String idcode = columnValueMap.get("idcode");
		String coords = columnValueMap.get("idcoords");
		if (idcode != null && idcode.isEmpty())
			idcode = null;
		if (coords != null && coords.isEmpty())
			coords = null;
		StereoMolecule mol = (idcode == null) ? null : new IDCodeParser().getCompactMolecule(idcode, coords);
		calculateMWAndMF(columnValueMap, mol);

		boolean structureChanged = false;
		CompoundRow row = (CompoundRow)getRow(primaryKey);
		if (row.getIDCode() == null || !new String(row.getIDCode()).equals(idcode))
			structureChanged = true;
		if (row.getCoords() == null || !new String(row.getCoords()).equals(coords))
			structureChanged = true;

		String errorMsg = super.updateRow(columnValueMap, primaryKey, issueErrorIfNoChange && !structureChanged);
		if (errorMsg != null)
			return errorMsg;

		if (mol != null)
			return updateIDCodeAndDescriptors(mol, idcode, coords, primaryKey);

		return null;
	}

	@Override
	protected String insertRow(TreeMap<String,String> columnValueMap, byte[][] newPrimaryKeyHolder) {
		String idcode = columnValueMap.get("idcode");
		String coords = columnValueMap.get("idcoords");
		if (idcode != null && idcode.isEmpty())
			idcode = null;
		if (coords != null && coords.isEmpty())
			coords = null;
		StereoMolecule mol = (idcode == null) ? null : new IDCodeParser().getCompactMolecule(idcode, coords);
		calculateMWAndMF(columnValueMap, mol);

		String errorMsg = super.insertRow(columnValueMap, newPrimaryKeyHolder);
		if (errorMsg != null)
			return errorMsg;

		if (mol != null)
			return updateIDCodeAndDescriptors(mol, idcode, coords, newPrimaryKeyHolder[0]);

		return null;
	}

	private void calculateMWAndMF(TreeMap<String,String> columnValueMap, StereoMolecule mol) {
		if (mol != null && (mMFColumn != -1 || mMWColumn != -1)) {
			MolecularFormula formula = new MolecularFormula(mol);
			if (mMFColumn != -1)
				columnValueMap.put(getColumnName(mMFColumn), formula.getFormula());
			if (mMWColumn != -1)
				columnValueMap.put(getColumnName(mMWColumn), DoubleFormat.toString(formula.getRelativeWeight(), 4));
		}
	}

	private String updateIDCodeAndDescriptors(StereoMolecule mol, String idcode, String coords, byte[] primaryKey) {
		if (coords == null)
			coords = "";
		long[] ffp = DescriptorHandlerLongFFP512.getDefaultInstance().createDescriptor(mol);
		String encodedFFP = DescriptorHandlerLongFFP512.getDefaultInstance().encode(ffp);
		byte[] skelSpheres = DescriptorHandlerSkeletonSpheres.getDefaultInstance().createDescriptor(mol);
		String encodedSkelSpheres = DescriptorHandlerSkeletonSpheres.getDefaultInstance().encode(skelSpheres);

		StringBuilder sql = new StringBuilder("UPDATE ");
		sql.append(getLongName());
		sql.append(" SET idcode='");
		sql.append(idcode);
		sql.append("',idcoords='");
		sql.append(coords);
		sql.append("',fragfp='");
		sql.append(encodedFFP);
		sql.append("',skelspheres='");
		sql.append(encodedSkelSpheres);
		sql.append("' WHERE ");
		sql.append(getColumnName(getPrimaryKeyColumn()));
		sql.append('=');
		sql.append(new String(primaryKey));

		String errorMsg = runUpdateSQL(sql.toString(), null);
		if (errorMsg != null)
			return null;

		CompoundRow row = (CompoundRow)getRow(primaryKey);
		row.setStructure(idcode, coords, ffp, encodedFFP, encodedFFP);
		return null;
	}
}
