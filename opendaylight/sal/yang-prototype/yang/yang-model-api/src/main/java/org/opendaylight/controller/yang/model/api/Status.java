/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

/**
 * Enum describing YANG 'status' statement. If no status is specified, the
 * default is CURRENT.
 */
public enum Status {

    /**
     * CURRENT means that the definition is current and valid.
     */
    CURRENT,

    /**
     * DEPRECATED indicates an obsolete definition, but it permits new/
     * continued implementation in order to foster interoperability with
     * older/existing implementations.
     */
    DEPRECATED,

    /**
     * OBSOLETE means the definition is obsolete and SHOULD NOT be implemented
     * and/or can be removed from implementations.
     */
    OBSOLETE

}
