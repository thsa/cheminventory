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

public interface InventoryServerConstants {
// The SERVER_URL is only used by the client
//	String SERVER_URL = "http://localhost:8092";
	String SERVER_URL = "https://inventory.openmolecules.org";

	String INSTALLATION_PATH = "/opt/inventoryserver/";
//	String INSTALLATION_PATH = "./";
	String LOG_FILE_NAME = INSTALLATION_PATH + "log/inventory_"+System.currentTimeMillis()+".log";

	String REQUEST_HELP = "help";
	String REQUEST_TEMPLATE = "template";
	String QUERY_STRUCTURE_SEARCH_SPEC = "ssspec";

	String QUERY_AMOUNT = "amount";
	String QUERY_PRICE_LIMIT = "price";
	String QUERY_MAX_ROWS = "maxrows";
	String QUERY_MOLWEIGHT = "molweight";

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// the following are the names of individual put/get parameters if the client doesn't use a query object //
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	String PARAMETER_WITH_STRUCTURE = "withidcode"; // whether to include structure columns in text result; default is false

	String PARAMETER_BB_ID = "id";
	String PARAMETER_SMILES = "smiles";
	String PARAMETER_SEARCH_TYPE = "searchType";
	String PARAMETER_THRESHOLD = "threshold";
	String PARAMETER_AMOUNT = "amount";
	String PARAMETER_PRICE_LIMIT = "price";
	String PARAMETER_MAX_ROWS = "maxrows";
	String PARAMETER_MOLWEIGHT = "molweight";
	String SEARCH_TYPE_SSS = "substructure";
	String SEARCH_TYPE_SIM = "similarity";

	// Molport specific non-query parameter names
	String PARAMETER_VERIFIED_AMOUNT = "verifiedAmount";
}
