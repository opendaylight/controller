/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.cluster.datastore.model;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;

public final class CarsModel {
    public static final QName BASE_QNAME = QName.create(
            "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test:cars", "2014-03-13", "cars");

    public static final QName CARS_QNAME = QName.create(BASE_QNAME, "cars");
    public static final QName CAR_QNAME = QName.create(CARS_QNAME, "car");
    public static final QName CAR_NAME_QNAME = QName.create(CAR_QNAME, "name");
    public static final QName CAR_PRICE_QNAME = QName.create(CAR_QNAME, "price");

    public static final YangInstanceIdentifier BASE_PATH = YangInstanceIdentifier.of(BASE_QNAME);
    public static final YangInstanceIdentifier CAR_LIST_PATH = BASE_PATH.node(CAR_QNAME);

    private CarsModel() {

    }

    public static NormalizedNode<?, ?> create() {

        // Create a list builder
        CollectionNodeBuilder<MapEntryNode, MapNode> cars =
            ImmutableMapNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(
                    CAR_QNAME));

        // Create an entry for the car altima
        MapEntryNode altima =
            ImmutableNodes.mapEntryBuilder(CAR_QNAME, CAR_NAME_QNAME, "altima")
                .withChild(ImmutableNodes.leafNode(CAR_NAME_QNAME, "altima"))
                .withChild(ImmutableNodes.leafNode(CAR_PRICE_QNAME, Uint64.valueOf(1000)))
                .build();

        // Create an entry for the car accord
        MapEntryNode honda =
            ImmutableNodes.mapEntryBuilder(CAR_QNAME, CAR_NAME_QNAME, "accord")
                .withChild(ImmutableNodes.leafNode(CAR_NAME_QNAME, "accord"))
                .withChild(ImmutableNodes.leafNode(CAR_PRICE_QNAME, Uint64.valueOf("2000")))
                .build();

        cars.withChild(altima);
        cars.withChild(honda);

        return ImmutableContainerNodeBuilder.create()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(BASE_QNAME))
            .withChild(cars.build())
            .build();

    }

    public static NormalizedNode<?, ?> createEmptyCarsList() {
        return newCarsNode(newCarsMapNode());
    }

    public static ContainerNode newCarsNode(final MapNode carsList) {
        return ImmutableContainerNodeBuilder.create().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
                BASE_QNAME)).withChild(carsList).build();
    }

    public static MapNode newCarsMapNode(final MapEntryNode... carEntries) {
        CollectionNodeBuilder<MapEntryNode, MapNode> builder = ImmutableMapNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(CAR_QNAME));
        for (MapEntryNode e : carEntries) {
            builder.withChild(e);
        }

        return builder.build();
    }

    public static NormalizedNode<?, ?> emptyContainer() {
        return ImmutableContainerNodeBuilder.create()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(BASE_QNAME))
            .build();
    }

    public static NormalizedNode<?, ?> newCarMapNode() {
        return ImmutableNodes.mapNodeBuilder(CAR_QNAME).build();
    }

    public static MapEntryNode newCarEntry(final String name, final Uint64 price) {
        return ImmutableNodes.mapEntryBuilder(CAR_QNAME, CAR_NAME_QNAME, name)
                .withChild(ImmutableNodes.leafNode(CAR_NAME_QNAME, name))
                .withChild(ImmutableNodes.leafNode(CAR_PRICE_QNAME, price)).build();
    }

    public static YangInstanceIdentifier newCarPath(final String name) {
        return YangInstanceIdentifier.builder(CAR_LIST_PATH).nodeWithKey(CAR_QNAME, CAR_NAME_QNAME, name).build();
    }
}
