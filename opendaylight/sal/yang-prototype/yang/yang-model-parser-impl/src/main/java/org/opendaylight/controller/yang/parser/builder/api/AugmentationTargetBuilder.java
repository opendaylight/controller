/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

/**
 * Interface for builders of those nodes, which can be augmentation targets.
 */
public interface AugmentationTargetBuilder {

    /**
     * Add augment, which points to this node.
     *
     * @param augment
     *            augment which points to this node
     */
    void addAugmentation(AugmentationSchemaBuilder augment);

    /**
     * Build again already built data node.
     *
     * In general, when Builder.build is called first time, it creates YANG data
     * model node instance. With every other call it just return this instance
     * without checking for properties change. This method causes that builder
     * object process again all its properties and return an updated instance of
     * YANG data node.
     */
    void rebuild();

}
