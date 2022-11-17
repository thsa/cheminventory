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

import java.sql.ResultSet;
import java.sql.SQLException;

public class CompoundTable extends AlphaNumTable {
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
		row.setStructure(idcode, coords, ffp, skelspheres);

		return row;
	}
}
