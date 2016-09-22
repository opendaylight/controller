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
 * A common parent for all transactions which operate on a conceptual data tree.
 *
 * See derived transaction types for more concrete behavior:
 * <ul>
 * <li>{@link AsyncReadTransaction} - Read capabilities, user is able to read data from data tree</li>
 * <li>{@link AsyncWriteTransaction} - Write capabilities, user is able to propose changes to data tree</li>
 * <li>{@link AsyncReadWriteTransaction} - Read and Write capabilities, user is able to read state and to propose changes of state.</li>
 * </ul>
 *
 * <b>Implementation Note:</b> This interface is not intended to be implemented
 * by users of MD-SAL.
 *
 * @param <P> Type of path (subtree identifier), which represents location in tree
 * @param <D> Type of data (payload), which represents data payload
 */
public interface AsyncTransaction<P extends Path<P>,D> extends //
    Identifiable<Object> {

    @Override
    Object getIdentifier();


}
