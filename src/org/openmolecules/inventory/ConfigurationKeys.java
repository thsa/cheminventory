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

public interface ConfigurationKeys {
	//***** Key names used in the test_config.txt file to customize inventory server behaviour *****

	String[] COLUMN_TYPES = { "[text]", "[id]", "[num]", "[date]", "[pk]", "[fk:" };
	String[] SQL_TYPE = { "varchar(255)", "varchar(255) NOT NULL UNIQUE", "float", "date", "int NOT NULL AUTO_INCREMENT", "int" };
	int COLUMN_TYPE_TEXT = 0; // Text values stored as 'varchar(255)' in the database
	int COLUMN_TYPE_ID = 1;   // Optional: may serve as identifier; stored as 'varchar(255) NOT NULL UNIQUE'
	int COLUMN_TYPE_NUM = 2;  // Numerical values stored as 'float' in the database
	int COLUMN_TYPE_DATE = 3; // Date values stored as 'date' in the database
	int COLUMN_TYPE_PK = 4;   // Mandatory once per table: An integer value serving as primary key
	int COLUMN_TYPE_FK = 5;   // Zero or more times per table: An integer value serving as foreign key

	String MAX_STRUCTURE_SEARCH_HITS = "maxStructureSearchHits";
	String MAX_NON_STRUCTURE_SEARCH_HITS = "maxNonStructureSearchHits";

	String CONNECT_STRING = "connectString";
	String DATABASE_NAME = "db_name";
	String DATABASE_USER = "db_user";
	String DATABASE_PASSWORD = "db_password";
	String ADMIN_USER = "admin_user";
	String ADMIN_HASH = "admin_hash";
	String COMPOUND_TABLE = "compoundTable";
	String BOTTLE_TABLE = "bottleTable";

	String SUPPORT_TABLES_COUNT = "supportTableCount";
	String SUPPORT_TABLE = "supportTable";

	String RESULT_TABLE = "resultTable";
}
