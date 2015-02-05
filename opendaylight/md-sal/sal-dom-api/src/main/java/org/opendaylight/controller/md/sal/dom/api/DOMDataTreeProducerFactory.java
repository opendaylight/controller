/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import java.util.Collection;
import javax.annotation.Nonnull;

public interface DOMDataTreeProducerFactory {
    /**
     * Create a producer, which is able to access to a set of trees.
     *
     * @param allowTxMerges Allow egress state compression. True if the producer
     *                      is allowed to merge transactions, should it find it
     *                      convenient.
     * @return A {@link DOMDataTreeProducer} instance.
     * @throws {@link IllegalStateException} if there is an open transaction
     * @throws {@link IllegalArgumentException} if subtrees is empty.
     */
    @Nonnull DOMDataTreeProducer createProducer(@Nonnull Collection<DOMDataTreeIdentifier> subtrees);
}
