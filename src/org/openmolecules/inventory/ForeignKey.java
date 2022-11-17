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

public class ForeignKey implements ConfigurationKeys {
	private String mFKTypeString;
	private AlphaNumTable mReferencedTable;

	/**
	 * @param fk_def in the form "[fk:<table-alias>.<column-name>]"
	 */
	public ForeignKey(String fk_def) {
		mFKTypeString = fk_def;
	}

	public boolean validate(AlphaNumTable[] allTables) {
		String reference = mFKTypeString.substring(COLUMN_TYPES[COLUMN_TYPE_FK].length(), mFKTypeString.length()-1);

		int index = reference.indexOf('.');
		if (index == -1) {
			System.out.println("Invalid foreign key specification. Needed: \"[fk:<tableAlias>.<columnName>]\" Found: "+mFKTypeString);
			return false;
		}

		String tableNameOrAlias = reference.substring(0, index);
		String columnName = reference.substring(index+1);

		for (AlphaNumTable table : allTables) {
			if (tableNameOrAlias.equals(table.getAliasName())
			 || tableNameOrAlias.equals(table.getName())
			 || tableNameOrAlias.equals(table.getLongName())) {
				mReferencedTable = table;
				break;
			}
		}

		if (mReferencedTable.getPrimaryKeyColumn() != mReferencedTable.getColumnIndex(columnName)) {
			System.out.println("Referenced column is not primary key: "+mFKTypeString);
			return false;
		}

		if (mReferencedTable == null) {
			System.out.println("Invalid foreign key specification. Referenced table not found: "+mFKTypeString);
			return false;
		}

		return true;
	}

	public AlphaNumTable getReferencedTable() {
		return mReferencedTable;
	}
}

