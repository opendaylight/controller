/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest
 *
 * Internal sal-rest-connector interface for defining a Broker's facade for DataBroker and
 * RPC Broker.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 22, 2015
 */
public interface RestBrokerFacade {

    /**
     * Method reads Configuration DataStore by provided path
     * @param path
     * @return read configuration data
     */
    NormalizedNode<?, ?> readConfigurationData(YangInstanceIdentifier path);

    /**
     * Method reads Configuration DataStore for MountPoint by provided path.
     * @param mountPoint
     * @param path
     * @return read configuration data in mount point
     */
    NormalizedNode<?, ?> readConfigurationData(DOMMountPoint mountPoint, YangInstanceIdentifier path);

    /**
     * Method reads Operational DataStore by provided path.
     * @param path
     * @return read operational data
     */
    NormalizedNode<?, ?> readOperationalData(YangInstanceIdentifier path);

    /**
     * Method read Operational DataStore for MountPoint by provided path.
     * @param mountPoint
     * @param path
     * @return read operational data
     */
    NormalizedNode<?, ?> readOperationalData(DOMMountPoint mountPoint, YangInstanceIdentifier path);

    // PUT configuration
    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPut(
            YangInstanceIdentifier path, NormalizedNode<?, ?> payload);

    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPut(
            DOMMountPoint mountPoint, YangInstanceIdentifier path, NormalizedNode<?, ?> payload);

    // POST configuration
    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPost(
            YangInstanceIdentifier path, NormalizedNode<?, ?> payload);

    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPost(
            DOMMountPoint mountPoint, YangInstanceIdentifier path, NormalizedNode<?, ?> payload);

    // DELETE configuration
    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataDelete(YangInstanceIdentifier path);

    CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataDelete(
            DOMMountPoint mountPoint, YangInstanceIdentifier path);

    // RPC
    CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(SchemaPath type, NormalizedNode<?, ?> input);

    void registerToListenDataChanges(LogicalDatastoreType datastore, DataChangeScope scope, ListenerAdapter listener);
}
