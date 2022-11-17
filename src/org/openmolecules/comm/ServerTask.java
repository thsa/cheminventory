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

import org.simpleframework.http.Query;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public abstract class ServerTask extends CommunicationHelper implements CommunicationConstants,Runnable {
    private Request mRequest;
	private Response mResponse;

	public ServerTask() {
		}

	public void setRequest(Request request) {
		mRequest = request;
		}

	public void setResponse(Response response) {
		mResponse = response;
		}

	public Request getRequest() {
		return mRequest;
		}

	public Response getResponse() {
		return mResponse;
		}

	public String getClientIP() {
		// if we have a proxy in-between (e.g. Apache mod_proxy)
		// the client address is the one of the proxy and the original client
		// address is added to the header as value of key "X-Forwarded-For"
		String forwardedIP = mRequest.getValue("X-Forwarded-For");
		if (forwardedIP != null && forwardedIP.length() != 0)
			return forwardedIP;

		InetAddress address = mRequest.getClientAddress().getAddress();
		return (address == null) ? "unresolved-IP" : address.getHostAddress();
		}

    public String getDateAndTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(new Date());
    	}

	/**
	 * Override this in order to return a more specific status
	 * @return
	 */
	public String getStatus() {
		return "alive";
		}

    /**
	 * Performs the actual work of this ServerTask, i.e. provides a proper HTTP header
	 * and calls then performTask() of the derived class to create the body of the response.
	 */
    @Override
    public void run() {
//    	System.out.println("#######################################");
//    	System.out.println(mRequest.getHeader());
//    	System.out.println("#######################################");

		if (REQUEST_GET_STATUS.equals(getRequestText(KEY_REQUEST))) {
			createTextResponse(getStatus());
			return;
			}

		performTask();
    	}

    /**
     * Retrieves a Java String that was attached to the HTTP request.
     * For other serialized objects an encoded String representation is returned.
     * For directly getting the decoded Java object use getRequestObject().
     * @param key
     * @return
     */
    public String getRequestTextAllowEmpty(String key) {
    	String value = mRequest.getValue(key);
    	if (value == null)
    		value = mRequest.getQuery().get(key);
    	return value;
    	}

	/**
	 * Retrieves a Java String that was attached to the HTTP request.
	 * For other serialized objects an encoded String representation is returned.
	 * Empty strings are returned as null.
	 * For directly getting the decoded Java object use getRequestObject().
	 * @param key
	 * @return
	 */
	public String getRequestText(String key) {
		String value = getRequestTextAllowEmpty(key);
		return (value == null || value.length() == 0) ? null : value;
		}

	/**
     * Retrieves a Java List<String> that was attached to the HTTP request.
     * @param key
     * @return
     */
    public List<String> getRequestTextArray(String key) {
    	Query query = mRequest.getQuery();
    	return query.getAll(key);
    	}

    /**
     * Retrieves a Java object that was serialized and attached to the HTTP request.
     * It is properly decoded but neets to be cast to the correct type.
     * @param key
     * @return
     */
    public Object getRequestObject(String key) {
    	String value = getRequestText(key);
    	return (value == null) ? null : decode(value);
    	}

    public void createResponseHeader(String contentType) {
    	long time = System.currentTimeMillis();
        mResponse.setValue("Content-Type", contentType);
        mResponse.setValue("Server", "openmolecules AppServer 1.0");
        mResponse.setValue("Access-Control-Allow-Origin", "*");
        mResponse.setDate("Date", time);
        mResponse.setDate("Last-Modified", time);
    	}
    
    /**
     * Creates the response header of a file
     * @param contentType
     * @param fileName
     * @param contentLength
     */
    private void createFileResponseHeader(String contentType, String fileName, int contentLength) {
    	createResponseHeader(contentType);
    	mResponse.setValue("Expires", "0");
    	mResponse.setValue("Cache-Control", "private");
    	mResponse.setValue("Pragma", "private");
    	mResponse.setValue("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
    	mResponse.setValue("Content-Disposition", "inline; filename=" + fileName);
    	mResponse.setValue("Content-Transfer-Encoding", "binary");
    	mResponse.setValue("Content-Length", Integer.toString(contentLength));
    }

    /**
	 * Creates header and body of an error response to a client request.
	 * @param message short plain text message that describes the error condition
	 */
    public void createErrorResponse(String message) {
    	createResponseHeader("text/plain");

    	try {
            PrintStream body = mResponse.getPrintStream();
            body.println(BODY_ERROR+":"+message);
            body.close();
            }
        catch (IOException e) {
            e.printStackTrace();
            }
        }
    
    /**
     * Creates header and body of a file response to a client request.
     * @param message Content of the file
     * @param fileName Name of the file
     */
    public void createFileResponse(String message, String fileName) {
    	createFileResponseHeader("application/octet-stream", fileName, message.length());

    	try {
            PrintStream body = mResponse.getPrintStream();
            body.println(message);
            body.close();
            }
        catch (IOException e) {
            e.printStackTrace();
            }  	
    	}

    /**
	 * Creates header and body of a plain text response to a client request.
	 * @param message plain text message that is written to the body of the result
	 */
    public void createPlainResponse(String message) {
    	createResponseHeader("text/plain");

    	try {
            PrintStream body = mResponse.getPrintStream();
            body.println(message);
            body.close();
            }
        catch (IOException e) {
            e.printStackTrace();
            }
        }

    /**
	 * Creates header and body of a text response to a client request.
	 * This works like createPlainResponse(), however, precedes the message text
	 * with a tag that indicates to the receiver that the request was successfully handled.
	 * @param message plain text message as successful result to a server request
	 */
    public void createTextResponse(String message) {
    	createPlainResponse(BODY_MESSAGE+":"+message);
        }

    /**
	 * Creates header and body of a text response to a client request.
	 * The object is encoded before being written into the body of the HTTP response.
	 * @param object serializable object as a successful result to a server request
	 */
    public void createObjectResponse(Serializable object) {
    	createResponseHeader("text/plain");

    	try {
            PrintStream body = mResponse.getPrintStream();
            body.println(BODY_OBJECT+":"+encode(object));
            body.close();
            }
        catch (IOException e) {
            e.printStackTrace();
            }
        }

    /**
	 * Creates header and body of an image response to a client request.
	 * @param image image as successful result to a server request
	 * @param format format string to be used for ImageIO.write(), e.g. "png","jpeg"
	 */
    public void createImageResponse(BufferedImage image, String format) {
    	createResponseHeader("image/"+format);

    	try {
        	OutputStream body = mResponse.getOutputStream();
			ImageIO.write(image, format, body);
        	body.close();
            }
        catch (IOException e) {
            e.printStackTrace();
            }
        }

    /**
     * This method does the work of this task. If the server works asynchronously,
     * i.e. if it uses multiple threads for handling requests, then this method
     * must be thread-safe. For retrieving task parameters use getRequestText(key)
     * and getRequestObject(key). After performing the task, call one of the
     * create...Response(result) methods with a Serializable/String as result object or
     * with an error message.
     */
    public abstract void performTask();
	}
