
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.util.ArrayList;
import java.io.*;

/**
 * Convenience object for reading from file
 *
 *
 *
 */
public class ReadFromFile {
    private FileInputStream fileStream;
    private DataInputStream dataInput;
    private BufferedReader bufferedReader;
    private String fileName;
    private File filePointer;

    public ReadFromFile(String name) throws FileNotFoundException {
        fileName = name;
        fileStream = new FileInputStream(this.fileName);
        filePointer = new File(fileName); //needed to allow file deletion
    }

    public ArrayList<String> readFile() throws IOException {
        dataInput = new DataInputStream(this.fileStream);
        bufferedReader = new BufferedReader(new InputStreamReader(dataInput));

        ArrayList<String> lineList = new ArrayList<String>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            lineList.add(line);
        }
        bufferedReader.close();
        dataInput.close();
        fileStream.close();
        return lineList;
    }

    public boolean delete() {
        if (filePointer == null) {
            return true;
        }
        return filePointer.delete();
    }
}