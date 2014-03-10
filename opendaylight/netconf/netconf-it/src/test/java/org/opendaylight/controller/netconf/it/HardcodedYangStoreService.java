/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.it;

import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreException;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreService;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreServiceImpl;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreSnapshot;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class HardcodedYangStoreService implements YangStoreService {

    private final List<InputStream> byteArrayInputStreams;

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

        YangParserImpl yangParser = new YangParserImpl();
        final SchemaContext schemaContext = yangParser.resolveSchemaContext(new HashSet<>(yangParser.parseYangModelsFromStreamsMapped(byteArrayInputStreams).values()));
        YangStoreServiceImpl yangStoreService = new YangStoreServiceImpl(new SchemaContextProvider() {
            @Override
            public SchemaContext getSchemaContext() {
                return schemaContext ;
            }
        });
        return yangStoreService.getYangStoreSnapshot();
    }
}
