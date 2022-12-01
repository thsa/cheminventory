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

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.SmilesParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.StructureSearchSpecification;
import org.openmolecules.comm.ServerTask;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.TreeMap;

public class InventoryTask extends ServerTask implements ConfigurationKeys,InventoryServerConstants {
	private static BufferedWriter sLogWriter;

	private InventorySearchEngine mSearchEngine;

	public InventoryTask(InventorySearchEngine searchEngine) {
		mSearchEngine = searchEngine;
	}

    @Override
    public void performTask() {
    	String what = getRequestText(KEY_REQUEST);
    	if (what == null) {
			createErrorResponse("Undefined request. For help set parameter 'what' to 'help'");
			return;
    		}

	    if (what.equals(REQUEST_HELP)) {
		    createPlainResponse(
		    		"Help page of the REST-API of the openmolecules.org Chemical-Inventory search engine\n\n"
		          + "  (For Java clients there is a more efficient API that allows\n"
				  + "   sending complex search specifications as single Java object)\n\n"
		          + "To use the REST-API you may send HTTP(S) GET or POST requests to the server,\n"
				  + "which runs the chem-inventory service, e.g. http://localhost:8092.\n"
		          + "You may attach key-value pairs as parameters to define your request:\n\n"
		          + "key 'what':\n"
		          + "  value 'help': Returns this help page as text.\n\n"
			      + "  value 'erm': Returns a specification of all tables and columns accessible by this server.\n\n"
			      + "  value 'query': Defines a structure query and requires more parameters:\n"
				  + "    key 'smiles': Optional parameter to attach a structure search to the query.\n"
				  + "      value: valid SMILES code of a chemical structure for substructure or similarity search.\n"
			      + "        key 'searchType': optional parameter to define the search type. If not given, 'substructure' is assumed.\n"
				  + "          value: Currently it must be 'substructure' or 'similarity'.\n"
				  + "        key 'threshold' Optional numerical similarity cut-off. If not given '80' is assumed.\n"
				  + "          value: numerical fractional or percent value, e.g. '0.75' or '75', to return all structures\n"
				  + "                 with a higher similarity to the query structure than 75%.\n"
				  + "    key 'withidcode': Optional parameter to define, whether the result shall include idcode,coords,fragfp,skelspheres.\n"
				  + "      value: 'true' or 'false'. The default is 'false'.\n"
				  + "    key 'table': Optional parameter to search a single table rather than the server default.\n"
				  + "      value: SQL table name. If 'table' is used, then result rows include all columns of that table,\n"
				  + "                             column names for <numcol> and <numtext> don't require a the table alias,\n"
				  + "                             and only alphanumerical query criteria can be used (no smiles).\n"
				  + "    key 'maxrows': Optional parameter to limit the number of result rows.\n"
				  + "      value: any integer value, e.g. 1000.\n"
				  + "    Within a query any numerical column in the database may be used as additional criterion:\n"
				  + "    key '<numcol>', where <numcol> is the database table alias followed by '.' and the column name\n"
				  + "      value: float value, leading '<' or '>' or ranges as '150-250' are accepted.\n"
				  + "      Valid values for <numcol>: "+getQueryColumnNames(COLUMN_TYPE_NUM)+"\n"
				  + "    Within a query any text column in the database may be used as additional criterion:\n"
				  + "    key '<textcol>', where <textcol> is the database table alias followed by '.' and the column name\n"
				  + "      value: text string, which must be a substring of a row's column content for the row to be a match.\n"
				  + "      Valid values for <textcol>: "+getQueryColumnNames(COLUMN_TYPE_TEXT)+"\n"
			      + "      (specify mixture of <numcol> and <textcol> key-value pairs to define matching rows)\n\n"
				  + "  value 'login': Returns a token, which is needed for inserting, changing, or deleting rows in the database.\n"
				  + "    key 'user': Valid user-ID.\n"
				  + "    key 'password': Valid password.\n"
				  + "  value 'logout': Invalidates the token returned by the previous 'login' request.\n\n"
				  + "  value 'insert': Inserts a new row into the specified table of the database.\n"
				  + "    key 'token': A valid token returned by a previous 'login' request.\n"
				  + "    key 'table': The name of the table name in which to insert a new row.\n"
				  + "    key '<column>', where <column> is SQL column name: column value for the new row.\n"
				  + "      (specify a key-value pair for every column except for null values and for the [pk] column)\n\n"
				  + "  value 'update': Updates an existing row of the specified table of the database.\n"
				  + "    key 'token': A valid token returned by a previous 'login' request.\n"
				  + "    key 'table': The name of the table name in which to row shall be updated.\n"
				  + "    key '<column>', where <column> is the SQL column name: new column value for the existing row.\n"
				  + "      (specify a key-value pair for every column that needs to change and for the [pk] column)\n\n"
				  + "  value 'delete': Deletes an existing row from the specified table of the database.\n"
				  + "    key 'token': A valid token returned by a previous 'login' request.\n"
				  + "    key 'table': The name of the table name in which to row shall be updated.\n"
				  + "    key '<pk-column>', where <pk-column> is the SQL column name of the primary key of the row to be deleted.\n\n"
				  + "Examples (as HTTP(S) GET requests):\n"
				  + "  http(s)://some.server.com/?what=help\n"
			      + "    Get this help page.\n\n"
				  + "  http(s)://some.server.com/?what=query&smiles=c1cncnc1OC\n"
				  + "    Retrieve information about all bottles containing a super-structure of 6-Methoxy-pyrimidine.\n\n"
				  + "  http(s)://some.server.com/?what=query&smiles=c1ccccc1O&current_amount=>5000&s.name=ABC\n"
				  + "    Retrieve all bottles from supplier 'ABC' with a phenol substructure and a current amount above 5000 mg.\n\n"
					);
		    return;
		    }

	    if (what.equals(REQUEST_ERM)) {
	    	createTextResponse(mSearchEngine.getTableSpecification());
	    	return;
		    }

	    if (what.equals(REQUEST_LOGIN)) {
		    String user = getRequestText(KEY_USER);
		    String password = getRequestText(KEY_PASSWORD);
	    	if (user == null || password == null) {
			    createErrorResponse("User-ID or password missing.");
			    return;
		        }
	    	if (Authorizer.getInstance().isBlocked(getClientIP())) {
			    createErrorResponse("Too many login tries. Try again later.");
			    return;
			    }
			String token = Authorizer.getInstance().createToken(user, password);
	    	if (token == null) {
			    createErrorResponse("Invalid user or password.");
			    return;
			    }
	    	createTextResponse(token);
	    	return;
		    }

	    if (what.equals(REQUEST_INSERT)
		 || what.equals(REQUEST_UPDATE)
		 || what.equals(REQUEST_DELETE)) {
		    String token = getRequestText(PARAMETER_TOKEN);
		    if (token == null) {
			    createErrorResponse("Missing token to change data.");
			    return;
		    }
		    if (!Authorizer.getInstance().isValidToken(token)) {
			    createErrorResponse("Invalid token.");
			    return;
		    }
		    String tableName = getRequestText(PARAMETER_TABLE);
		    if (tableName == null) {
			    createErrorResponse("Missing table name.");
			    return;
		    }
			AlphaNumTable table = mSearchEngine.getInMemoryData().getTable(tableName);
		    if (table == null) {
			    createErrorResponse("Table '"+tableName+"' not found.");
			    return;
		    }
		    String primaryKey = null;
			if (!what.equals(REQUEST_INSERT)) {
				String primaryKeyName = table.getColumnName(table.getPrimaryKeyColumn());
				primaryKey = getRequestText(primaryKeyName);
				if (primaryKey == null) {
					createErrorResponse("Primary key '"+primaryKeyName+"' not defined.");
					return;
				}
			}
			if (what.equals(REQUEST_DELETE)) {
				if (!table.deleteRow(primaryKey))
					createErrorResponse("Could not delete row '"+primaryKey+"' of table '"+tableName+"'.");
				else
					createTextResponse("OK");
				return;
			}
			TreeMap<String,String> columnValueMap = new TreeMap<>();
			for (int i=0; i<table.getColumnCount(); i++) {
				if (table.getColumnType(i) != ConfigurationKeys.COLUMN_TYPE_PK) {
					String value = getRequestText(table.getColumnName(i));
					if (value != null)
						columnValueMap.put(table.getColumnName(i), value);
				}
			}
		    if (columnValueMap.size() == 0) {
			    if (what.equals(REQUEST_INSERT))
				    createErrorResponse("Insert row into '"+tableName+"': No column values found.");
			    else
				    createErrorResponse("Update row of '"+tableName+"': No new column values found.");
			    return;
		    }
		    if (what.equals(REQUEST_INSERT)) {
			    int[] newPrimaryKeyHolder = new int[1];
			    if (!table.insertRow(columnValueMap, newPrimaryKeyHolder))
				    createErrorResponse("Could not insert row into table '"+tableName+"'.");
			    else
				    createTextResponse("OK");
			    return;
		    }
		    else {  // UPDATE
			    if (!table.updateRow(columnValueMap, primaryKey))
				    createErrorResponse("Could not update row '"+primaryKey+"' of table '"+tableName+"'.");
			    else
				    createTextResponse("OK");
			    return;
		    }
	    }

	    if (what.equals(REQUEST_TEMPLATE)) {
			byte[] template = mSearchEngine.getTemplate();
		    try {
			    createObjectResponse(template);
			    }
		    catch (Exception e) {
			    e.printStackTrace();
			    createErrorResponse(e.toString());
			    }
		    return;
		    }

	    if (what.equals(REQUEST_RUN_QUERY)) {
			@SuppressWarnings("unchecked")
			TreeMap<String,Object> query = (TreeMap<String,Object>)getRequestObject(KEY_QUERY);
			try {
			    if (query == null) {
				    query = tryConstructQueryFromParameters();  // this creates any error message itself
				    if (query == null)
					    return;

				    long startmillis = System.currentTimeMillis();

				    createResponseHeader("text/plain");
				    PrintStream body = getResponse().getPrintStream();
				    int resultRowCount = mSearchEngine.printResultTable(query, body);
				    body.close();

				    long millis = System.currentTimeMillis() - startmillis;

				    writeLogEntry(what, resultRowCount+" table rows in "+millis+" ms");
				    return;
			        }
			    else {
				    StructureSearchSpecification ssSpec = (StructureSearchSpecification)query.get(QUERY_STRUCTURE_SEARCH_SPEC);
				    if (ssSpec == null)
					    query.put(QUERY_STRUCTURE_SEARCH_SPEC, new StructureSearchSpecification(StructureSearchSpecification.TYPE_NO_STRUCTURE,
							    null, null, null, 0f));

				    long startmillis = System.currentTimeMillis();
				    byte[][][] result = mSearchEngine.getMatchingRowsAsBytes(query);
				    createObjectResponse(result);
				    long millis = System.currentTimeMillis() - startmillis;

				    writeLogEntry(what, result.length+" rows in "+millis+" ms");
				    }
			    }
		    catch (SearchEngineException e) {
			    createErrorResponse(e.getMessage());
			    writeLogEntry(what, e.getMessage());
			    }
		    catch (Exception e) {
			    e.printStackTrace();
			    createErrorResponse(e.toString());
			    writeLogEntry(what, e.toString());
			    }
			return;
    		}

	    createErrorResponse("Unknown request");
        }

