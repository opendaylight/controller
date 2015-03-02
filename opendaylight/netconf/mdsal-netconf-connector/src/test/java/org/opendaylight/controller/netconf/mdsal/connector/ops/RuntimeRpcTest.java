/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.mdsal.connector.CurrentSchemaContext;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.builder.impl.BuilderUtils;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class RuntimeRpcTest {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeRpcTest.class);

    private String sessionIdForReporting = "netconf-test-session1";

    private static Document RPC_REPLY_OK = null;

    static {
        try {
            RPC_REPLY_OK = XmlFileLoader.xmlFileToDocument("messages/mapping/rpcs/runtimerpc-ok-reply.xml");
        } catch (Exception e) {
            LOG.debug("unable to load rpc reply ok.", e);
            RPC_REPLY_OK = XmlUtil.newDocument();
        }
    }

    private DOMRpcService rpcServiceVoidInvoke = new DOMRpcService() {
        @Nonnull
        @Override
        public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull SchemaPath type, @Nullable NormalizedNode<?, ?> input) {
            return Futures.immediateCheckedFuture((DOMRpcResult) new DefaultDOMRpcResult(null, Collections.<RpcError>emptyList()));
        }

        @Nonnull
        @Override
        public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(@Nonnull T listener) {
            return null;
        }
    };

    private DOMRpcService rpcServiceFailedInvocation = new DOMRpcService() {
        @Nonnull
        @Override
        public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull SchemaPath type, @Nullable NormalizedNode<?, ?> input) {
            return Futures.immediateFailedCheckedFuture((DOMRpcException) new DOMRpcException("rpc invocation not implemented yet") {
            });
        }

        @Nonnull
        @Override
        public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(@Nonnull T listener) {
            return null;
        }
    };

    private DOMRpcService rpcServiceSuccesfullInvocation = new DOMRpcService() {
        @Nonnull
        @Override
        public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull SchemaPath type, @Nullable NormalizedNode<?, ?> input) {
            Collection<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> children = (Collection) input.getValue();
            Module module = schemaContext.findModuleByNamespaceAndRevision(type.getLastComponent().getNamespace(), null);
            RpcDefinition rpcDefinition = getRpcDefinitionFromModule(module, module.getNamespace(), type.getLastComponent().getLocalName());
            ContainerSchemaNode outputSchemaNode = rpcDefinition.getOutput();
            ContainerNode node =ImmutableContainerNodeBuilder.create()
                    .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(outputSchemaNode.getQName()))
                    .withValue(children).build();

            return Futures.immediateCheckedFuture((DOMRpcResult) new DefaultDOMRpcResult(node));
        }

        @Nonnull
        @Override
        public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(@Nonnull T listener) {
            return null;
        }
    };

    private SchemaContext schemaContext = null;
    private CurrentSchemaContext currentSchemaContext = null;

    @Before
    public void setUp() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);

        this.schemaContext = parseSchemas(getYangSchemas());
        this.currentSchemaContext = new CurrentSchemaContext(createSchemaService());
    }

    @Test
    public void testVoidRpc() throws Exception {
        RuntimeRpc rpc = new RuntimeRpc(sessionIdForReporting, currentSchemaContext, rpcServiceVoidInvoke);

        Document rpcDocument = XmlFileLoader.xmlFileToDocument("messages/mapping/rpcs/rpc-void.xml");
        HandlingPriority priority = rpc.canHandle(rpcDocument);
        Preconditions.checkState(priority != HandlingPriority.CANNOT_HANDLE);

        Document response = rpc.handle(rpcDocument, NetconfOperationChainedExecution.EXECUTION_TERMINATION_POINT);

        verifyResponse(response, RPC_REPLY_OK);
    }

    @Test
    public void testSuccesfullInvocation() throws Exception {
        RuntimeRpc rpc = new RuntimeRpc(sessionIdForReporting, currentSchemaContext, rpcServiceSuccesfullInvocation);

        Document rpcDocument = XmlFileLoader.xmlFileToDocument("messages/mapping/rpcs/rpc-nonvoid.xml");
        HandlingPriority priority = rpc.canHandle(rpcDocument);
        Preconditions.checkState(priority != HandlingPriority.CANNOT_HANDLE);

        Document response = rpc.handle(rpcDocument, NetconfOperationChainedExecution.EXECUTION_TERMINATION_POINT);
        verifyResponse(response, XmlFileLoader.xmlFileToDocument("messages/mapping/rpcs/rpc-nonvoid-control.xml"));
    }

    @Test
    public void testFailedInvocation() throws Exception {
        RuntimeRpc rpc = new RuntimeRpc(sessionIdForReporting, currentSchemaContext, rpcServiceFailedInvocation);

        Document rpcDocument = XmlFileLoader.xmlFileToDocument("messages/mapping/rpcs/rpc-nonvoid.xml");
        HandlingPriority priority = rpc.canHandle(rpcDocument);
        Preconditions.checkState(priority != HandlingPriority.CANNOT_HANDLE);

        try {
            rpc.handle(rpcDocument, NetconfOperationChainedExecution.EXECUTION_TERMINATION_POINT);
            fail("should have failed with rpc invocation not implemented yet");
        } catch (NetconfDocumentedException e) {
            assertTrue(e.getErrorType() == ErrorType.application);
            assertTrue(e.getErrorSeverity() == ErrorSeverity.error);
            assertTrue(e.getErrorTag() == ErrorTag.operation_failed);
        }
    }

    private void verifyResponse(Document response, Document template) {
        DetailedDiff dd = new DetailedDiff(new Diff(response, template));
        dd.overrideElementQualifier(new RecursiveElementNameAndTextQualifier());
        assertTrue(dd.similar());
    }

    private RpcDefinition getRpcDefinitionFromModule(Module module, URI namespaceURI, String name) {
        for (RpcDefinition rpcDef : module.getRpcs()) {
            if (rpcDef.getQName().getNamespace().equals(namespaceURI)
                    && rpcDef.getQName().getLocalName().equals(name)) {
                return rpcDef;
            }
        }

        return null;

    }

    private Collection<InputStream> getYangSchemas() {
        final List<String> schemaPaths = Arrays.asList("/yang/mdsal-netconf-rpc-test.yang");
        final List<InputStream> schemas = new ArrayList<>();

        for (String schemaPath : schemaPaths) {
            InputStream resourceAsStream = getClass().getResourceAsStream(schemaPath);
            schemas.add(resourceAsStream);
        }

        return schemas;
    }

    private SchemaContext parseSchemas(Collection<InputStream> schemas) throws IOException, YangSyntaxErrorException {
        final YangParserImpl parser = new YangParserImpl();
        Collection<ByteSource> sources = BuilderUtils.streamsToByteSources(schemas);
        return parser.parseSources(sources);
    }

    private SchemaService createSchemaService() {
        return new SchemaService() {

            @Override
            public void addModule(Module module) {
            }

            @Override
            public void removeModule(Module module) {

            }

            @Override
            public SchemaContext getSessionContext() {
                return schemaContext;
            }

            @Override
            public SchemaContext getGlobalContext() {
                return schemaContext;
            }

            @Override
            public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(final SchemaContextListener listener) {
                listener.onGlobalContextUpdated(getGlobalContext());
                return new ListenerRegistration<SchemaContextListener>() {
                    @Override
                    public void close() {

                    }

                    @Override
                    public SchemaContextListener getInstance() {
                        return listener;
                    }
                };
            }
        };
    }
}
