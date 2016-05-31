/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.cluster.datastore.model;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;

public class SchemaContextHelper {

    public static final String ODL_DATASTORE_TEST_YANG = "/odl-datastore-test.yang";
    public static final String PEOPLE_YANG = "/people.yang";
    public static final String CARS_YANG = "/cars.yang";

    public static InputStream getInputStream(final String yangFileName) {
        return SchemaContextHelper.class.getResourceAsStream(yangFileName);
    }

    public static SchemaContext full(){
        return select(ODL_DATASTORE_TEST_YANG, PEOPLE_YANG, CARS_YANG);
    }

    public static SchemaContext select(String... schemaFiles){
        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        final SchemaContext schemaContext;
        List<InputStream> streams = new ArrayList<>();

        for(String schemaFile : schemaFiles){
            streams.add(getInputStream(schemaFile));
        }

        try {
            schemaContext = reactor.buildEffective(streams);
        } catch (ReactorException e) {
            throw new RuntimeException("Unable to build schema context from " + streams, e);
        }

        return schemaContext;
    }

    public static SchemaContext entityOwners() {
        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        final SchemaContext schemaContext;
        File file = null;

        try {
            file = new File("src/main/yang/entity-owners.yang");
            final List<ByteSource> sources = Arrays.asList(Resources.asByteSource(file.toURI().toURL()));
            try {
                schemaContext = reactor.buildEffective(sources);
            } catch (IOException e1) {
                throw new ExceptionInInitializerError(e1);
            } catch (ReactorException e2) {
                throw new RuntimeException("Unable to build schema context from " + sources, e2);
            }
            return schemaContext;
        } catch (MalformedURLException e3) {
            throw new RuntimeException("Malformed URL detected in " + file, e3);
        }
    }
}
