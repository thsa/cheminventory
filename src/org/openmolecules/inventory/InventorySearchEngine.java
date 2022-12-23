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

import com.actelion.research.chem.StructureSearch;
import com.actelion.research.chem.StructureSearchController;
import com.actelion.research.chem.StructureSearchSpecification;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeMap;

public class InventorySearchEngine implements ConfigurationKeys,InventoryServerConstants {
	private static final int MAX_SSS_MATCHES = 0;		// no limit
	private static final int MAX_NON_SSS_MATCHES = 0;	// no limit

	private static final int MAX_ATOMS = 256;

	private static final int RESULT_STRUCTURE_COLUMNS = 3;
	private static final int RESULT_COLUMN_IDCODE = 0;
	private static final int RESULT_COLUMN_COORDS2D = 1;
	private static final int RESULT_COLUMN_FFP512 = 2;

	private InMemoryData mData;
	private ResultBuilder mResultBuilder;
	private TreeMap<String,QueryColumn> mQueryColumnMap;
	private byte[] mTemplate;

	public InventorySearchEngine(InMemoryData data, ResultBuilder resultBuilder) {
		mData = data;
		mResultBuilder = resultBuilder;
		compileQueryColumns();
		}

	public String getTableSpecification() {
		StringBuilder erm = new StringBuilder();
		for (AlphaNumTable table:mData.getTables()) {
			erm.append(table.getSpecification());
			erm.append('\n');
			}
		return erm.toString();
		}

	public InMemoryData getInMemoryData() {
		return mData;
		}

	private void compileQueryColumns() {
		mQueryColumnMap = new TreeMap<>();
		for (AlphaNumTable table: mData.getTables()) {
			for (int column=0; column<table.getColumnCount(); column++) {
				int type = table.getColumnType(column);
				if (type == ConfigurationKeys.COLUMN_TYPE_NUM
				 || type == ConfigurationKeys.COLUMN_TYPE_TEXT) {
					String key = table.getAliasName()+"."+table.getColumnName(column);
					mQueryColumnMap.put(key, new QueryColumn(table, column, type));
				}
			}
		}
	}

	/**
	 * @return array of all query column names in the form '<table-alias>.<column-name>'
	 */
	public String[] getQueryColumnNames() {
		return mQueryColumnMap.keySet().toArray(new String[0]);
	}

	/**
	 * @return Collection of all query columns (queryable database columns)
	 */
	public Collection<QueryColumn> getQueryColumns() {
		return mQueryColumnMap.values();
	}

	public byte[][][] getMatchingRowsAsBytes(TreeMap<String,Object> query) throws SearchEngineException {
		return createSearchTask(query).getMatchingRowBytes();
		}

	/**
	 * @param query
	 * @return
	 * @throws SearchEngineException
	 */
	public String searchIDs(TreeMap<String,Object> query) throws SearchEngineException {
		return createSearchTask(query).getMatchingBottleIDs();
		}

	public int printResultTable(TreeMap<String,Object> query, PrintStream body) throws SearchEngineException {
		return createSearchTask(query).printResultRows(body);
		}

	private SearchTask createSearchTask(TreeMap<String,Object> query) throws SearchEngineException {
		ArrayList<QueryColumn> queryColumns = new ArrayList<>();
		ArrayList<String> queryCriterions = new ArrayList<>();

		AlphaNumTable table = mData.getTable((String)query.get(QUERY_PARAMETER_TABLE));

		for (String key:mQueryColumnMap.keySet()) {
			String value = (String)query.get(key);
			if (value != null && value.length() != 0) {
				queryColumns.add(mQueryColumnMap.get(key));
				queryCriterions.add(value);
			}
		}

		int maxRows = Integer.MAX_VALUE;
		try {
			String max = (String)query.get(QUERY_MAX_ROWS);
			if (max != null)
				maxRows = Integer.parseInt(max);
			}
		catch (NumberFormatException nfe) {}

		StructureSearchSpecification ssSpec = (StructureSearchSpecification)query.get(QUERY_STRUCTURE_SEARCH_SPEC);
		if (table == null && ssSpec == null)
			throw new SearchEngineException("No structure search defined.");
		if (table != null && ssSpec != null)
			ssSpec = null;

		// default is true (unless the query was built from URL parameters)
		boolean includeStructureColumns = !"false".equals(query.get(PARAMETER_WITH_STRUCTURE));

		return new SearchTask(table, ssSpec, includeStructureColumns, maxRows,
				queryColumns.toArray(new QueryColumn[0]), queryCriterions.toArray(new String[0]));
		}

