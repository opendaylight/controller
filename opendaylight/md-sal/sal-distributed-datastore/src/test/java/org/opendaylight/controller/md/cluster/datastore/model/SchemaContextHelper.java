/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.cluster.datastore.model;

import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SchemaContextHelper {

    public static InputStream getInputStream(final String yangFileName) {
        return TestModel.class.getResourceAsStream(yangFileName);
    }

    public static SchemaContext full(){
        YangParserImpl parser = new YangParserImpl();
        List<InputStream> streams = new ArrayList<>();
        streams.add(getInputStream("/odl-datastore-test.yang"));
        streams.add(getInputStream("/people.yang"));
        streams.add(getInputStream("/cars.yang"));

        Set<Module> modules = parser.parseYangModelsFromStreams(streams);
        return parser.resolveSchemaContext(modules);

    }
}
