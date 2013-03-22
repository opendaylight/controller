
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read object to read from file stream
 *
 *
 *
 */
public class ObjectReader {
    private static Logger logger = LoggerFactory.getLogger(ObjectReader.class);
    private FileInputStream fis;
    public ObjectInputStream ois;

    public ObjectReader() {
        fis = null;
        ois = null;
    }

    public Object read(IObjectReader reader, String file) {
        Object obj = null;
        try {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            obj = reader.readObject(ois);
        } catch (FileNotFoundException fnfex) {
            //logger.info("Cannot find {} for reading", file);
        } catch (IOException ioex) {
            logger.error("Failed to read from {}", file);
        } catch (ClassNotFoundException cnfex) {
            logger.error("Failed to interpret content of {}", file);
        } catch (Exception e) {

        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException ioex) {
                    logger.error("Failed to close object input stream: {}",
                            file);
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ioex) {
                    logger.error("Failed to close input file stream: {}", file);
                }
            }
        }
        return obj;
    }
}
