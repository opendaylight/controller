/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.impl;

import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yang.store.api.YangStoreService;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertNotNull;

public class HardcodedYangStoreService implements YangStoreService {

    private final Collection<ByteArrayInputStream> byteArrayInputStreams;

    public HardcodedYangStoreService(
            Collection<? extends InputStream> inputStreams)
            throws YangStoreException, IOException {
        byteArrayInputStreams = new ArrayList<>();
        for (InputStream inputStream : inputStreams) {
            assertNotNull(inputStream);
            byte[] content = IOUtils.toByteArray(inputStream);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                    content);
            byteArrayInputStreams.add(byteArrayInputStream);
        }
    }

    @Override
    public YangStoreSnapshot getYangStoreSnapshot() throws YangStoreException {
        for (InputStream inputStream : byteArrayInputStreams) {
            try {
                inputStream.reset();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new MbeParser().parseYangFiles(byteArrayInputStreams);
    }
}
