/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.threads;

import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.ssh2.ServerConnection;
import ch.ethz.ssh2.ServerSession;

@ThreadSafe
public class IOThread extends Thread {

    private static final Logger logger =  LoggerFactory.getLogger(IOThread.class);

    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final ServerSession servSession;
    private final ServerConnection servconnection;
    private String customHeader;


    public IOThread (InputStream is, OutputStream os, String id,ServerSession ss, ServerConnection conn){
        this.inputStream = is;
        this.outputStream = os;
        this.servSession = ss;
        this.servconnection = conn;
        super.setName(id);
        logger.trace("IOThread {} created", super.getName());
    }

    public IOThread (InputStream is, OutputStream os, String id,ServerSession ss, ServerConnection conn,String header){
        this.inputStream = is;
        this.outputStream = os;
        this.servSession = ss;
        this.servconnection = conn;
        this.customHeader = header;
        super.setName(id);
        logger.trace("IOThread {} created", super.getName());
    }

    @Override
    public void run() {
        logger.trace("thread {} started", super.getName());
        try {
            if (this.customHeader!=null && !this.customHeader.equals("")){
                this.outputStream.write(this.customHeader.getBytes());
                logger.trace("adding {} header", this.customHeader);
            }
            IOUtils.copy(this.inputStream, this.outputStream);
        } catch (Exception e) {
            logger.error("inputstream -> outputstream copy error ",e);
        }
        logger.trace("closing server session");
        servSession.close();
        servconnection.close();
        logger.trace("thread {} is closing",super.getName());
    }
}
