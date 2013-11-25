/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.threads;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOThread extends Thread {

    private static final Logger logger =  LoggerFactory.getLogger(IOThread.class);

    private InputStream inputStream;
    private OutputStream outputStream;
    private String id;


    public IOThread (InputStream is, OutputStream os, String id){
        inputStream = is;
        outputStream = os;
        logger.trace("IOThread created");
        id = id;
    }

    @Override
    public void run() {
        try {
            int c = 0;
            while (c >= 0){
                c = IOUtils.copy(inputStream, outputStream);
            }
        } catch (IOException e) {
            logger.error("input -> output copy error in thread id {}",id,e);
        }
    }
}
