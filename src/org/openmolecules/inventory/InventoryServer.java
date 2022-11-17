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

import org.openmolecules.comm.ServerCommunicator;
import org.openmolecules.comm.ServerTaskFactory;

import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

public class InventoryServer implements ConfigurationKeys {
	private static final int DEFAULT_PORT = 8092;
	private static final int DEFAULT_THREAD_COUNT = 4;
	private static final String CONFIG_FILE = "/opt/inventoryserver/test_config.txt";

	public static void main(String[] args) {
		System.out.println("Inventory Server 1.0 (Java HTTP version)");
		System.out.println("(C) 2022 Thomas Sander, Therwilerstr. 41, 4153 Reinach, Switzerland");
		System.out.println("Launch Time: "+getDateAndTime());

		if (!parseArguments(args))
			showUsage();
		}

	private static void showUsage() {
		System.out.println("Build a table creation script from the configuration with:");
		System.out.println("java -cp inventoryserver.jar org.openmolecules.inventory.InventoryServer -tcs [-c path]");
		System.out.println("Launch the server with:");
		System.out.println("java -cp inventoryserver.jar org.openmolecules.inventory.InventoryServer [-p port] [-c path] [-s maxRequests] [-dbs]");
		System.out.println("  -p  Default port is "+DEFAULT_PORT+". Use option -p to choose a different port.");
		System.out.println("  -c  Alternative config file path. Default is '/opt/inventoryserver/test_config.txt'.");
		System.out.println("  -s  Maximum number of simultaneously handled requests (default:"+DEFAULT_THREAD_COUNT+").");
		System.out.println("      If this is 1, requests are handled synchronously rather than in multiple threads.");
		}

	private static String getDateAndTime() {
	    return new SimpleDateFormat("d-MMM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
		}

	private static boolean parseArguments(String[] args) {
		int port = DEFAULT_PORT;
		int threadCount = DEFAULT_THREAD_COUNT;
		String configFilePath = CONFIG_FILE;
		boolean createTCS = false;
//		boolean isTest = false;

		for (int i=0; i<args.length; i++) {
			if (args[i].equals("-p") && args.length > i+1) {
				try {
					port = Integer.parseInt(args[++i]);
					continue;
					}
				catch (NumberFormatException e) {
					return false;
					}
				}
			if (args[i].equals("-c") && args.length > i+1) {
				configFilePath = args[++i];
				continue;
				}
			if (args[i].equals("-s") && args.length > i+1) {
				try {
					threadCount = Integer.parseInt(args[++i]);
					continue;
					}
				catch (NumberFormatException e) {
					return false;
					}
				}
			if (args[i].equals("-tcs")) {
				createTCS = true;
				continue;
				}
//			if (args[i].equals("-t")) {
//				isTest = true;
//				continue;
//				}
			return false;
			}

		try {
			Properties config = new Properties();
			File configFile = new File(configFilePath);
			if (!configFile.exists()) {
				System.out.println("ERROR: Config file not found: " + configFilePath);
				return false;
				}

			config.load(new FileReader(configFile));
			String connectString = config.getProperty(CONNECT_STRING);
			if (connectString == null || connectString.length() == 0) {
				System.out.println("ERROR: No '"+CONNECT_STRING+"' found in config file.");
				return false;
				}

			InMemoryData data = new InMemoryData(config);
			if (createTCS) {
				data.createTableCreationScript();
				return true;
			}

			System.out.println("Loading inventory database using ' "+connectString+"'...");
			if (!data.load()) {
				System.out.println("ERROR: Could not load database content.");
				return false;
				}

			final ResultBuilder resultBuilder = new ResultBuilder(data);
			if (!resultBuilder.initialize(config)) {
				System.out.println("ERROR: Could not initialize result builder.");
				return false;
			}

			final InventorySearchEngine searchEngine = new InventorySearchEngine(data, resultBuilder);

			ServerTaskFactory factory = () -> new InventoryTask(searchEngine);

			ServerCommunicator.initialize(factory, threadCount, port);

			System.out.println("listening on port "+port+" (thread pool size:"+threadCount+")"+" ...");
			System.out.println();
			}
		catch (Exception e) {
			e.printStackTrace();
			}

		return true;
		}
	}
