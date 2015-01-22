
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience object to write a file
 *
 *
 *
 */
@Deprecated
public class WriteToFile {
    protected static final Logger logger = LoggerFactory
    .getLogger(WriteToFile.class);
    private FileWriter fstream;
    private BufferedWriter bufferOut;

    public WriteToFile(String fileName) throws IOException {
        fstream = new FileWriter(fileName);
        bufferOut = new BufferedWriter(fstream);
    }

    public void save(ArrayList<String> entryList) throws IOException {
        for (String entry : entryList) {
            bufferOut.write(entry);
            bufferOut.append('\n');
        }
        try {
            this.bufferOut.flush();
        } catch (IOException e) {
            logger.error("",e);
        }
    }

    public boolean close() {
        try {
            bufferOut.close();
            fstream.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