	public byte[] getTemplate() {
		return mTemplate;
	}

	private class SearchTask implements StructureSearchController {
		private StructureSearchSpecification mSSSpec;
		private AlphaNumTable mSearchedSingleTable;   // in case of alphanumerical search on one table only
		private boolean mIncludeStructureColumns;
		private int mMaxRows;
		private String[] mQueryCriterion;
		private float[] mQueryLow,mQueryHigh;
		private byte[][] mQueryText;
		private boolean[] mQueryTextIsNot;
		private int[] mQueryColumnIndex,mQueryColumnType,mForeignKeyIndex;

		/**
		 * This constructs a standard structure search task on the joined tables with predefined result structure
		 * @param searchedSingleTable null, if using a standard structure search on the predefined result structure
		 * @param structureSearchSpec null, if search is alphanumerical on a single table
		 * @param maxRows
		 * @param queryColumn
		 * @param queryCriteria
		 */
		public SearchTask(AlphaNumTable searchedSingleTable, StructureSearchSpecification structureSearchSpec,
		                  boolean includeStructureColumns, int maxRows, QueryColumn[] queryColumn, String[] queryCriteria) {
			mSSSpec = structureSearchSpec;
			mSearchedSingleTable = searchedSingleTable;
			mIncludeStructureColumns = includeStructureColumns;
			mMaxRows = maxRows;
			mQueryCriterion = queryCriteria;

			mQueryColumnIndex = new int[mQueryCriterion.length];
			mQueryColumnType = new int[mQueryCriterion.length];
			mForeignKeyIndex = new int[mQueryCriterion.length];
			AlphaNumTable table = mSearchedSingleTable != null ? mSearchedSingleTable : mData.getBottleTable();
			for (int i=0; i<mQueryCriterion.length; i++) {
				mQueryColumnIndex[i] = queryColumn[i].getColumnIndex();
				mQueryColumnType[i] = queryColumn[i].getColumnType();
				mForeignKeyIndex[i] = table.getForeignKeyIndex(queryColumn[i].getTable().getAliasName());
			}

			mQueryLow = new float[mQueryCriterion.length];
			mQueryHigh = new float[mQueryCriterion.length];
			mQueryText = new byte[mQueryCriterion.length][];
			mQueryTextIsNot = new boolean[mQueryCriterion.length];
			for (int i=0; i<mQueryCriterion.length; i++) {
				if (queryColumn[i].getColumnType() == COLUMN_TYPE_NUM)
					parseNumericalCriterion(mQueryCriterion[i], i);
				else if (queryColumn[i].getColumnType() == COLUMN_TYPE_TEXT)
					parseTextCriterion(mQueryCriterion[i], i);
			}
		}

