/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Reader for reading YANG subtrees based on their path.
 *
 * Reader is requested to return object at specified path and all it's subnodes
 * known to the reader or null if node is not found in this reader.
 *
 * @param <P>
 *            Path Type
 * @param <D>
 *            Data Type
 */
public interface AsyncDataReader<P extends Path<P>, D> {

    /**
     * Reads data from Operational data store located at provided path
     *
     * @param path
     *            Path to data
     * @return ListenableFuture containing Optional object with read result.
     *         Future contains {@link Optional#absent()} when data are not
     *         present in datastore.
     */
    ListenableFuture<Optional<D>> readOperationalData(P path);

    /**
     * Reads data from Configuration data store located at provided path
     *
     * @param path
     *            Path to data
     * @return ListenableFuture containing Optional object with read result.
     *         Future contains {@link Optional#absent()} when data are not
     *         present in datastore.
     */
    ListenableFuture<Optional<D>> readConfigurationData(P path);
}
