/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
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
    public NormalizedNode<?, ?> readConfigurationData(final YangInstanceIdentifier path);

    /**
     * Method reads Configuration DataStore for MountPoint by provided path.
     * @param mountPoint
     * @param path
     * @return read configuration data in mount point
     */
    public NormalizedNode<?, ?> readConfigurationData(final DOMMountPoint mountPoint, final YangInstanceIdentifier path);

    /**
     * Method reads Operational DataStore by provided path.
     * @param path
     * @return read operational data
     */
    public NormalizedNode<?, ?> readOperationalData(final YangInstanceIdentifier path);

    /**
     * Method read Operational DataStore for MountPoint by provided path.
     * @param mountPoint
     * @param path
     * @return read operational data
     */
    public NormalizedNode<?, ?> readOperationalData(final DOMMountPoint mountPoint, final YangInstanceIdentifier path);

    // PUT configuration
    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPut(
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload);

    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPut(
            final DOMMountPoint mountPoint, final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload);

    // POST configuration
    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPost(
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload);

    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPost(
            final DOMMountPoint mountPoint, final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload);

    // DELETE configuration
    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataDelete(
            final YangInstanceIdentifier path);

    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataDelete(
            final DOMMountPoint mountPoint, final YangInstanceIdentifier path);

    // RPC
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final SchemaPath type, final NormalizedNode<?, ?> input);
}
