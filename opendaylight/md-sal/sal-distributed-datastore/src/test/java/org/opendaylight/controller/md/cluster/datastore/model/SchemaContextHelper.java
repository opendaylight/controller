/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.cluster.datastore.model;

import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class SchemaContextHelper {

    public static final String ODL_DATASTORE_TEST_YANG = "/odl-datastore-test.yang";
    public static final String PEOPLE_YANG = "/people.yang";
    public static final String CARS_YANG = "/cars.yang";

    public static InputStream getInputStream(final String yangFileName) {
        return TestModel.class.getResourceAsStream(yangFileName);
    }

    public static SchemaContext full(){
        return select(ODL_DATASTORE_TEST_YANG, PEOPLE_YANG, CARS_YANG);
    }

    public static SchemaContext select(String... schemaFiles){
        YangParserImpl parser = new YangParserImpl();
        List<InputStream> streams = new ArrayList<>();

        for(String schemaFile : schemaFiles){
            streams.add(getInputStream(schemaFile));
        }

        Set<Module> modules = parser.parseYangModelsFromStreams(streams);
        return parser.resolveSchemaContext(modules);
    }

    public static SchemaContext entityOwners() {
        YangParserImpl parser = new YangParserImpl();
        try {
            File file = new File("src/main/yang/entity-owners.yang");
            return parser.parseSources(Arrays.asList(Resources.asByteSource(file.toURI().toURL())));
        } catch (IOException | YangSyntaxErrorException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
