/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

class NetconfRemoteSchemaSourceProvider implements SchemaSourceProvider<String> {

    public static final QName IETF_NETCONF_MONITORING = QName.create(
            "urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring", "2010-10-04", "ietf-netconf-monitoring");
    public static final QName GET_SCHEMA_QNAME = QName.create(IETF_NETCONF_MONITORING, "get-schema");
    public static final QName GET_DATA_QNAME = QName.create(IETF_NETCONF_MONITORING, "data");

    private final NetconfDevice device;

    public NetconfRemoteSchemaSourceProvider(NetconfDevice device) {
        this.device = Preconditions.checkNotNull(device);
    }

    @Override
    public Optional<String> getSchemaSource(String moduleName, Optional<String> revision) {
        CompositeNodeBuilder<ImmutableCompositeNode> request = ImmutableCompositeNode.builder(); //
        request.setQName(GET_SCHEMA_QNAME) //
                .addLeaf("format", "yang") //
                .addLeaf("identifier", moduleName); //
        if (revision.isPresent()) {
            request.addLeaf("version", revision.get());
        }

        device.logger.trace("Loading YANG schema source for {}:{}", moduleName, revision);
        try {
            RpcResult<CompositeNode> schemaReply = device.invokeRpc(GET_SCHEMA_QNAME, request.toInstance()).get();
            if (schemaReply.isSuccessful()) {
                String schemaBody = getSchemaFromRpc(schemaReply.getResult());
                if (schemaBody != null) {
                    device.logger.trace("YANG Schema successfully retrieved from remote for {}:{}", moduleName, revision);
                    return Optional.of(schemaBody);
                }
            }
            device.logger.warn("YANG shcema was not successfully retrieved.");
        } catch (InterruptedException | ExecutionException e) {
            device.logger.warn("YANG shcema was not successfully retrieved.", e);
        }
        return Optional.absent();
    }

    private String getSchemaFromRpc(CompositeNode result) {
        if (result == null) {
            return null;
        }
        SimpleNode<?> simpleNode = result.getFirstSimpleByName(GET_DATA_QNAME.withoutRevision());
        Object potential = simpleNode.getValue();
        if (potential instanceof String) {
            return (String) potential;
        }
        return null;
    }

    public static final boolean isSupportedFor(Collection<QName> capabilities) {
        return capabilities.contains(IETF_NETCONF_MONITORING);
    }
}
