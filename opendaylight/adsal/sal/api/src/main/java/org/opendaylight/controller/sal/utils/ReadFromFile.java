
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Convenience object for reading from file
 *
 *
 *
 */
@Deprecated
public class ReadFromFile {
    private FileInputStream fileStream;
    private File filePointer;

    public ReadFromFile(String fileName) throws FileNotFoundException {
        fileStream = new FileInputStream(fileName);
        filePointer = new File(fileName); //needed to allow file deletion
    }

    public ArrayList<String> readFile() throws IOException {
        DataInputStream dataInput = new DataInputStream(this.fileStream);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(dataInput));

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