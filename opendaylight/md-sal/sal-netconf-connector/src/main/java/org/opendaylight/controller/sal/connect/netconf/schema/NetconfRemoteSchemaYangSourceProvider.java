/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.schema;

import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.GET_SCHEMA_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DATA_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toId;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.dom.DOMSource;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Yang;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public final class NetconfRemoteSchemaYangSourceProvider implements SchemaSourceProvider<YangTextSchemaSource> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfRemoteSchemaYangSourceProvider.class);

    private static final ExceptionMapper<SchemaSourceException> MAPPER = new ExceptionMapper<SchemaSourceException>(
            "schemaDownload", SchemaSourceException.class) {
        @Override
        protected SchemaSourceException newWithCause(final String s, final Throwable throwable) {
            return new SchemaSourceException(s, throwable);
        }
    };

    private final DOMRpcService rpc;
    private final RemoteDeviceId id;

    public NetconfRemoteSchemaYangSourceProvider(final RemoteDeviceId id, final DOMRpcService rpc) {
        this.id = id;
        this.rpc = Preconditions.checkNotNull(rpc);
    }

    public static ContainerNode createGetSchemaRequest(final String moduleName, final Optional<String> revision) {
        final QName identifierQName = QName.cachedReference(QName.create(NetconfMessageTransformUtil.GET_SCHEMA_QNAME, "identifier"));
        final YangInstanceIdentifier.NodeIdentifier identifierId = new YangInstanceIdentifier.NodeIdentifier(identifierQName);
        final LeafNode<String> identifier = Builders.<String>leafBuilder().withNodeIdentifier(identifierId).withValue(moduleName).build();

        final QName formatQName = QName.cachedReference(QName.create(NetconfMessageTransformUtil.GET_SCHEMA_QNAME, "format"));
        final YangInstanceIdentifier.NodeIdentifier formatId = new YangInstanceIdentifier.NodeIdentifier(formatQName);
        final LeafNode<QName> format = Builders.<QName>leafBuilder().withNodeIdentifier(formatId).withValue(Yang.QNAME).build();

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> builder = Builders.containerBuilder();

        builder.withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NetconfMessageTransformUtil.GET_SCHEMA_QNAME))
        .withChild(identifier)
        .withChild(format);

        if(revision.isPresent()) {
            final QName revisionQName = QName.cachedReference(QName.create(NetconfMessageTransformUtil.GET_SCHEMA_QNAME, "version"));
            final YangInstanceIdentifier.NodeIdentifier revisionId = new YangInstanceIdentifier.NodeIdentifier(revisionQName);
            final LeafNode<String> revisionNode = Builders.<String>leafBuilder().withNodeIdentifier(revisionId).withValue(revision.get()).build();

            builder.withChild(revisionNode);
        }

        return builder.build();
    }

    private static Optional<String> getSchemaFromRpc(final RemoteDeviceId id, final NormalizedNode<?, ?> result) {
        if (result == null) {
            return Optional.absent();
        }

        final QName schemaWrapperNode = QName.cachedReference(QName.create(GET_SCHEMA_QNAME, NETCONF_DATA_QNAME.getLocalName()));
        final Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> child = ((ContainerNode) result).getChild(toId(schemaWrapperNode));

        Preconditions.checkState(child.isPresent() && child.get() instanceof AnyXmlNode,
                "%s Unexpected response to get-schema, expected response with one child %s, but was %s", id,
                schemaWrapperNode, result);

        final DOMSource wrappedNode = ((AnyXmlNode) child.get()).getValue();
        Preconditions.checkNotNull(wrappedNode.getNode());
        final Element dataNode = (Element) wrappedNode.getNode();

        return Optional.of(dataNode.getTextContent().trim());
    }

    @Override
    public CheckedFuture<YangTextSchemaSource, SchemaSourceException> getSource(final SourceIdentifier sourceIdentifier) {
        final String moduleName = sourceIdentifier.getName();

        // If formatted revision is SourceIdentifier.NOT_PRESENT_FORMATTED_REVISION, we have to omit it from request
        final String formattedRevision = sourceIdentifier.getRevision().equals(SourceIdentifier.NOT_PRESENT_FORMATTED_REVISION) ? null : sourceIdentifier.getRevision();
        final Optional<String> revision = Optional.fromNullable(formattedRevision);
        final NormalizedNode<?, ?> getSchemaRequest = createGetSchemaRequest(moduleName, revision);

        logger.trace("{}: Loading YANG schema source for {}:{}", id, moduleName, revision);

        final ListenableFuture<YangTextSchemaSource> transformed = Futures.transform(
                rpc.invokeRpc(SchemaPath.create(true, NetconfMessageTransformUtil.GET_SCHEMA_QNAME), getSchemaRequest),
                new ResultToYangSourceTransformer(id, sourceIdentifier, moduleName, revision));

        final CheckedFuture<YangTextSchemaSource, SchemaSourceException> checked = Futures.makeChecked(transformed, MAPPER);

        // / FIXME remove this get, it is only present to wait until source is retrieved
        // (goal is to limit concurrent schema download, since NetconfDevice listener does not handle concurrent messages properly)
        // TODO retest this
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
            Function<DOMRpcResult, YangTextSchemaSource> {

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
        public YangTextSchemaSource apply(final DOMRpcResult input) {

            if (input.getErrors().isEmpty()) {

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
        protected MoreObjects.ToStringHelper addToStringAttributes(final MoreObjects.ToStringHelper toStringHelper) {
            return toStringHelper.add("device", id);
        }

        @Override
        public InputStream openStream() throws IOException {
            return new ByteArrayInputStream(schemaString.get().getBytes(Charsets.UTF_8));
        }
    }
}
