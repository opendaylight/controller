/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import java.util.concurrent.Future;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

public interface DOMStoreReadTransaction extends DOMStoreTransaction {

    /**
     *
     * Reads data from provided logical data store located at provided path
     *
     *
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
    ListenableFuture<Optional<NormalizedNode<?,?>>> read(InstanceIdentifier path);
}
