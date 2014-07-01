/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.CONFIG_SOURCE_RUNNING;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DATA_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_GET_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_GET_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toFilterStructure;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;

public final class NetconfDeviceDataReader implements DataReader<InstanceIdentifier,CompositeNode> {

    private final RpcImplementation rpc;
    private final RemoteDeviceId id;

    public NetconfDeviceDataReader(final RemoteDeviceId id, final RpcImplementation rpc) {
        this.id = id;
        this.rpc = rpc;
    }

    @Override
    public CompositeNode readConfigurationData(final InstanceIdentifier path) {
        final RpcResult<CompositeNode> result;
        try {
            result = rpc.invokeRpc(NETCONF_GET_CONFIG_QNAME,
                    NetconfMessageTransformUtil.wrap(NETCONF_GET_CONFIG_QNAME, CONFIG_SOURCE_RUNNING, toFilterStructure(path))).get();
        } catch (final InterruptedException e) {
            throw onInterruptedException(e);
        } catch (final ExecutionException e) {
            throw new RuntimeException(id + ": Read configuration data " + path + " failed", e);
        }

        final CompositeNode data = result.getResult().getFirstCompositeByName(NETCONF_DATA_QNAME);
        return data == null ? null : (CompositeNode) findNode(data, path);
    }

    private RuntimeException onInterruptedException(final InterruptedException e) {
        Thread.currentThread().interrupt();
        return new RuntimeException(id + ": Interrupted while waiting for response", e);
    }

    @Override
    public CompositeNode readOperationalData(final InstanceIdentifier path) {
        final RpcResult<CompositeNode> result;
        try {
            result = rpc.invokeRpc(NETCONF_GET_QNAME, NetconfMessageTransformUtil.wrap(NETCONF_GET_QNAME, toFilterStructure(path))).get();
        } catch (final InterruptedException e) {
            throw onInterruptedException(e);
        } catch (final ExecutionException e) {
            throw new RuntimeException(id + ": Read operational data " + path + " failed", e);
        }

        final CompositeNode data = result.getResult().getFirstCompositeByName(NETCONF_DATA_QNAME);
        return (CompositeNode) findNode(data, path);
    }

    private static Node<?> findNode(final CompositeNode node, final InstanceIdentifier identifier) {

        Node<?> current = node;
        for (final InstanceIdentifier.PathArgument arg : identifier.getPathArguments()) {
            if (current instanceof SimpleNode<?>) {
                return null;
            } else if (current instanceof CompositeNode) {
                final CompositeNode currentComposite = (CompositeNode) current;

                current = currentComposite.getFirstCompositeByName(arg.getNodeType());
                if (current == null) {
                    current = currentComposite.getFirstCompositeByName(arg.getNodeType().withoutRevision());
                }
                if (current == null) {
                    current = currentComposite.getFirstSimpleByName(arg.getNodeType());
                }
                if (current == null) {
                    current = currentComposite.getFirstSimpleByName(arg.getNodeType().withoutRevision());
                }
                if (current == null) {
                    return null;
                }
            }
        }
        return current;
    }
}
