/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

/**
 * Interface describing YANG 'deviation' statement.
 * <p>
 * The 'deviation' statement defines a hierarchy of a module that the device
 * does not implement faithfully. Deviations define the way a device deviate
 * from a standard.
 * </p>
 */
public interface Deviation {

    /**
     * Enum describing YANG deviation 'deviate' statement. It defines how the
     * device's implementation of the target node deviates from its original
     * definition.
     */
    enum Deviate {
        NOT_SUPPORTED, ADD, REPLACE, DELETE
    }

    /**
     * @return SchemaPath that identifies the node in the schema tree where a
     *         deviation from the module occurs.
     */
    SchemaPath getTargetPath();

    /**
     * @return deviate statement of this deviation
     */
    Deviate getDeviate();

    /**
     * @return textual cross-reference to an external document that provides
     *         additional information relevant to this node.
     */
    String getReference();

}
