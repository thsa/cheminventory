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

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerSocketProcessor;
import org.simpleframework.transport.SocketProcessor;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ServerCommunicator implements Container {
	private long DEFAULT_MISUSE_DELAY = 10000;
	private int DEFAULT_MISUSE_REQUESTS = 10;

    private Executor mExecutor;
    private ServerTaskFactory mTaskFactory;
    private ArrayDeque<ClientRequest> mClientDeque;
    private long mMisuseDelay;
    private int mMisuseRequests;

    private ServerCommunicator(Executor executor, ServerTaskFactory taskFactory) {
    	mExecutor = executor;
    	mTaskFactory = taskFactory;
    	mClientDeque = new ArrayDeque<>();
	    mMisuseDelay = DEFAULT_MISUSE_DELAY;
	    mMisuseRequests = DEFAULT_MISUSE_REQUESTS;
    	}

	@Override
	public void handle(Request request, Response response) {
		ServerTask task = mTaskFactory.createServerTask();
		task.setRequest(request);
		task.setResponse(response);
		if (isAllowed(task))
			mExecutor.execute(task);
		else
			task.createErrorResponse("Service misuse!");
		}

	/**
	 * Initialized and starts listening on the given port.
	 * @param taskFactory the factory being asked for a task whenever a client request comes in
	 * @param threadCount number of threads in thread pool; if threadCount==1 then then tasks are served synchronously
	 * @param port the port, this server is listening on
	 * @throws Exception
	 */
    public static void initialize(ServerTaskFactory taskFactory, int threadCount, int port) throws Exception {
     	Executor executor = (threadCount==1) ? Executors.newSingleThreadExecutor() : Executors.newFixedThreadPool(threadCount);

    	Container container = new ServerCommunicator(executor, taskFactory);
		SocketProcessor server = new ContainerSocketProcessor(container);
    	Connection connection = new SocketConnection(server);
    	SocketAddress address = new InetSocketAddress(port);
    	connection.connect(address);
    	}

	/**
	 * Defines the maximum number of request that  are allowed from the same IP-address within a given time.
	 * @param delay delay in millis
	 * @param requests 0 -> no limit
	 */
	public void setMisuseLimit(long delay, int requests) {
    	mMisuseDelay = delay;
    	mMisuseRequests = requests;
	    }

	private boolean isAllowed(ServerTask task) {
		if (mMisuseRequests == 0)
			return true;

    	String ip = task.getClientIP();
    	long timeLimit = System.currentTimeMillis() - mMisuseDelay;
    	int requests = 0;
		Iterator<ClientRequest> iterator = mClientDeque.iterator();
		while (iterator.hasNext()) {
			ClientRequest cr = iterator.next();
			if (cr.time < timeLimit)
				iterator.remove();
			else if (cr.ip.equals(ip)) {
				if (++requests == mMisuseRequests)
					return false;
				}
			}

		mClientDeque.add(new ClientRequest(ip));
		return true;
		}
	}

class ClientRequest {
	public String ip;
	public long time;

	public ClientRequest(String ip) {
		this.ip = ip;
		this.time = System.currentTimeMillis();
		}
	}