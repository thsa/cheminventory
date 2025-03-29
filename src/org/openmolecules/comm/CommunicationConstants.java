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

public interface CommunicationConstants {
    String ERROR_INVALID_SESSION = "Invalid session";
    String ERROR_INVALID_TOKEN = "Invalid token";

    String BODY_MESSAGE = "Message";
    String BODY_OBJECT = "Object";
    String BODY_ERROR = "Error";
    String BODY_ERROR_INVALID_SESSION = BODY_ERROR + ":" + ERROR_INVALID_SESSION;
    String BODY_IMAGE_PNG = "PNG";

    String KEY_SESSION_ID = "sessionID";
    String KEY_REQUEST = "what";
    String KEY_QUERY = "query";
    String KEY_APP_NAME = "appname";
    String KEY_USER = "user";
    String KEY_PASSWORD = "password";
    String KEY_ERROR_200 = "error200";  // if true, then the server uses HTTP code 200 for error messages

    String REQUEST_NEW_SESSION = "new";
    String REQUEST_END_SESSION = "end";
    String REQUEST_GET_STATUS = "status";
    String REQUEST_RUN_QUERY = "query";
    String REQUEST_LOGIN = "login";
    String REQUEST_LOGOUT = "logout";
	}
