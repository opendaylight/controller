/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncReadTransaction<P extends Path<P>, D> extends AsyncTransaction<P,D> {



    /**
     *
     * Reads data from provided logical data store located at provided path
     *
     *
     * @param store Logical data store from which read should occur.
     * @param path
     * @return
     */
    ListenableFuture<Optional<D>> read(LogicalDatastore store, P path);


    /**
     *
     * Closes transaction and resources allocated with it.
     *
     */
    @Override
    public void close();
}
