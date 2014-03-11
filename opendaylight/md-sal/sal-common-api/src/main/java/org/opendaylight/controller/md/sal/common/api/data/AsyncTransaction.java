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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;


/**
 *
 * @author
 *
 * @param <P> Type of path (subtree identifier), which represents location in tree
 * @param <D> Type of data (payload), which represents data payload
 */
public interface AsyncTransaction<P extends Path<P>,D> extends //
    Identifiable<Object>,
    AsyncDataReader<P, D>,
    AutoCloseable {

    @Override
    public Object getIdentifier();

    @Override
    public ListenableFuture<Optional<D>> readConfigurationData(P path);

    @Override
    public ListenableFuture<Optional<D>> readOperationalData(P path);

    /**
     * Closes transaction.
     *
     *
     */
    @Override
    public void close();
}
