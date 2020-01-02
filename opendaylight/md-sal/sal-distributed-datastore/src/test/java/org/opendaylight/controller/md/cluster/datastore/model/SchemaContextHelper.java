/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.cluster.datastore.model;

import java.io.InputStream;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public final class SchemaContextHelper {

    public static final String ODL_DATASTORE_TEST_YANG = "/odl-datastore-test.yang";
    public static final String PEOPLE_YANG = "/people.yang";
    public static final String CARS_YANG = "/cars.yang";

    private SchemaContextHelper() {

    }

    public static InputStream getInputStream(final String yangFileName) {
        return SchemaContextHelper.class.getResourceAsStream(yangFileName);
    }

    public static SchemaContext full() {
        return select(ODL_DATASTORE_TEST_YANG, PEOPLE_YANG, CARS_YANG);
    }

    public static SchemaContext select(final String... schemaFiles) {
        return YangParserTestUtils.parseYangResources(SchemaContextHelper.class, schemaFiles);
    }

    public static SchemaContext distributedShardedDOMDataTreeSchemaContext() {
        // we need prefix-shard-configuration and odl-datastore-test models
        // for DistributedShardedDOMDataTree tests
        return YangParserTestUtils.parseYangResources(SchemaContextHelper.class, ODL_DATASTORE_TEST_YANG,
            "/META-INF/yang/prefix-shard-configuration@2017-01-10.yang");
    }
}
