/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import java.util.concurrent.Future;

import org.opendaylight.yangtools.concepts.Path;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncReadTransaction<P extends Path<P>, D> extends AsyncTransaction<P, D> {

    /**
     *
     * Reads data from provided logical data store located at provided path
     *
     *
     * @param store
     *            Logical data store from which read should occur.
     * @param path
     *            Path which uniquely identifies subtree which client want to
     *            read
     * @return Listenable Future which contains read result
     *         <ul>
     *         <li>If data at supplied path exists the {@link Future#get()}
     *         returns Optional object containing data
     *         <li>If data at supplied path does not exists the
     *         {@link Future#get()} returns {@link Optional#absent()}.
     *         </ul>
     */
    ListenableFuture<Optional<D>> read(LogicalDatastoreType store, P path);

}
