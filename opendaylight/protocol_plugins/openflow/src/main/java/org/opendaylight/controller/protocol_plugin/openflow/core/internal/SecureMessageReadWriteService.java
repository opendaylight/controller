
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import org.opendaylight.controller.protocol_plugin.openflow.core.IMessageReadWrite;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.factory.BasicFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements methods to read/write messages over an established
 * socket channel. The data exchange is encrypted/decrypted by SSLEngine.
 */
public class SecureMessageReadWriteService implements IMessageReadWrite {
    private static final Logger logger = LoggerFactory
            .getLogger(SecureMessageReadWriteService.class);

    private Selector selector;
    private SelectionKey clientSelectionKey;
    private SocketChannel socket;
    private BasicFactory factory;

    private SSLEngine sslEngine;
	private SSLEngineResult sslEngineResult;   	// results from sslEngine last operation
    private ByteBuffer myAppData;				// clear text message to be sent
    private ByteBuffer myNetData;   			// encrypted message to be sent
    private ByteBuffer peerAppData;				// clear text message received from the switch
    private ByteBuffer peerNetData; 			// encrypted message from the switch
    private FileInputStream kfd = null, tfd = null;

    public SecureMessageReadWriteService(SocketChannel socket, Selector selector) throws Exception {
    	this.socket = socket;
    	this.selector = selector;
    	this.factory = new BasicFactory();

    	try {
    		createSecureChannel(socket);
    		createBuffers(sslEngine);
    	} catch (Exception e) {
    		stop();
    		throw e;
    	}
    }

