/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.schema;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfRemoteSchemaYangSourceProvider implements SchemaSourceProvider<YangTextSchemaSource> {

    public static final QName GET_SCHEMA_QNAME = QName.create(NetconfMessageTransformUtil.IETF_NETCONF_MONITORING,"get-schema");
    public static final QName GET_DATA_QNAME = QName.create(NetconfMessageTransformUtil.IETF_NETCONF_MONITORING, "data");

    private static final Logger logger = LoggerFactory.getLogger(NetconfRemoteSchemaYangSourceProvider.class);

    private static final ExceptionMapper<SchemaSourceException> MAPPER = new ExceptionMapper<SchemaSourceException>(
            "schemaDownload", SchemaSourceException.class) {
        @Override
        protected SchemaSourceException newWithCause(final String s, final Throwable throwable) {
            return new SchemaSourceException(s, throwable);
        }
    };

    private final RpcImplementation rpc;
    private final RemoteDeviceId id;

    public NetconfRemoteSchemaYangSourceProvider(final RemoteDeviceId id, final RpcImplementation rpc) {
        this.id = id;
        this.rpc = Preconditions.checkNotNull(rpc);
    }

    private ImmutableCompositeNode createGetSchemaRequest(final String moduleName, final Optional<String> revision) {
        final CompositeNodeBuilder<ImmutableCompositeNode> request = ImmutableCompositeNode.builder();
        request.setQName(GET_SCHEMA_QNAME).addLeaf("identifier", moduleName);
        if (revision.isPresent()) {
            request.addLeaf("version", revision.get());
        }
        request.addLeaf("format", "yang");
        return request.build();
    }

    private static Optional<String> getSchemaFromRpc(final RemoteDeviceId id, final CompositeNode result) {
        if (result == null) {
            return Optional.absent();
        }
        final SimpleNode<?> simpleNode = result.getFirstSimpleByName(GET_DATA_QNAME.withoutRevision());

        Preconditions.checkNotNull(simpleNode,
                "%s Unexpected response to get-schema, expected response with one child %s, but was %s", id,
                GET_DATA_QNAME.withoutRevision(), result);

        final Object potential = simpleNode.getValue();
        return potential instanceof String ? Optional.of((String) potential) : Optional.<String> absent();
    }

    @Override
    public CheckedFuture<YangTextSchemaSource, SchemaSourceException> getSource(final SourceIdentifier sourceIdentifier) {
        final String moduleName = sourceIdentifier.getName();

        // If formatted revision is SourceIdentifier.NOT_PRESENT_FORMATTED_REVISION, we have to omit it from request
        final String formattedRevision = sourceIdentifier.getRevision().equals(SourceIdentifier.NOT_PRESENT_FORMATTED_REVISION) ? null : sourceIdentifier.getRevision();
        final Optional<String> revision = Optional.fromNullable(formattedRevision);
        final ImmutableCompositeNode getSchemaRequest = createGetSchemaRequest(moduleName, revision);

        logger.trace("{}: Loading YANG schema source for {}:{}", id, moduleName, revision);

        final ListenableFuture<YangTextSchemaSource> transformed = Futures.transform(
                rpc.invokeRpc(GET_SCHEMA_QNAME, getSchemaRequest),
                new ResultToYangSourceTransformer(id, sourceIdentifier, moduleName, revision));

        final CheckedFuture<YangTextSchemaSource, SchemaSourceException> checked = Futures.makeChecked(transformed, MAPPER);

        // / FIXME remove this get, it is only present to wait until source is retrieved
        // (goal is to limit concurrent schema download, since NetconfDevice listener does not handle concurrent messages properly)
        try {
            logger.trace("{}: Blocking for {}", id, sourceIdentifier);
            checked.checkedGet();
        } catch (final SchemaSourceException e) {
            return Futures.immediateFailedCheckedFuture(e);
        }

        return checked;
    }

    /**
     * Transform composite node to string schema representation and then to ASTSchemaSource
     */
    private static final class ResultToYangSourceTransformer implements
            Function<RpcResult<CompositeNode>, YangTextSchemaSource> {

        private final RemoteDeviceId id;
        private final SourceIdentifier sourceIdentifier;
        private final String moduleName;
        private final Optional<String> revision;

        public ResultToYangSourceTransformer(final RemoteDeviceId id, final SourceIdentifier sourceIdentifier,
                final String moduleName, final Optional<String> revision) {
            this.id = id;
            this.sourceIdentifier = sourceIdentifier;
            this.moduleName = moduleName;
            this.revision = revision;
        }

        @Override
        public YangTextSchemaSource apply(final RpcResult<CompositeNode> input) {

            if (input.isSuccessful()) {

                final Optional<String> schemaString = getSchemaFromRpc(id, input.getResult());

                Preconditions.checkState(schemaString.isPresent(),
                        "%s: Unexpected response to get-schema, schema not present in message for: %s", id, sourceIdentifier);

                logger.debug("{}: YANG Schema successfully retrieved for {}:{}", id, moduleName, revision);

                return new NetconfYangTextSchemaSource(id, sourceIdentifier, schemaString);
            }

            logger.warn("{}: YANG schema was not successfully retrieved for {}. Errors: {}", id, sourceIdentifier,
                    input.getErrors());

            throw new IllegalStateException(String.format(
                    "%s: YANG schema was not successfully retrieved for %s. Errors: %s", id, sourceIdentifier,
                    input.getErrors()));

        }

    }

    private static class NetconfYangTextSchemaSource extends YangTextSchemaSource {
        private final RemoteDeviceId id;
        private final Optional<String> schemaString;

        public NetconfYangTextSchemaSource(final RemoteDeviceId id, final SourceIdentifier sId, final Optional<String> schemaString) {
            super(sId);
            this.id = id;
            this.schemaString = schemaString;
        }

        @Override
        protected Objects.ToStringHelper addToStringAttributes(final Objects.ToStringHelper toStringHelper) {
            return toStringHelper.add("device", id);
        }

        @Override
        public InputStream openStream() throws IOException {
            return IOUtils.toInputStream(schemaString.get());
        }
    }
}