    private String getQueryColumnNames(int type) {
		StringBuilder queryColumnNames = new StringBuilder();
	    for (QueryColumn queryColumn:mSearchEngine.getQueryColumns()) {
	    	if (queryColumn.getColumnType() == type) {
	    		if (queryColumnNames.length() != 0)
				    queryColumnNames.append(", ");
			    queryColumnNames.append(queryColumn.getTable().getAliasName());
			    queryColumnNames.append(".");
			    queryColumnNames.append(queryColumn.getTable().getColumnName(queryColumn.getColumnIndex()));
		    }
	    }
	    return queryColumnNames.toString();
    }

	/**
	 * If the incoming server request comes with plain text parameters (PUT or GET)
	 * rather than with a serialized query object, then this method parses those
	 * individual text parameters and converts them into a new query object.
	 * @return query object
	 */
	private TreeMap<String,Object> tryConstructQueryFromParameters() {
	    String smiles = getRequestText(PARAMETER_SMILES);
	    String searchType = getRequestText(PARAMETER_SEARCH_TYPE);
	    String threshold = getRequestText(PARAMETER_THRESHOLD);
	    String tableName = getRequestText(PARAMETER_TABLE);
		String withStructure = getRequestText(PARAMETER_WITH_STRUCTURE);

		AlphaNumTable table = mSearchEngine.getInMemoryData().getTable(tableName);

	    TreeMap<String,Object> query = new TreeMap<>();

	    query.put(PARAMETER_WITH_STRUCTURE, withStructure == null ? "false" : withStructure);

		if (table != null)
			query.put(PARAMETER_TABLE, tableName);

		for (String queryColumnName:mSearchEngine.getQueryColumnNames()) {
		    String value = getRequestText(queryColumnName);
		    if (value == null && table != null)   // we allow column specification without table alias
		    	value = getRequestText(queryColumnName.substring(1+queryColumnName.indexOf('.')));
		    if (value != null)
				query.put(queryColumnName, value);
	    }

	    if (smiles != null) {
		    StereoMolecule mol = new StereoMolecule();
		    try {
			    new SmilesParser().parse(mol, smiles);
			    mol.setFragment(true);
		        }
		    catch (Exception e) {
			    createErrorResponse("Invalid SMILES:"+e);
			    return null;
		        }

		    byte[][] idcode = new byte[1][];
		    idcode[0] = new Canonizer(mol).getIDCode().getBytes();

		    String descriptorName = "FragFp";
		    int type = StructureSearchSpecification.TYPE_SUBSTRUCTURE;
		    if (searchType != null && !SEARCH_TYPE_SSS.equals(searchType)) {
				if (SEARCH_TYPE_SIM.equals(searchType)) {
					type = StructureSearchSpecification.TYPE_SIMILARITY;
					descriptorName = "SkelSpheres";
					}
			    else {
					createErrorResponse("Search type not recognized");
					return null;
					}
		        }

		    float cutoff = 0.8f;
		    try {
			    if (threshold != null) {
				    cutoff = Float.parseFloat(threshold);
				    if (cutoff > 1f)
					    cutoff /= 100;   // we also support percent values
			        if (cutoff < 0.6f || cutoff > 1f) {
				        createErrorResponse("Similarity threshold is out of range");
				        return null;
				        }
			        }
			    }
		    catch (Exception e) {
			    createErrorResponse("Invalid similarity threshold");
			    return null;
		        }

		    query.put(QUERY_STRUCTURE_SEARCH_SPEC, new StructureSearchSpecification(type, idcode, null, descriptorName, cutoff));
		    }
	    else {
		    query.put(QUERY_STRUCTURE_SEARCH_SPEC, new StructureSearchSpecification(StructureSearchSpecification.TYPE_NO_STRUCTURE, null, null, null, 0f));
		    }

	    return query;
        }

	private void writeLogEntry(final String what, final String message) {
		if (SwingUtilities.isEventDispatchThread()) {
			writeLogEntryInEDT(what+"\t"+message);
			}
		else {
			try {
				SwingUtilities.invokeLater(() -> writeLogEntryInEDT(what+"\t"+message) );
				}
			catch (Exception e) {}
			}
		}

	private void writeLogEntryInEDT(String logEntry) {
		String ip = getClientIP();
		String time = getDateAndTime();
		try {
			if (sLogWriter == null)
				sLogWriter = new BufferedWriter(new FileWriter(LOG_FILE_NAME));

			sLogWriter.append(time);
			sLogWriter.append("\t");
			sLogWriter.append(ip);
			sLogWriter.append("\t");
			sLogWriter.append(logEntry);
			sLogWriter.newLine();
			sLogWriter.flush();

			System.out.println(logEntry);
			}
		catch (IOException ioe) {
			System.out.println(time+"\t"+ip+"\t"+logEntry);
			}
		}
	}
