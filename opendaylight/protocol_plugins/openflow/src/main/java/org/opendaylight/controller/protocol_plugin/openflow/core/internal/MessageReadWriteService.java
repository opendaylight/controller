
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.opendaylight.controller.protocol_plugin.openflow.core.IMessageReadWrite;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.factory.BasicFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements methods to read/write messages over an established
 * socket channel. The data exchange is in clear text format.
 */
public class MessageReadWriteService implements IMessageReadWrite {
    private static final Logger logger = LoggerFactory
            .getLogger(MessageReadWriteService.class);
    private static final int bufferSize = 1024 * 1024;

    private Selector selector;
    private SelectionKey clientSelectionKey;
    private SocketChannel socket;
    private ByteBuffer inBuffer;
    private ByteBuffer outBuffer;
    private BasicFactory factory;

    public MessageReadWriteService(SocketChannel socket, Selector selector) throws ClosedChannelException {
    	this.socket = socket;
    	this.selector = selector;
    	this.factory = new BasicFactory();
    	this.inBuffer = ByteBuffer.allocateDirect(bufferSize);
    	this.outBuffer = ByteBuffer.allocateDirect(bufferSize);
    	this.clientSelectionKey = this.socket.register(this.selector,
    			SelectionKey.OP_READ);
    }

	/**
	 * Sends the OF message out over the socket channel.
	 * 
	 * @param msg OF message to be sent
	 * @throws Exception
	 */
    @Override
    public void asyncSend(OFMessage msg) throws IOException {
    	synchronized (outBuffer) {
    		int msgLen = msg.getLengthU();
    		if (outBuffer.remaining() < msgLen) {
    			// increase the buffer size so that it can contain this message
    			ByteBuffer newBuffer = ByteBuffer.allocateDirect(outBuffer
    					.capacity()
    					+ msgLen);
    			outBuffer.flip();
    			newBuffer.put(outBuffer);
    			outBuffer = newBuffer;
    		}
    		msg.writeTo(outBuffer);

    		if (!socket.isOpen()) {
    			return;
    		}

    		outBuffer.flip();
    		socket.write(outBuffer);
    		outBuffer.compact();
    		if (outBuffer.position() > 0) {
    			this.clientSelectionKey = this.socket.register(
    					this.selector, SelectionKey.OP_WRITE, this);
    		}
    		logger.trace("Message sent: {}", msg.toString());
    	}
    }

	/**
	 * Resumes sending the remaining messages in the outgoing buffer
	 * @throws Exception
	 */
    @Override
    public void resumeSend() throws IOException {
		synchronized (outBuffer) {
			if (!socket.isOpen()) {
				return;
			}

        	outBuffer.flip();
        	socket.write(outBuffer);
        	outBuffer.compact();
        	if (outBuffer.position() > 0) {
        		this.clientSelectionKey = this.socket.register(
        				this.selector, SelectionKey.OP_WRITE, this);
        	} else {
        		this.clientSelectionKey = this.socket.register(
        				this.selector, SelectionKey.OP_READ, this);
        	}
        }
    }

	/**
	 * Reads the incoming network data from the socket and retrieves the OF
	 * messages.
	 * 
	 * @return list of OF messages
	 * @throws Exception
	 */
    @Override
    public List<OFMessage> readMessages() throws IOException {
		if (!socket.isOpen()) {
			return null;
		}

		List<OFMessage> msgs = null;
        int bytesRead = -1;        
        bytesRead = socket.read(inBuffer);
        if (bytesRead < 0) {
			throw new AsynchronousCloseException();
        }

        inBuffer.flip();
        msgs = factory.parseMessages(inBuffer);
        if (inBuffer.hasRemaining()) {
            inBuffer.compact();
        } else {
            inBuffer.clear();
        }
        return msgs;
    }

	@Override
	public void stop() {
		inBuffer = null;
		outBuffer = null;
	}
}
