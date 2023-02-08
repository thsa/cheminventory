USE cheminventory;

CREATE TABLE location (
	location_id int NOT NULL AUTO_INCREMENT,
	name varchar(255) NOT NULL UNIQUE,
	PRIMARY KEY (location_id)
);

CREATE TABLE supplier (
	supplier_id int NOT NULL AUTO_INCREMENT,
	name varchar(255) NOT NULL UNIQUE,
	PRIMARY KEY (supplier_id)
);

CREATE TABLE compound (
	compound_id int NOT NULL AUTO_INCREMENT,
	name varchar(255) NOT NULL,
	cas_no varchar(255),
	molweight int,
	formula varchar(255),
	idcode varchar(255),
	idcoords varchar(255),
	fragfp varchar(255),
	skelspheres varchar(1023),
	PRIMARY KEY (compound_id)
);

CREATE TABLE bottle (
	bottle_id int NOT NULL AUTO_INCREMENT,
	compound_id int NOT NULL,
	location_id int NOT NULL,
	supplier_id int NOT NULL,
	barcode varchar(255) NOT NULL UNIQUE,
	catalog_no varchar(255),
	amount_unit varchar(16),
	initial_amount float NOT NULL,
	current_amount float NOT NULL,
	purity float,
	density float,
	tara float,
	reg_date date,
	comment varchar(255),
	PRIMARY KEY (bottle_id),
	FOREIGN KEY (compound_id) REFERENCES compound(compound_id),
	FOREIGN KEY (location_id) REFERENCES location(location_id),
	FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id)
);

