/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.Path;

/**
 *
 *
 *
 * @param <P>
 * @param <D>
 * @deprecated Replaced by {@link AsyncDataChangeEvent}
 */
@Deprecated
public interface DataChangeEvent<P extends Path<P>,D> extends DataChange<P, D>, Immutable {

    /**
     * Returns a orignal subtree of data, which starts at the path
     * where listener was registered.
     *
     */
    D getOriginalConfigurationSubtree();

    /**
     * Returns a new subtree of data, which starts at the path
     * where listener was registered.
     *
     */
    D getOriginalOperationalSubtree();



    /**
     * Returns a updated subtree of data, which starts at the path
     * where listener was registered.
     *
     */
    D getUpdatedConfigurationSubtree();

    /**
     * Returns a udpated subtree of data, which starts at the path
     * where listener was registered.
     *
     */
    D getUpdatedOperationalSubtree();
}
