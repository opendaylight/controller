/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

import java.util.Map;
import java.util.Set;

public interface UsesNode {

    /**
     * @return path to 'grouping' on which this 'uses' statement points
     */
    SchemaPath getGroupingPath();

    /**
     * @return Set of augment statements defined under this uses node
     */
    Set<AugmentationSchema> getAugmentations();

    /**
     * Returns <code>true</code> if the data node was added by augmentation,
     * otherwise returns <code>false</code>
     *
     * @return <code>true</code> if the data node was added by augmentation,
     *         otherwise returns <code>false</code>
     */
    boolean isAugmenting();

    /**
     * Some of the properties of each node in the grouping can be refined with
     * the "refine" statement.
     *
     * @return Map, where key is schema path of refined node and value is
     *         refined node
     */
    Map<SchemaPath, SchemaNode> getRefines();
}