	/**
	 * Bring up secure channel using SSL Engine
	 * 
	 * @param socket TCP socket channel
	 * @throws Exception
	 */
    private void createSecureChannel(SocketChannel socket) throws Exception {
     	String keyStoreFile = System.getProperty("controllerKeyStore").trim();
    	String keyStorePassword = System.getProperty("controllerKeyStorePassword").trim();
     	String trustStoreFile = System.getProperty("controllerTrustStore").trim();
    	String trustStorePassword = System.getProperty("controllerTrustStorePassword").trim();

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        kfd = new FileInputStream(keyStoreFile);
        tfd = new FileInputStream(trustStoreFile);
        ks.load(kfd, keyStorePassword.toCharArray());
        ts.load(tfd, trustStorePassword.toCharArray());
        kmf.init(ks, keyStorePassword.toCharArray());
        tmf.init(ts);

        SecureRandom random = new SecureRandom();
        random.nextInt();

    	SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), random);
    	sslEngine = sslContext.createSSLEngine();
    	sslEngine.setUseClientMode(false);
    	sslEngine.setNeedClientAuth(true);
    	
    	// Do initial handshake
    	doHandshake(socket, sslEngine);
    	
        this.clientSelectionKey = this.socket.register(this.selector,
                SelectionKey.OP_READ);
    }

	/**
	 * Sends the OF message out over the socket channel. The message is
	 * encrypted by SSL Engine.
	 * 
	 * @param msg OF message to be sent
	 * @throws Exception
	 */
    @Override
    public void asyncSend(OFMessage msg) throws Exception {
    	synchronized (myAppData) {
    		int msgLen = msg.getLengthU();
    		if (myAppData.remaining() < msgLen) {
    			// increase the buffer size so that it can contain this message
    			ByteBuffer newBuffer = ByteBuffer.allocateDirect(myAppData
    					.capacity()
    					+ msgLen);
    			myAppData.flip();
    			newBuffer.put(myAppData);
    			myAppData = newBuffer;
    		}
    		msg.writeTo(myAppData);
    		myAppData.flip();
    		sslEngineResult = sslEngine.wrap(myAppData, myNetData);
    		logger.trace("asyncSend sslEngine wrap: {}", sslEngineResult);
    		runDelegatedTasks(sslEngineResult, sslEngine);

    		if (!socket.isOpen()) {
    			return;
    		}

    		myNetData.flip();
    		socket.write(myNetData);
    		if (myNetData.hasRemaining()) {
    			myNetData.compact();
    		} else {
    			myNetData.clear();
    		}

    		if (myAppData.hasRemaining()) {
    			myAppData.compact();
    			this.clientSelectionKey = this.socket.register(
    					this.selector, SelectionKey.OP_WRITE, this);
    		} else {
    			myAppData.clear();
    			this.clientSelectionKey = this.socket.register(
    					this.selector, SelectionKey.OP_READ, this);
    		}

    		logger.trace("Message sent: {}", msg.toString());
    	}
    }

	/**
	 * Resumes sending the remaining messages in the outgoing buffer
	 * @throws Exception
	 */
    @Override
    public void resumeSend() throws Exception {
		synchronized (myAppData) {
			myAppData.flip();
			sslEngineResult = sslEngine.wrap(myAppData, myNetData);
			logger.trace("resumeSend sslEngine wrap: {}", sslEngineResult);
			runDelegatedTasks(sslEngineResult, sslEngine);

			if (!socket.isOpen()) {
				return;
			}

			myNetData.flip();
			socket.write(myNetData);
			if (myNetData.hasRemaining()) {
				myNetData.compact();
			} else {
				myNetData.clear();
			}

			if (myAppData.hasRemaining()) {
				myAppData.compact();
				this.clientSelectionKey = this.socket.register(this.selector,
						SelectionKey.OP_WRITE, this);
			} else {
				myAppData.clear();
				this.clientSelectionKey = this.socket.register(this.selector,
						SelectionKey.OP_READ, this);
			}
		}
    }

	/**
	 * Reads the incoming network data from the socket, decryptes them and then
	 * retrieves the OF messages.
	 * 
	 * @return list of OF messages
	 * @throws Exception
	 */
    @Override
    public List<OFMessage> readMessages() throws Exception {
		if (!socket.isOpen()) {
			return null;
		}

		List<OFMessage> msgs = null;
        int bytesRead = -1;
    	int countDown = 50;        	

    	bytesRead = socket.read(peerNetData);
    	if (bytesRead < 0) {
    		logger.debug("Message read operation failed");
			throw new AsynchronousCloseException();
    	}

    	do {        		
    		peerNetData.flip();
    		sslEngineResult = sslEngine.unwrap(peerNetData, peerAppData);
    		if (peerNetData.hasRemaining()) {
    			peerNetData.compact();
    		} else {
    			peerNetData.clear();
    		}
    		logger.trace("sslEngine unwrap result: {}", sslEngineResult);
    		runDelegatedTasks(sslEngineResult, sslEngine);
    	} while ((sslEngineResult.getStatus() == SSLEngineResult.Status.OK) &&
    			  peerNetData.hasRemaining() && (--countDown > 0));
    	
    	if (countDown == 0) {
    		logger.trace("countDown reaches 0. peerNetData pos {} lim {}", peerNetData.position(), peerNetData.limit());
    	}

    	peerAppData.flip();
    	msgs = factory.parseMessages(peerAppData);
    	if (peerAppData.hasRemaining()) {
    		peerAppData.compact();
    	} else {
    		peerAppData.clear();
    	}

    	this.clientSelectionKey = this.socket.register(
    			this.selector, SelectionKey.OP_READ, this);
        
        return msgs;
    }

    /**
     *  If the result indicates that we have outstanding tasks to do,
     *  go ahead and run them in this thread.
     */
    private void runDelegatedTasks(SSLEngineResult result,
    		SSLEngine engine) throws Exception {

    	if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
    		Runnable runnable;
    		while ((runnable = engine.getDelegatedTask()) != null) {
    			logger.debug("\trunning delegated task...");
    			runnable.run();
    		}
    		HandshakeStatus hsStatus = engine.getHandshakeStatus();
    		if (hsStatus == HandshakeStatus.NEED_TASK) {
    			throw new Exception(
    					"handshake shouldn't need additional tasks");
    		}
    		logger.debug("\tnew HandshakeStatus: {}", hsStatus);
    	}
    }

    private void doHandshake(SocketChannel socket, SSLEngine engine) throws Exception {
    	SSLSession session = engine.getSession();
    	ByteBuffer myAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
    	ByteBuffer peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
    	ByteBuffer myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    	ByteBuffer peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());

    	// Begin handshake
    	engine.beginHandshake();
    	SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();

    	// Process handshaking message
    	while (hs != SSLEngineResult.HandshakeStatus.FINISHED &&
    		   hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
    		switch (hs) {
    		case NEED_UNWRAP:
    			// Receive handshaking data from peer
    			if (socket.read(peerNetData) < 0) {
    				throw new AsynchronousCloseException();
    			}

    			// Process incoming handshaking data
    			peerNetData.flip();
    			SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
    			peerNetData.compact();
    			hs = res.getHandshakeStatus();

    			// Check status
    			switch (res.getStatus()) {
    			case OK :
    				// Handle OK status
    				break;
    			}
    			break;

    		case NEED_WRAP :
    			// Empty the local network packet buffer.
    			myNetData.clear();

    			// Generate handshaking data
    			res = engine.wrap(myAppData, myNetData);
    			hs = res.getHandshakeStatus();

    			// Check status
    			switch (res.getStatus()) {
    			case OK :
    				myNetData.flip();

    				// Send the handshaking data to peer
    				while (myNetData.hasRemaining()) {
    					if (socket.write(myNetData) < 0) {
    	    				throw new AsynchronousCloseException();
    					}
    				}
    				break;
    			}
    			break;

    		case NEED_TASK :
    			// Handle blocking tasks
        		Runnable runnable;
        		while ((runnable = engine.getDelegatedTask()) != null) {
        			logger.debug("\trunning delegated task...");
        			runnable.run();
        		}
        		hs = engine.getHandshakeStatus();
        		if (hs == HandshakeStatus.NEED_TASK) {
        			throw new Exception(
        					"handshake shouldn't need additional tasks");
        		}
        		logger.debug("\tnew HandshakeStatus: {}", hs);
    			break;
    		}
    	}
    }
    
    private void createBuffers(SSLEngine engine) {
    	SSLSession session = engine.getSession();
    	this.myAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
    	this.peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
    	this.myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    	this.peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    }

	@Override
	public void stop() throws IOException {
		this.sslEngine = null;
		this.sslEngineResult = null;
		this.myAppData = null;
		this.myNetData = null;
		this.peerAppData = null;
		this.peerNetData = null;
	    
	    if (this.kfd != null) {
	    	this.kfd.close();
	    	this.kfd = null;
		}
		if (this.tfd != null) {
			this.tfd.close();
			this.tfd = null;
		}
	}
}
