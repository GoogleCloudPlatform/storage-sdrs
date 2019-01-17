/*
 * Copyright 2018 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 * Any software provided by Google hereunder is distributed “AS IS”,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, and is not intended for production use.
 *
 */

package com.google.cloudy.retention.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.sql.SQLException;

import com.google.cloudy.retention.scheduler.JobScheduler;
import com.google.cloudy.retention.utils.StringUtils;


/**
 * Basic idea here: while (true) 
 * {
 * accept a connection
 * delegate/create a thread to deal with the client
 * end while only when shutting down the server 
 * otherwise, keep listening for incoming connections
 * }
 * 
 * Inspired from example tutorial @ https://www.javaworld.com/article/2853780/core-java/socket-programming-for-scalable-systems.html
 * @deprecated For POC purposes only 
 */
@Deprecated
public class AsyncSocketServer {
	
	private JobScheduler jobScheduler;
	private static String SHUTDOWN_COMMAND;
	
	public AsyncSocketServer( String bindAddr, int bindPort, String shutDownCommand, JobScheduler jobScheduler) throws IOException {
		this.jobScheduler = jobScheduler;
		InetSocketAddress sockAddr = new InetSocketAddress(bindAddr, bindPort);
		SHUTDOWN_COMMAND = shutDownCommand;
		//create channel and bind to addy
		AsynchronousServerSocketChannel serverSock =  AsynchronousServerSocketChannel.open().bind(sockAddr);
		System.out.println("Asysnc Socker Server operational and listening for incoming connections...");
			//start to accept the connection from client
			serverSock.accept(serverSock, new CompletionHandler<AsynchronousSocketChannel,AsynchronousServerSocketChannel >() {

				@Override
				public void completed(AsynchronousSocketChannel sockChannel, AsynchronousServerSocketChannel serverSock ) {
					//a connection is accepted, start to accept next connection
					serverSock.accept( serverSock, this );  // very important to call accept again
					//start to read message from the client(s)...we are a server, we so we just listen 
					doRead( sockChannel );
				}

				@Override
				public void failed(Throwable exc, AsynchronousServerSocketChannel serverSock) {
					System.out.println( "error, failed to accept a conn");
				}
			} );

	}

	protected void doRead( AsynchronousSocketChannel sockChannel ) {
		final ByteBuffer buf = ByteBuffer.allocate(2048);
		//read message from client
		sockChannel.read( buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel >() {

			/**
			 * some message is read from client, callback will be invoked
			 */
			@Override
			public void completed(Integer result, AsynchronousSocketChannel channel  ) {
				buf.flip(); // flip the buffer

				// send the message back to the client to prove that we got it
				String incoming =StringUtils.bb_to_str(buf, Charset.forName("UTF-8"));
				String reply=null;
				try {
					if(incoming.contains(SHUTDOWN_COMMAND)) {
						jobScheduler.handleRequest(incoming);
						shutDown();
					} else {
						jobScheduler.handleRequest(incoming);
						reply = "from the server with love "+incoming;
					}
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				doWrite( channel, ByteBuffer.wrap(reply.getBytes(Charset.forName("UTF-8")))) ;

				//keep lines open, start to read next message again
				doRead( channel );
			}

			@Override
			public void failed(Throwable exc, AsynchronousSocketChannel channel ) {
				System.out.println( "error, failed to read message from client");
			}
		});
	}

	protected void doWrite( AsynchronousSocketChannel sockChannel, final ByteBuffer buf) {
		sockChannel.write(buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel >() {

			@Override
			public void completed(Integer result, AsynchronousSocketChannel channel) {                 
				//finish to write message to client, nothing to do
			}

			@Override
			public void failed(Throwable exc, AsynchronousSocketChannel channel) {
				//fail to write message to client
				System.out.println( "Fail to write message to client");
			}

		});
	}

	private static void shutDown() {
		System.exit(0);
	}
}