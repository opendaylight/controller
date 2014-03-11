/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcResult;

public interface DataModification<P extends Path<P>, D> extends DataChange<P, D>, DataReader<P, D>,
        WriteableTransaction<P, D> {

    @Override
    public void putConfigurationData(P path, D data);

    @Override
    public void putOperationalData(P path, D data);

    @Override
    public void removeConfigurationData(P path);

    @Override
    public void removeOperationalData(P path);

    @Override
    public Future<RpcResult<TransactionStatus>> commit();

}
