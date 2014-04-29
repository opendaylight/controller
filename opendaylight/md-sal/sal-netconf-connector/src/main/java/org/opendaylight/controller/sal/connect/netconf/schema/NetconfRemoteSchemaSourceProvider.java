/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.schema;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public final class NetconfRemoteSchemaSourceProvider implements SchemaSourceProvider<String> {

    public static final QName GET_SCHEMA_QNAME = QName.create(NetconfMessageTransformUtil.IETF_NETCONF_MONITORING,
            "get-schema");
    public static final QName GET_DATA_QNAME = QName
            .create(NetconfMessageTransformUtil.IETF_NETCONF_MONITORING, "data");

    private static final Logger logger = LoggerFactory.getLogger(NetconfRemoteSchemaSourceProvider.class);

    private final RpcImplementation rpc;
    private final RemoteDeviceId id;

    public NetconfRemoteSchemaSourceProvider(final RemoteDeviceId id, final RpcImplementation rpc) {
        this.id = id;
        this.rpc = Preconditions.checkNotNull(rpc);
    }

    @Override
    public Optional<String> getSchemaSource(final String moduleName, final Optional<String> revision) {
        final ImmutableCompositeNode getSchemaRequest = createGetSchemaRequest(moduleName, revision);

        logger.trace("{}: Loading YANG schema source for {}:{}", id, moduleName, revision);
        try {
            final RpcResult<CompositeNode> schemaReply = rpc.invokeRpc(GET_SCHEMA_QNAME, getSchemaRequest).get();
            if (schemaReply.isSuccessful()) {
                final Optional<String> schemaBody = getSchemaFromRpc(schemaReply.getResult());
                if (schemaBody.isPresent()) {
                    logger.debug("{}; YANG Schema successfully retrieved for {}:{}", id, moduleName, revision);
                    return schemaBody;
                }
            }
            // TODO add reply to log
            logger.warn("{}: YANG schema was not successfully retrieved for {}:{}. Errors: {}", id, moduleName,
                    revision, schemaReply.getErrors());
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("{}: YANG schema was not successfully retrieved for {}:{}.", id, moduleName, revision, e);
        }
        return Optional.absent();
    }

    private ImmutableCompositeNode createGetSchemaRequest(final String moduleName, final Optional<String> revision) {
        final CompositeNodeBuilder<ImmutableCompositeNode> request = ImmutableCompositeNode.builder(); //
        request.setQName(GET_SCHEMA_QNAME)
                .addLeaf("format", "yang")
                .addLeaf("identifier", moduleName);

        if (revision.isPresent()) {
            request.addLeaf("version", revision.get());
        }
        return request.toInstance();
    }

    private static Optional<String> getSchemaFromRpc(final CompositeNode result) {
        if (result == null) {
            return Optional.absent();
        }
        final SimpleNode<?> simpleNode = result.getFirstSimpleByName(GET_DATA_QNAME.withoutRevision());

        Preconditions.checkNotNull(simpleNode,
                "Unexpected response to get-schema, expected response with one child %s, but was %s",
                GET_DATA_QNAME.withoutRevision(), result);

        final Object potential = simpleNode.getValue();
        if (potential instanceof String) {
            return Optional.of((String) potential);
        }
        return Optional.absent();
    }
}
