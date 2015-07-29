/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.rest.connector;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.rest.streams.listeners.ListenerAdapter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

import com.google.common.util.concurrent.CheckedFuture;

public interface RestBrokerFacade {

    void setRpcService(@Nonnull final DOMRpcService rpcService);

    NormalizedNode<?, ?> readConfigurationData(YangInstanceIdentifier normalizedII);

    NormalizedNode<?, ?> readConfigurationData(DOMMountPoint mountPoint, YangInstanceIdentifier normalizedII);

    NormalizedNode<?, ?> readOperationalData(YangInstanceIdentifier normalizedII);

    NormalizedNode<?, ?> readOperationalData(DOMMountPoint mountPoint, YangInstanceIdentifier normalizedII);

    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPut(SchemaContext globalSchema,
            YangInstanceIdentifier path, NormalizedNode<?, ?> payload);

    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPut(DOMMountPoint mountPoint,
            YangInstanceIdentifier path, NormalizedNode<?, ?> payload);

    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPost(SchemaContext globalSchema,
            YangInstanceIdentifier path, NormalizedNode<?, ?> payload);

    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPost(DOMMountPoint mountPoint,
            YangInstanceIdentifier path, NormalizedNode<?, ?> payload);

    /**
     * @param globalSchema
     * @param path
     * @param payload
     * @return
     */
    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPatch(SchemaContext globalSchema,
            @CheckForNull YangInstanceIdentifier path, NormalizedNode<?, ?> payload);

    /**
     * @param mountPoint
     * @param path
     * @param payload
     * @return
     */
    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPatch(DOMMountPoint mountPoint,
            YangInstanceIdentifier path, NormalizedNode<?, ?> payload);

    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataDelete(YangInstanceIdentifier path);

    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataDelete(DOMMountPoint mountPoint,
            YangInstanceIdentifier path);

    CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(SchemaPath type, NormalizedNode<?, ?> input);

    void registerToListenDataChanges(LogicalDatastoreType datastore, DataChangeScope scope, ListenerAdapter listener);

}
