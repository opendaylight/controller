/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

/**
 * Transaction enabling client to have combined transaction,
 * which provides read and write capabilities.
 *
 *
 * @param <P> Path Type
 * @param <D> Data Type
 */
public interface AsyncReadWriteTransaction<P extends Path<P>, D> extends AsyncReadTransaction<P, D>,
        AsyncWriteTransaction<P, D> {

}
