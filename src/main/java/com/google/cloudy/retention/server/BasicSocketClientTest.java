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
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import com.google.cloudy.retention.utils.StringUtils;


/**
 * 
 *  @deprecated For POC purposes only 
 */
@Deprecated
public class BasicSocketClientTest {
	
	
	  public static void main( String[] args ) throws ConfigurationException {
		  System.out.println("Running client test...");
		  Configurations configs = new Configurations();
		HierarchicalConfiguration config = configs.xml("default-applicationConfig.xml");
	    	try {
	    		for( int i = 0; i < 1; i++ ) {
	    			new BasicSocketClientTest(config.getString("serverConfig.address"), 
	    					config.getInt("serverConfig.port"), new String("sts") );//fire up X clients with unique messages
	    		}
	    		TimeUnit.MINUTES.sleep(config.getInt("serverConfig.sleepMins"));//automatically shut down in 1 hour for POC purposes

	    	} catch (Exception ex) {
	    		Logger.getLogger(BasicSocketClientTest.class.getName()).log(Level.SEVERE, null, ex);
	    	}
	    }
    
    public BasicSocketClientTest( String host, int port, final String message) throws IOException {
        //create a socket channel
        AsynchronousSocketChannel sockChannel = AsynchronousSocketChannel.open();
        
        //try to connect to the server side
        sockChannel.connect( new InetSocketAddress(host, port), sockChannel, new CompletionHandler<Void, AsynchronousSocketChannel >() {
            @Override
            public void completed(Void result, AsynchronousSocketChannel channel ) {
                //start to read message
                doRead( channel);
                
                //write a message to server side
                doWrite( channel, message);
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                System.out.println( "fail to connect to server");
            }
            
        });
    }
    
   
   
    protected void doRead( final AsynchronousSocketChannel sockChannel) {
        final ByteBuffer buf = ByteBuffer.allocate(2048);
        
        sockChannel.read( buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>(){

            @Override
            public void completed(Integer result, AsynchronousSocketChannel channel) {   
                System.out.println( "Read message coming from server:" + StringUtils.bb_to_str(buf,Charset.forName("UTF-8") ) );
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                System.out.println( "fail to read message from server");
            }
            
        });
        
    }
    
    protected void doWrite( final AsynchronousSocketChannel sockChannel, final String message ) {
    	ByteBuffer buf = ByteBuffer.allocate(2048);
    	//String messageToSend ="from the client with love "+message;
    	System.out.println("We are sending to the server "+ message);
    	buf.put(message.getBytes());
    	buf.flip();

    	sockChannel.write(buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel >() {
    		@Override
    		public void completed(Integer result, AsynchronousSocketChannel channel ) {
    			//after message written, we have no work to do, we're done
    		}

    		@Override
    		public void failed(Throwable exc, AsynchronousSocketChannel channel) {
    			System.out.println( "Fail to write the message to server");
    		}
    	});
    }
      
}