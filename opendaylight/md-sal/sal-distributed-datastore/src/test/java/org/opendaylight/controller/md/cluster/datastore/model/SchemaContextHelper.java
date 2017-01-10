/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.cluster.datastore.model;

import com.google.common.base.Throwables;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public class SchemaContextHelper {

    public static final String ODL_DATASTORE_TEST_YANG = "/odl-datastore-test.yang";
    public static final String PEOPLE_YANG = "/people.yang";
    public static final String CARS_YANG = "/cars.yang";

    public static InputStream getInputStream(final String yangFileName) {
        return SchemaContextHelper.class.getResourceAsStream(yangFileName);
    }

    public static SchemaContext full() {
        return select(ODL_DATASTORE_TEST_YANG, PEOPLE_YANG, CARS_YANG);
    }

    public static SchemaContext select(final String... schemaFiles) {
        List<InputStream> streams = new ArrayList<>(schemaFiles.length);

        for (String schemaFile : schemaFiles) {
            streams.add(getInputStream(schemaFile));
        }

        try {
            return YangParserTestUtils.parseYangStreams(streams);
        } catch (ReactorException e) {
            throw new RuntimeException("Unable to build schema context from " + streams, e);
        }
    }

    public static SchemaContext distributedShardedDOMDataTreeSchemaContext() {
        final List<InputStream> streams = new ArrayList<>();
        try {
            // we need prefix-shard-configuration and odl-datastore-test models
            // for DistributedShardedDOMDataTree tests
            streams.add(getInputStream(ODL_DATASTORE_TEST_YANG));
            streams.add(new FileInputStream("src/main/yang/prefix-shard-configuration.yang"));
            return YangParserTestUtils.parseYangStreams(streams);
        } catch (FileNotFoundException | ReactorException e) {
            throw new RuntimeException(e);
        }
    }

    public static SchemaContext entityOwners() {
        try {
            return YangParserTestUtils.parseYangSources(new File("src/main/yang/entity-owners.yang"));
        } catch (IOException | ReactorException e) {
            throw Throwables.propagate(e);
        }
    }
}
