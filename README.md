## ChemInventory
*ChemInventory* is a simple and flexible HTTP based chemical inventory server. It allows storing
and tracking of chemicals including their structures, bottles, locations, and suppliers.
It provides fast structure search by substructure-, exact match-, and similarity-search.
It uses *OpenChemLib* (a Java based cheminformatics framework), *Simple-Framework* (a robust
Java based HTTP(S) engine), and MySQL.

When up and running, *ChemInventory* server listens on a dedicated port for HTTP based search requests.
It can be be used as virtual host of a web server to be accessible under a web-server subdomain
(e.g. https://cheminventory.organisation.org).
*ChemInventory* is meant to work in combination with a Web-based and/or Java-based front end
application that let's end users store, edit and search bottles of chemicals. These client
applications are not part of this project, yet.

### Dependencies
*ChemInventory* requires JRE 8 or newer, OpenChemLib, SimpleFramework, and MySQL (or ProstgreSQL).
All dependencies are included with this project in the lib folder.

### How to download the project
```bash
git clone https://github.com/thsa/cheminventory.git
```

### Create Inventory database, insert test data
Provided, you have installed MySQL-Server, you may run the following on a Linux computer
to create a new inventory test database including test data.
```bash
./create_db
```
For information about how to install MySQL or how to create an empty inventory database
check the comments in the create_db file. For a different database structure, i.e.
different tables with different columns, edit the *config.txt* file and use inventory server
to create a new table_creation script. Launch the server with the updated *config.txt* and
it will work with the new entity relationship model.

### How build the service
```bash
./build_all
```

### How to run the service
```bash
java -jar inventoryserver.jar -c test_config.txt
```

### How to access the service
To display the help page open a web browser and access the server's URL
```
http://localhost:8092/?what=help
```
An example for a little more complex query would be
```
http://localhost:8092/?what=query&smiles=c1ccccc1O&b.current_amount=>5&s.name=ABC
```
This will retrieve all bottles from supplier 'ABC' with a phenol substructure and a current amount above 5 g or ml.


### License
*ChemInventory*. Copyright (C) 2022 Thomas Sander, openmolecules.org

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