		private void parseNumericalCriterion(String criterion, int criterionIndex) {
			mQueryLow[criterionIndex] = -Float.MAX_VALUE;
			mQueryHigh[criterionIndex] = Float.MAX_VALUE;
			float min = Float.MIN_VALUE;

			criterion = criterion.replaceAll(" ", "");
			int index = criterion.indexOf('-');
			if (index == -1) {
				if (criterion.startsWith("<="))
					try { mQueryHigh[criterionIndex] = Float.parseFloat(criterion.substring(2)); } catch (NumberFormatException nfe) {}
				else if (criterion.startsWith("<"))
					try { mQueryHigh[criterionIndex] = Float.parseFloat(criterion.substring(1)) - min; } catch (NumberFormatException nfe) {}
				else if (criterion.startsWith(">="))
					try { mQueryLow[criterionIndex] = Float.parseFloat(criterion.substring(2)); } catch (NumberFormatException nfe) {}
				else if (criterion.startsWith(">"))
					try { mQueryLow[criterionIndex] = Float.parseFloat(criterion.substring(1)) + min; } catch (NumberFormatException nfe) {}
				else
					try { mQueryLow[criterionIndex] = mQueryHigh[criterionIndex] = Float.parseFloat(criterion); } catch (NumberFormatException nfe) {}
			}
			else {
				try { mQueryLow[criterionIndex] = Float.parseFloat(criterion.substring(0, index)); } catch (NumberFormatException nfe) {}
				try { mQueryHigh[criterionIndex] = Float.parseFloat(criterion.substring(index+1)); } catch (NumberFormatException nfe) {}
			}
		}

		private void parseTextCriterion(String criterion, int criterionIndex) {
			if (criterion.startsWith("!")) {
				criterion = criterion.substring(1);
				mQueryTextIsNot[criterionIndex] = true;
			}
			mQueryText[criterionIndex] = criterion.getBytes();
		}

		@Override
		public boolean rowQualifies(int row) {
			AlphaNumTable table = mSearchedSingleTable != null ? mSearchedSingleTable : mData.getBottleTable();
			AlphaNumRow bottleRow = table.getRow(row);
			for (int i=0; i<mQueryCriterion.length; i++) {
				if (mQueryColumnType[i] == COLUMN_TYPE_NUM) {
					float value = (mForeignKeyIndex[i] == -1) ? bottleRow.getFloat(mQueryColumnIndex[i])
							: bottleRow.getReferencedRow(mForeignKeyIndex[i]).getFloat(mQueryColumnIndex[i]);
					if (Float.isNaN(value) || value < mQueryLow[i] || value > mQueryHigh[i])
						return false;
				}
				else if (mQueryColumnType[i] == COLUMN_TYPE_TEXT) {
					byte[] value = (mForeignKeyIndex[i] == -1) ? bottleRow.getData(mQueryColumnIndex[i])
							: bottleRow.getReferencedRow(mForeignKeyIndex[i]).getData(mQueryColumnIndex[i]);
					boolean match = false;
					if (value != null) {
						for (int j=0; j<value.length-mQueryText[i].length+1; j++) {
							match = true;
							for (int k=0; k<mQueryText[i].length; k++) {
								if (value[j+k] != mQueryText[i][k]) {
									match = false;
									break;
								}
							}
							if (match)
								break;
						}
					if (match == mQueryTextIsNot[i])
						return false;
					}
				}
			}

			return true;
			}

		/**
		 * Compiles a list of matching rows for an alphanumerical search on one table
		 * @return
		 */
		private int[] getSingleTableMatchingRowIndexes() {
			int[] hitIndex = new int[mSearchedSingleTable.getRowCount()];
			int count = 0;
			for (int i = 0; i<mSearchedSingleTable.getRowCount(); i++)
			    if (rowQualifies(i))
					hitIndex[count++] = i;

			return Arrays.copyOf(hitIndex, count);
		}

