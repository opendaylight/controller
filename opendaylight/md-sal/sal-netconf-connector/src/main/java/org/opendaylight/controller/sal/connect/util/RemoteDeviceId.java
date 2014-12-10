/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.util;

import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.common.QName;

public class RemoteDeviceId {

    private final String name;
    private final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier path;
    private final InstanceIdentifier<Node> bindingPath;
    private final NodeKey key;

    public RemoteDeviceId(final ModuleIdentifier identifier) {
        this(Preconditions.checkNotNull(identifier).getInstanceName());
    }

    public RemoteDeviceId(final String name) {
        Preconditions.checkNotNull(name);
        this.name = name;
        this.key = new NodeKey(new NodeId(name));
        this.path = createBIPath(name);
        this.bindingPath = createBindingPath(key);
    }

    private static InstanceIdentifier<Node> createBindingPath(final NodeKey key) {
        return InstanceIdentifier.builder(Nodes.class).child(Node.class, key).build();
    }

    private static org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier createBIPath(final String name) {
        final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder builder =
                org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder();
        builder.node(Nodes.QNAME).nodeWithKey(Node.QNAME, QName.create(Node.QNAME.getNamespace(), Node.QNAME.getRevision(), "id"), name);

        return builder.build();
    }

    public String getName() {
        return name;
    }

    public InstanceIdentifier<Node> getBindingPath() {
        return bindingPath;
    }

    public org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier getPath() {
        return path;
    }

    public NodeKey getBindingKey() {
        return key;
    }

    @Override
    public String toString() {
        return "RemoteDevice{" + name +'}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemoteDeviceId)) {
            return false;
        }

        final RemoteDeviceId that = (RemoteDeviceId) o;

        if (!name.equals(that.name)) {
            return false;
        }
        if (!bindingPath.equals(that.bindingPath)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + bindingPath.hashCode();
        return result;
    }
}
