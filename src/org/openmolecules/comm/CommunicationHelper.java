/*
 * Copyright 2022, Thomas Sander, openmolecules.org
 *
 * This file is part of the Simple-Server, a light-weight extension of Simpleframework by Niall Gallagher.
 *
 * Simple-Server is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Simple-Server is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Simple-Server.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package org.openmolecules.comm;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class CommunicationHelper implements CommunicationConstants {
	private static final String USER_AGENT = "Mozilla/5.0";

	public static Object decode(String text) {
        Object decoded = null;
        ByteArrayInputStream byteStream = new ByteArrayInputStream(text.getBytes());
        Base64.InputStream base64Stream = new Base64.InputStream(byteStream);
        ZipInputStream zipStream = new ZipInputStream(base64Stream);
        try {
            zipStream.getNextEntry();
            ObjectInputStream objectStream = new ObjectInputStream(zipStream);
            decoded = objectStream.readObject();
            objectStream.close();
            }
        catch (ClassNotFoundException e) {
        	e.printStackTrace();
            }
        catch (IOException e) {
        	e.printStackTrace();
            }
        return decoded;
        }

    public static String encode(Serializable object) {
        String encoded;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        Base64.OutputStream base64Stream = new Base64.OutputStream(byteStream);
        ZipOutputStream zipStream = new ZipOutputStream(base64Stream);
        try {
            zipStream.putNextEntry(new ZipEntry("z"));
            ObjectOutputStream objectStream = new ObjectOutputStream(zipStream);
            objectStream.writeObject(object);
            objectStream.flush();
            zipStream.closeEntry();
            encoded = byteStream.toString();
            objectStream.close();
            }
        catch (IOException e) {
        	e.printStackTrace();
            encoded = null;
            }
        return encoded;
        }

	public static String sendGet(String url) throws Exception {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
 
		// optional default is GET
		con.setRequestMethod("GET");
 
		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
 
//		int responseCode = con.getResponseCode();	// 200 or 401
 
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();
 
		while ((inputLine = in.readLine()) != null)
			response.append(inputLine);

		in.close();
 
		return response.toString();
		}

	/**
	 * @param url
	 * @param params e.g. what=molfile&name=Acetone
	 * @throws Exception
	 */
	public static String sendPost(String url, String params) throws Exception {
		HttpsURLConnection con = (HttpsURLConnection)new URL(url).openConnection();
 
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
//		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
 
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(params);
		wr.close();
 
//		int responseCode = con.getResponseCode();	// 200 or 401
 
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();
 
		while ((inputLine = in.readLine()) != null)
			response.append(inputLine);

		in.close();
 
		return response.toString();
		}
	}
