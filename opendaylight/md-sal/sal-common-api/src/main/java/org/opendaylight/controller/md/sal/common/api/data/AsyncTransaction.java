/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Path;


/**
 *
 * @author
 *
 * @param <P> Type of path (subtree identifier), which represents location in tree
 * @param <D> Type of data (payload), which represents data payload
 */
public interface AsyncTransaction<P extends Path<P>,D> extends //
    Identifiable<Object>,
    AutoCloseable {

    @Override
    public Object getIdentifier();

    /**
     * Closes transaction and releases all resources associated with it.
     */
    @Override
    public void close();
}
