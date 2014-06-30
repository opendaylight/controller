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
 *
 * @deprecated Replaced by org.opendaylight.controller.sal.core.spi.data.DOMStore Contract.
 */
@Deprecated
public interface DataStore<P extends Path<P>, D> extends //
        DataReader<P, D>, //
        DataModificationTransactionFactory<P, D> {

    @Override
    public DataModification<P, D> beginTransaction();

    @Override
    public D readConfigurationData(P path);

    @Override
    public D readOperationalData(P path);

}
