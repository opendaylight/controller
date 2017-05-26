/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.cluster.datastore.model;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;

public class PeopleModel {
    public static final QName BASE_QNAME = QName.create(
            "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test:people", "2014-03-13", "people");

    public static final QName PEOPLE_QNAME = QName.create(BASE_QNAME, "people");
    public static final QName PERSON_QNAME = QName.create(PEOPLE_QNAME, "person");
    public static final QName PERSON_NAME_QNAME = QName.create(PERSON_QNAME, "name");
    public static final QName PERSON_AGE_QNAME = QName.create(PERSON_QNAME, "age");

    public static final YangInstanceIdentifier BASE_PATH = YangInstanceIdentifier.of(BASE_QNAME);
    public static final YangInstanceIdentifier PERSON_LIST_PATH = BASE_PATH.node(PERSON_QNAME);

    public static NormalizedNode<?, ?> create() {

        // Create a list builder
        CollectionNodeBuilder<MapEntryNode, MapNode> cars =
            ImmutableMapNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(
                    PERSON_QNAME));

        // Create an entry for the person jack
        MapEntryNode jack =
            ImmutableNodes.mapEntryBuilder(PERSON_QNAME, PERSON_NAME_QNAME, "jack")
                .withChild(ImmutableNodes.leafNode(PERSON_NAME_QNAME, "jack"))
                .withChild(ImmutableNodes.leafNode(PERSON_AGE_QNAME, 100L))
                .build();

        // Create an entry for the person jill
        MapEntryNode jill =
            ImmutableNodes.mapEntryBuilder(PERSON_QNAME, PERSON_NAME_QNAME, "jill")
                .withChild(ImmutableNodes.leafNode(PERSON_NAME_QNAME, "jill"))
                .withChild(ImmutableNodes.leafNode(PERSON_AGE_QNAME, 200L))
                .build();

        cars.withChild(jack);
        cars.withChild(jill);

        return ImmutableContainerNodeBuilder.create()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(BASE_QNAME))
            .withChild(cars.build())
            .build();

    }

    public static NormalizedNode<?, ?> emptyContainer() {
        return ImmutableContainerNodeBuilder.create()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(BASE_QNAME))
            .build();
    }

    public static NormalizedNode<?, ?> newPersonMapNode() {
        return ImmutableNodes.mapNodeBuilder(PERSON_QNAME).build();
    }

    public static MapEntryNode newPersonEntry(String name) {
        return ImmutableNodes.mapEntryBuilder(PERSON_QNAME, PERSON_NAME_QNAME, name)
                .withChild(ImmutableNodes.leafNode(PERSON_NAME_QNAME, name)).build();
    }

    public static YangInstanceIdentifier newPersonPath(String name) {
        return YangInstanceIdentifier.builder(PERSON_LIST_PATH)
                .nodeWithKey(PERSON_QNAME, PERSON_NAME_QNAME, name).build();
    }
}
