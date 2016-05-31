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
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;

public class TestModel {

    public static final QName TEST_QNAME =
            QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test", "2014-03-13", "test");

    public static final QName TEST2_QNAME =
            QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test", "2014-03-13", "test2");

    public static final QName JUNK_QNAME =
            QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:junk", "2014-03-13", "junk");


    public static final QName OUTER_LIST_QNAME = QName.create(TEST_QNAME, "outer-list");
    public static final QName OUTER_CONTAINER_QNAME = QName.create(TEST_QNAME, "outer-container");
    public static final QName INNER_LIST_QNAME = QName.create(TEST_QNAME, "inner-list");
    public static final QName OUTER_CHOICE_QNAME = QName.create(TEST_QNAME, "outer-choice");
    public static final QName ID_QNAME = QName.create(TEST_QNAME, "id");
    public static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
    public static final QName DESC_QNAME = QName.create(TEST_QNAME, "desc");
    private static final String DATASTORE_TEST_YANG = "/odl-datastore-test.yang";

    public static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier.of(TEST_QNAME);
    public static final YangInstanceIdentifier TEST2_PATH = YangInstanceIdentifier.of(TEST2_QNAME);
    public static final YangInstanceIdentifier JUNK_PATH = YangInstanceIdentifier.of(JUNK_QNAME);
    public static final YangInstanceIdentifier OUTER_LIST_PATH = YangInstanceIdentifier.builder(TEST_PATH).
            node(OUTER_LIST_QNAME).build();
    public static final YangInstanceIdentifier INNER_LIST_PATH = YangInstanceIdentifier.builder(TEST_PATH).
            node(OUTER_LIST_QNAME).node(INNER_LIST_QNAME).build();
    public static final YangInstanceIdentifier OUTER_CONTAINER_PATH = TEST_PATH.node(OUTER_CONTAINER_QNAME);
    public static final QName TWO_QNAME = QName.create(TEST_QNAME,"two");
    public static final QName THREE_QNAME = QName.create(TEST_QNAME,"three");

    public static SchemaContext createTestContext() {
        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        final SchemaContext schemaContext;
        final Set<ByteSource> sources = Collections.singleton(Resources.asByteSource(TestModel.class.getResource
                (DATASTORE_TEST_YANG)));

        try {
            schemaContext = reactor.buildEffective(sources);
        } catch (IOException e1) {
            throw new ExceptionInInitializerError(e1);
        }  catch (ReactorException e2) {
            throw new RuntimeException("Unable to build schema context from " + sources, e2);
        }
        return schemaContext;
    }

    public static DataContainerChild<?, ?> outerNode(int... ids) {
        CollectionNodeBuilder<MapEntryNode, MapNode> outer = ImmutableNodes.mapNodeBuilder(OUTER_LIST_QNAME);
        for(int id: ids) {
            outer.addChild(ImmutableNodes.mapEntry(OUTER_LIST_QNAME, ID_QNAME, id));
        }

        return outer.build();
    }

    public static DataContainerChild<?, ?> outerNode(MapEntryNode... entries) {
        CollectionNodeBuilder<MapEntryNode, MapNode> outer = ImmutableNodes.mapNodeBuilder(OUTER_LIST_QNAME);
        for(MapEntryNode e: entries) {
            outer.addChild(e);
        }

        return outer.build();
    }

    public static DataContainerChild<?, ?> innerNode(String... names) {
        CollectionNodeBuilder<MapEntryNode, MapNode> outer = ImmutableNodes.mapNodeBuilder(INNER_LIST_QNAME);
        for(String name: names) {
            outer.addChild(ImmutableNodes.mapEntry(INNER_LIST_QNAME, NAME_QNAME, name));
        }

        return outer.build();
    }

    public static MapEntryNode outerNodeEntry(int id, DataContainerChild<?, ?> inner) {
        return ImmutableNodes.mapEntryBuilder(OUTER_LIST_QNAME, ID_QNAME, id).addChild(inner).build();
    }

    public static NormalizedNode<?, ?> testNodeWithOuter(int... ids) {
        return testNodeWithOuter(outerNode(ids));
    }

    public static NormalizedNode<?, ?> testNodeWithOuter(DataContainerChild<?, ?> outer) {
        return ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME)).withChild(outer).build();
    }

    public static NodeIdentifierWithPredicates outerEntryKey(int id) {
        return new NodeIdentifierWithPredicates(OUTER_LIST_QNAME, ID_QNAME, id);
    }

    public static YangInstanceIdentifier outerEntryPath(int id) {
        return OUTER_LIST_PATH.node(outerEntryKey(id));
    }

    public static NodeIdentifierWithPredicates innerEntryKey(String name) {
        return new NodeIdentifierWithPredicates(INNER_LIST_QNAME, NAME_QNAME, name);
    }

    public static YangInstanceIdentifier innerEntryPath(int id, String name) {
        return OUTER_LIST_PATH.node(outerEntryKey(id)).node(INNER_LIST_QNAME).node(innerEntryKey(name));
    }
}