		public byte[][][] getMatchingRowBytes() throws SearchEngineException {
			if (mSSSpec != null) {
				StructureSearch search = new StructureSearch(mSSSpec, mData, this, null, null);
				search.setMatchLimit(Math.min(mMaxRows, MAX_SSS_MATCHES), Math.min(mMaxRows, MAX_NON_SSS_MATCHES));
				int[] hitIndexes = search.start();
				if (hitIndexes == null)
					return null;

				if (MAX_SSS_MATCHES != 0 && mSSSpec.isSubstructureSearch() && hitIndexes.length > MAX_SSS_MATCHES)
					throw new SearchEngineException("Sub-structure search hit limit exceeded.\nTry to make your search more specific.");
				if (MAX_NON_SSS_MATCHES != 0 && !mSSSpec.isSubstructureSearch() && hitIndexes.length > MAX_NON_SSS_MATCHES)
					throw new SearchEngineException("Structure search hit limit exceeded.\nTry to make your search more specific.");

				return mResultBuilder.buildResult(hitIndexes);
				}

			if (mSearchedSingleTable != null) {
				int[] hitIndexes = getSingleTableMatchingRowIndexes();
				return new SingleTableResultBuilder(mSearchedSingleTable).buildResult(hitIndexes, mIncludeStructureColumns);
				}

			return null;
			}

		public String getMatchingBottleIDs() throws SearchEngineException {
			if (mSSSpec != null) {
				StructureSearch search = new StructureSearch(mSSSpec, mData, this, null, null);
				search.setMatchLimit(Math.min(mMaxRows, MAX_SSS_MATCHES), Math.min(mMaxRows, MAX_NON_SSS_MATCHES));
				int[] hitIndexes = search.start();
				if (hitIndexes == null)
					return null;

				if (MAX_SSS_MATCHES != 0 && mSSSpec.isSubstructureSearch() && hitIndexes.length > MAX_SSS_MATCHES)
					throw new SearchEngineException("Sub-structure search hit limit exceeded.\nTry to make your search more specific.");
				if (MAX_NON_SSS_MATCHES != 0 && !mSSSpec.isSubstructureSearch() && hitIndexes.length > MAX_NON_SSS_MATCHES)
					throw new SearchEngineException("Structure search hit limit exceeded.\nTry to make your search more specific.");

				AlphaNumTable bottleTable = mData.getBottleTable();
				int pkColumn = bottleTable.getPrimaryKeyColumn();

				StringBuilder result = new StringBuilder();
				for (int hitIndex:hitIndexes) {
					result.append(new String(bottleTable.getRow(hitIndex).getData(pkColumn)));
					result.append("\n");
					}

				return result.toString();
				}

			if (mSearchedSingleTable != null) {
				int[] hitIndexes = getSingleTableMatchingRowIndexes();
				int pkColumn = mSearchedSingleTable.getPrimaryKeyColumn();
				StringBuilder result = new StringBuilder();
				for (int hitIndex:hitIndexes) {
					result.append(new String(mSearchedSingleTable.getRow(hitIndex).getData(pkColumn)));
					result.append("\n");
					}

				return result.toString();
				}

			return null;
			}

		public int printResultRows(PrintStream body) throws SearchEngineException {
			if (mSSSpec != null) {
				StructureSearch search = new StructureSearch(mSSSpec, mData, this, null, null);
				search.setMatchLimit(Math.min(mMaxRows, MAX_SSS_MATCHES), Math.min(mMaxRows, MAX_NON_SSS_MATCHES));
				int[] hitIndexes = search.start();
				if (hitIndexes == null)
					return 0;

				if (MAX_SSS_MATCHES != 0 && mSSSpec.isSubstructureSearch() && hitIndexes.length > MAX_SSS_MATCHES)
					throw new SearchEngineException("Sub-structure search hit limit exceeded.\nTry to make your search more specific.");
				if (MAX_NON_SSS_MATCHES != 0 && !mSSSpec.isSubstructureSearch() && hitIndexes.length > MAX_NON_SSS_MATCHES)
					throw new SearchEngineException("Structure search hit limit exceeded.\nTry to make your search more specific.");

				mResultBuilder.printResult(hitIndexes, body, mIncludeStructureColumns);

				return hitIndexes.length;
				}

			if (mSearchedSingleTable != null) {
				int[] hitIndexes = getSingleTableMatchingRowIndexes();
				new SingleTableResultBuilder(mSearchedSingleTable).printResult(hitIndexes, body, mIncludeStructureColumns);
				return hitIndexes.length;
				}

			return 0;
			}
		}
	}
