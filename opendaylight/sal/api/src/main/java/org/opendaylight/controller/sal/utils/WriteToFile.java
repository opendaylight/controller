
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.io.*;
import java.util.ArrayList;

/**
 * Convenience object to write a file
 *
 *
 *
 */
public class WriteToFile {
    private FileWriter fstream;
    private BufferedWriter bufferOut;
    private String fileName;

    public WriteToFile(String name) throws IOException {
        fileName = name;
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
            e.printStackTrace();
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
