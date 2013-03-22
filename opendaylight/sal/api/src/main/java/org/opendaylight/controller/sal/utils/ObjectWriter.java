
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write object to write to file stream
 *
 */
public class ObjectWriter {
    private static Logger logger = LoggerFactory.getLogger(ObjectWriter.class);
    private FileOutputStream fos;
    private ObjectOutputStream oos;

    public ObjectWriter() {
        fos = null;
        oos = null;
    }

    public Status write(Object obj, String file) {
        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(obj);
        } catch (FileNotFoundException fex) {
            logger.error("Cannot create {} for writing", file);
            return new Status(StatusCode.INTERNALERROR, "IO Error");
        } catch (IOException ioex) {
            logger.error("Failed to write to {}", file);
            return new Status(StatusCode.INTERNALERROR, "IO Error");
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException ioex) {
                    logger.error("Failed to close object output stream: {}",
                            file);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioex) {
                    logger
                            .error("Failed to close output file stream: {}",
                                    file);
                }
            }
        }
        return new Status(StatusCode.SUCCESS, null);
    }
}
