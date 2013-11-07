/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Immutable;

public interface DataChangeEvent<P,D> extends DataChange<P, D>, Immutable {

    /**
     * Returns a new subtree of data, which starts at the path
     * where listener was registered.
     * 
     */
    D getUpdatedConfigurationSubtree();

    /**
     * Returns a new subtree of data, which starts at the path
     * where listener was registered.
     * 
     */
    D getUpdatedOperationalSubtree();
}
