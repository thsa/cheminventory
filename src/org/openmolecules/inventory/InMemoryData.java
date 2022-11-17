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

import com.actelion.research.chem.StructureSearchDataSource;
import com.actelion.research.chem.StructureSearchSpecification;
import com.actelion.research.chem.descriptor.DescriptorConstants;

import java.util.Properties;

public class InMemoryData implements ConfigurationKeys,StructureSearchDataSource {
	private Properties mConfig;
	private AlphaNumTable mBottleTable;
	private CompoundTable mCompoundTable;
	private AlphaNumTable[] mAllTables; // includes mCompoundTable and mBottleTable at the end
	private int mCompoundForeignKeyIndex;

	public InMemoryData(Properties config) {
		mConfig = config;
	}

	public boolean createTableCreationScript() {
		if (!initialize())
			return false;

		StringBuilder script = new StringBuilder();

		script.append("USE ");
		script.append(mConfig.getProperty(DATABASE_NAME));
		script.append(";\n");
		script.append("\n");

		for (AlphaNumTable table : mAllTables) {
			table.addTableCreationSQL(script, mAllTables);
			script.append("\n");
		}

		System.out.println(script);

		return true;
	}

	private boolean initialize() {
		String compoundDef = mConfig.getProperty(COMPOUND_TABLE);
		if (compoundDef == null || compoundDef.length() == 0) {
			System.out.println("'"+COMPOUND_TABLE+"' missing in config file.");
			return false;
		}
		String bottleDef = mConfig.getProperty(BOTTLE_TABLE);
		if (bottleDef == null || bottleDef.length() == 0) {
			System.out.println("'"+BOTTLE_TABLE+"' missing in config file.");
			return false;
		}
		int supportTableCount;
		String supportTableCountText = mConfig.getProperty(SUPPORT_TABLES_COUNT);
		if (supportTableCountText == null || supportTableCountText.length() == 0) {
			System.out.println("'" + SUPPORT_TABLES_COUNT + "' missing in config file.");
			return false;
		}
		try {
			supportTableCount = Integer.parseInt(supportTableCountText);
		}
		catch (NumberFormatException nfe) {
			System.out.println("'"+SUPPORT_TABLES_COUNT+"' is not an integer.");
			return false;
		}
		String[] supportTableDef = new String[supportTableCount];
		for (int i=0; i<supportTableCount; i++) {
			String key = SUPPORT_TABLE+(i+1);
			supportTableDef[i] = mConfig.getProperty(key);
			if (supportTableDef[i] == null || supportTableDef[i].length() == 0) {
				System.out.println("'"+key+"' missing in config file.");
				return false;
			}
		}

		mCompoundTable = new CompoundTable();
		if (!mCompoundTable.initialize(compoundDef)) {
			System.out.println("Could not initialize compound table. Check config!");
			return false;
		}
		mBottleTable = new AlphaNumTable();
		if (!mBottleTable.initialize(bottleDef)) {
			System.out.println("Could not initialize bottle table. Check config!");
			return false;
		}
		mAllTables = new AlphaNumTable[supportTableCount+2];
		for (int i=0; i<supportTableCount; i++) {
			mAllTables[i] = new AlphaNumTable();
			if (!mAllTables[i].initialize(supportTableDef[i])) {
				System.out.println("Could not initialize support table "+i+". Check config!");
				return false;
			}
		}
		mAllTables[supportTableCount] = mCompoundTable;
		mAllTables[supportTableCount+1] = mBottleTable;

		for (AlphaNumTable table:mAllTables)
			if (!table.validateForeignKeys(mAllTables))
				return false;

		mCompoundForeignKeyIndex = -1;
		for (int i = 0; i<mBottleTable.getForeignKeys().length; i++) {
			if (mBottleTable.getForeignKeys()[i].getReferencedTable() == mCompoundTable) {
				mCompoundForeignKeyIndex = i;
				break;
			}
		}
		if (mCompoundForeignKeyIndex == -1) {
			System.out.println("Could not process table reference from bottle to compound.");
			return false;
		}

		return true;
	}

	public AlphaNumTable[] getTables() {
		return mAllTables;
	}

	public AlphaNumTable getBottleTable() {
		return mBottleTable;
	}

	public CompoundTable getCompoundTable() {
		return mCompoundTable;
	}

	public CompoundRow getCompoundRow(AlphaNumRow bottleRow) {
		return (CompoundRow)bottleRow.getReferencedRow(mCompoundForeignKeyIndex);
	}

	public boolean load() {
		if (!initialize())
			return false;

		DatabaseConnector connector = new DatabaseConnector(mConfig.getProperty(CONNECT_STRING), mConfig.getProperty(DATABASE_USER), mConfig.getProperty(DATABASE_PASSWORD));
		for (AlphaNumTable table:mAllTables) {
			if (!table.loadData(connector)) {
				System.out.println("Could not load data of table "+table.getName()+".");
				return false;
			}
		}
		for (AlphaNumTable table:mAllTables) {
			if (!table.buildForeignKeyReferences()) {
				System.out.println("Could not create foreign key references of table "+table.getName()+".");
				return false;
			}
		}
		return true;
	}

	@Override
	public int getRowCount() {
		return mBottleTable.getRowCount();
	}

	@Override
	public int getStructureCount(int row) {
		return 1;
	}

	@Override
	public int getDescriptorColumn(String descriptorShortName) {
		return DescriptorConstants.DESCRIPTOR_FFP512.shortName.equals(descriptorShortName) ? 0
			 : DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName.equals(descriptorShortName) ? 1 : -1;
	}

	@Override
	public Object getDescriptor(int column, int row, int i, boolean largestFragmentOnly) {
		CompoundRow compoundRow = getCompoundRow(mBottleTable.getRow(row));
		return (compoundRow == null) ? null
				: (column == 0) ? (largestFragmentOnly ? null : compoundRow.getFFP())
				: (column == 1) ? (largestFragmentOnly ? null : compoundRow.getSkelSpheres())
				: null;
	}

	@Override
	public byte[] getIDCode(int row, int i, boolean largestFragmentOnly) {
		CompoundRow compoundRow = getCompoundRow(mBottleTable.getRow(row));
		return (compoundRow == null) ? null : largestFragmentOnly ? null : compoundRow.getIDCode();
	}

	@Override
	public long getNoStereoCode(int row, int i, boolean largestFragmentOnly) {
		return SEARCH_TYPE_NOT_SUPPORTED;
	}

	@Override
	public long getTautomerCode(int row, int i, boolean largestFragmentOnly) {
		return SEARCH_TYPE_NOT_SUPPORTED;
	}

	@Override
	public long getNoStereoTautomerCode(int row, int i, boolean largestFragmentOnly) {
		return SEARCH_TYPE_NOT_SUPPORTED;
	}

	@Override
	public long getBackboneCode(int row, int i, boolean largestFragmentOnly) {
		return SEARCH_TYPE_NOT_SUPPORTED;
	}

	@Override
	public boolean isSupportedSearchType(StructureSearchSpecification specification) {
		return specification.isNoStructureSearch()
				|| specification.isSubstructureSearch()
				|| (specification.isSimilaritySearch()
				&& !specification.isLargestFragmentOnly()
				&& DescriptorConstants.DESCRIPTOR_FFP512.shortName.equals(specification.getDescriptorShortName()))
				|| (specification.isSimilaritySearch()
				&& !specification.isLargestFragmentOnly()
				&& DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName.equals(specification.getDescriptorShortName()))
				|| (specification.isExactSearch()
				&& !specification.isLargestFragmentOnly());
	}
}
