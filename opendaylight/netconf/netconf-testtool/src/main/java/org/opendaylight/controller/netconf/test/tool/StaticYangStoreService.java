/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.test.tool;

import java.io.File;
import java.util.Collection;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreException;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreService;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreServiceImpl;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreSnapshot;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreSnapshotImpl;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class StaticYangStoreService implements YangStoreService {

    private final Collection<File> inputStreams;
    private YangStoreSnapshotImpl snapshot;

    public StaticYangStoreService(
            final Collection<File> inputStreams) {
        this.inputStreams = inputStreams;
    }

    @Override
    public synchronized YangStoreSnapshot getYangStoreSnapshot() throws YangStoreException {
        if(snapshot != null) {
            return snapshot;
        }

        final YangParserImpl yangParser = new YangParserImpl();
        final SchemaContext schemaContext = yangParser.parseFiles(inputStreams);

        final YangStoreServiceImpl yangStoreService = new YangStoreServiceImpl(new SchemaContextProvider() {
            @Override
            public SchemaContext getSchemaContext() {
                return schemaContext ;
            }
        });

        this.snapshot = yangStoreService.getYangStoreSnapshot();
        return this.snapshot;
    }
}
