/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.persistence.support;

import com.typesafe.config.Config;
import net.jpountz.lz4.LZ4FrameOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputOutputStreamFactory {
    private static final Logger LOG = LoggerFactory.getLogger(InputOutputStreamFactory.class);

    private InputOutputStreamFactory() {}

    public static InputOutputStreamSupport newInstance(Config config) {
        if (config.getBoolean("use-lz4-compression")) {
            String size = config.getString("lz4-blocksize");
            LZ4FrameOutputStream.BLOCKSIZE blocksize = LZ4FrameOutputStream.BLOCKSIZE.valueOf("SIZE_" + size);
            LOG.debug("Using LZ4 Input/Output Stream, blocksize: {}", blocksize);
            return new LZ4InputOutputStreamSupport(blocksize);
        } else {
            LOG.debug("Using plain Input/Output Stream");
            return new PlainInputOutputStreamSupport();
        }
    }
}
