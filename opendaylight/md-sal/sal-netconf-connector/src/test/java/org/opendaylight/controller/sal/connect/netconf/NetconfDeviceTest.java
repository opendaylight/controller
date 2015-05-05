/*
* Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.sal.connect.netconf;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceRpc;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleImport;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.repo.api.MissingSchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaContextFactory;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaResolutionException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.spi.PotentialSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistration;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistry;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class NetconfDeviceTest {

    private static final NetconfMessage notification;

    private static final ContainerNode compositeNode;

    static {
        try {
            compositeNode = mockClass(ContainerNode.class);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        try {
            notification = new NetconfMessage(XmlUtil.readXmlToDocument(NetconfDeviceTest.class.getResourceAsStream("/notification-payload.xml")));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final DOMRpcResult rpcResultC = new DefaultDOMRpcResult(compositeNode);

    public static final String TEST_NAMESPACE = "test:namespace";
    public static final String TEST_MODULE = "test-module";
    public static final String TEST_REVISION = "2013-07-22";
    public static final SourceIdentifier TEST_SID = new SourceIdentifier(TEST_MODULE, Optional.of(TEST_REVISION));
    public static final String TEST_CAPABILITY = TEST_NAMESPACE + "?module=" + TEST_MODULE + "&amp;revision=" + TEST_REVISION;

    public static final SourceIdentifier TEST_SID2 = new SourceIdentifier(TEST_MODULE + "2", Optional.of(TEST_REVISION));
    public static final String TEST_CAPABILITY2 = TEST_NAMESPACE + "?module=" + TEST_MODULE + "2" + "&amp;revision=" + TEST_REVISION;

    private static final NetconfStateSchemas.NetconfStateSchemasResolver stateSchemasResolver = new NetconfStateSchemas.NetconfStateSchemasResolver() {

        @Override
        public NetconfStateSchemas resolve(final NetconfDeviceRpc deviceRpc, final NetconfSessionPreferences remoteSessionCapabilities, final RemoteDeviceId id) {
            return NetconfStateSchemas.EMPTY;
        }
    };

    @Test
    public void testNetconfDeviceFailFirstSchemaFailSecondEmpty() throws Exception {
        final ArrayList<String> capList = Lists.newArrayList(TEST_CAPABILITY);

        final RemoteDeviceHandler<NetconfSessionPreferences> facade = getFacade();
        final NetconfDeviceCommunicator listener = getListener();

        final SchemaContextFactory schemaFactory = getSchemaFactory();

        // Make fallback attempt to fail due to empty resolved sources
        final SchemaResolutionException schemaResolutionException
                = new SchemaResolutionException("fail first",
                Collections.<SourceIdentifier>emptyList(), HashMultimap.<SourceIdentifier, ModuleImport>create());
        doReturn(Futures.immediateFailedCheckedFuture(
                schemaResolutionException))
                .when(schemaFactory).createSchemaContext(anyCollectionOf(SourceIdentifier.class));

        final NetconfDevice.SchemaResourcesDTO schemaResourcesDTO
                = new NetconfDevice.SchemaResourcesDTO(getSchemaRegistry(), schemaFactory, stateSchemasResolver);
        final NetconfDevice device = new NetconfDevice(schemaResourcesDTO, getId(), facade, getExecutor(),true);
        // Monitoring not supported
        final NetconfSessionPreferences sessionCaps = getSessionCaps(false, capList);
        device.onRemoteSessionUp(sessionCaps, listener);

        Mockito.verify(facade, Mockito.timeout(5000)).onDeviceDisconnected();
        Mockito.verify(listener, Mockito.timeout(5000)).close();
        Mockito.verify(schemaFactory, times(1)).createSchemaContext(anyCollectionOf(SourceIdentifier.class));
    }

    @Test
    public void testNetconfDeviceMissingSource() throws Exception {
        final RemoteDeviceHandler<NetconfSessionPreferences> facade = getFacade();
        final NetconfDeviceCommunicator listener = getListener();
        final SchemaContext schema = getSchema();

        final SchemaContextFactory schemaFactory = getSchemaFactory();

        // Make fallback attempt to fail due to empty resolved sources
        final MissingSchemaSourceException schemaResolutionException = new MissingSchemaSourceException("fail first", TEST_SID);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                if(((Collection<?>) invocation.getArguments()[0]).size() == 2) {
                    return Futures.immediateFailedCheckedFuture(schemaResolutionException);
                } else {
                    return Futures.immediateCheckedFuture(schema);
                }
            }
        }).when(schemaFactory).createSchemaContext(anyCollectionOf(SourceIdentifier.class));

        final NetconfDevice.SchemaResourcesDTO schemaResourcesDTO
                = new NetconfDevice.SchemaResourcesDTO(getSchemaRegistry(), schemaFactory, new NetconfStateSchemas.NetconfStateSchemasResolver() {
            @Override
            public NetconfStateSchemas resolve(final NetconfDeviceRpc deviceRpc, final NetconfSessionPreferences remoteSessionCapabilities, final RemoteDeviceId id) {
                final Module first = Iterables.getFirst(schema.getModules(), null);
                final QName qName = QName.create(first.getQNameModule(), first.getName());
                final NetconfStateSchemas.RemoteYangSchema source1 = new NetconfStateSchemas.RemoteYangSchema(qName);
                final NetconfStateSchemas.RemoteYangSchema source2 = new NetconfStateSchemas.RemoteYangSchema(QName.create(first.getQNameModule(), "test-module2"));
                return new NetconfStateSchemas(Sets.newHashSet(source1, source2));
            }
        });

        final NetconfDevice device = new NetconfDevice(schemaResourcesDTO, getId(), facade, getExecutor(), true);
        // Monitoring supported
        final NetconfSessionPreferences sessionCaps = getSessionCaps(true, Lists.newArrayList(TEST_CAPABILITY, TEST_CAPABILITY2));
        device.onRemoteSessionUp(sessionCaps, listener);

        Mockito.verify(facade, Mockito.timeout(5000)).onDeviceConnected(any(SchemaContext.class), any(NetconfSessionPreferences.class), any(NetconfDeviceRpc.class));
        Mockito.verify(schemaFactory, times(2)).createSchemaContext(anyCollectionOf(SourceIdentifier.class));
    }

    private SchemaSourceRegistry getSchemaRegistry() {
        final SchemaSourceRegistry mock = mock(SchemaSourceRegistry.class);
        final SchemaSourceRegistration<?> mockReg = mock(SchemaSourceRegistration.class);
        doNothing().when(mockReg).close();
        doReturn(mockReg).when(mock).registerSchemaSource(any(org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider.class), any(PotentialSchemaSource.class));
        return mock;
    }

    @Test
    public void testNotificationBeforeSchema() throws Exception {
        final RemoteDeviceHandler<NetconfSessionPreferences> facade = getFacade();
        final NetconfDeviceCommunicator listener = getListener();

        final NetconfDevice.SchemaResourcesDTO schemaResourcesDTO
                = new NetconfDevice.SchemaResourcesDTO(getSchemaRegistry(), getSchemaFactory(), stateSchemasResolver);
        final NetconfDevice device = new NetconfDevice(schemaResourcesDTO, getId(), facade, getExecutor(), true);

        device.onNotification(notification);
        device.onNotification(notification);

        verify(facade, times(0)).onNotification(any(DOMNotification.class));

        final NetconfSessionPreferences sessionCaps = getSessionCaps(true,
                Lists.newArrayList(TEST_CAPABILITY));

        final DOMRpcService deviceRpc = mock(DOMRpcService.class);

        device.handleSalInitializationSuccess(NetconfToNotificationTest.getNotificationSchemaContext(getClass()), sessionCaps, deviceRpc);

        verify(facade, timeout(10000).times(2)).onNotification(any(DOMNotification.class));

        device.onNotification(notification);
        verify(facade, timeout(10000).times(3)).onNotification(any(DOMNotification.class));
    }

    @Test
    public void testNetconfDeviceReconnect() throws Exception {
        final RemoteDeviceHandler<NetconfSessionPreferences> facade = getFacade();
        final NetconfDeviceCommunicator listener = getListener();

        final SchemaContextFactory schemaContextProviderFactory = getSchemaFactory();

        final NetconfDevice.SchemaResourcesDTO schemaResourcesDTO
                = new NetconfDevice.SchemaResourcesDTO(getSchemaRegistry(), schemaContextProviderFactory, stateSchemasResolver);
        final NetconfDevice device = new NetconfDevice(schemaResourcesDTO, getId(), facade, getExecutor(), true);
        final NetconfSessionPreferences sessionCaps = getSessionCaps(true,
                Lists.newArrayList(TEST_NAMESPACE + "?module=" + TEST_MODULE + "&amp;revision=" + TEST_REVISION));
        device.onRemoteSessionUp(sessionCaps, listener);

        verify(schemaContextProviderFactory, timeout(5000)).createSchemaContext(any(Collection.class));
        verify(facade, timeout(5000)).onDeviceConnected(any(SchemaContext.class), any(NetconfSessionPreferences.class), any(DOMRpcService.class));

        device.onRemoteSessionDown();
        verify(facade, timeout(5000)).onDeviceDisconnected();

        device.onRemoteSessionUp(sessionCaps, listener);

        verify(schemaContextProviderFactory, timeout(5000).times(2)).createSchemaContext(any(Collection.class));
        verify(facade, timeout(5000).times(2)).onDeviceConnected(any(SchemaContext.class), any(NetconfSessionPreferences.class), any(DOMRpcService.class));
    }

    private SchemaContextFactory getSchemaFactory() {
        final SchemaContextFactory schemaFactory = mockClass(SchemaContextFactory.class);
        doReturn(Futures.immediateCheckedFuture(getSchema())).when(schemaFactory).createSchemaContext(any(Collection.class));
        return schemaFactory;
    }

    public static SchemaContext getSchema() {
        final YangParserImpl parser = new YangParserImpl();
        final List<InputStream> modelsToParse = Lists.newArrayList(
                NetconfDeviceTest.class.getResourceAsStream("/schemas/test-module.yang")
        );
        final Set<Module> models = parser.parseYangModelsFromStreams(modelsToParse);
        return parser.resolveSchemaContext(models);
    }

    private RemoteDeviceHandler<NetconfSessionPreferences> getFacade() throws Exception {
        final RemoteDeviceHandler<NetconfSessionPreferences> remoteDeviceHandler = mockCloseableClass(RemoteDeviceHandler.class);
        doNothing().when(remoteDeviceHandler).onDeviceConnected(any(SchemaContext.class), any(NetconfSessionPreferences.class), any(NetconfDeviceRpc.class));
        doNothing().when(remoteDeviceHandler).onDeviceDisconnected();
        doNothing().when(remoteDeviceHandler).onNotification(any(DOMNotification.class));
        return remoteDeviceHandler;
    }

    private <T extends AutoCloseable> T mockCloseableClass(final Class<T> remoteDeviceHandlerClass) throws Exception {
        final T mock = mockClass(remoteDeviceHandlerClass);
        doNothing().when(mock).close();
        return mock;
    }

    private static <T> T mockClass(final Class<T> remoteDeviceHandlerClass) {
        final T mock = mock(remoteDeviceHandlerClass);
        Mockito.doReturn(remoteDeviceHandlerClass.getSimpleName()).when(mock).toString();
        return mock;
    }

    public RemoteDeviceId getId() {
        return new RemoteDeviceId("test-D");
    }

    public ExecutorService getExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    public MessageTransformer<NetconfMessage> getMessageTransformer() throws Exception {
        final MessageTransformer<NetconfMessage> messageTransformer = mockClass(MessageTransformer.class);
        doReturn(notification).when(messageTransformer).toRpcRequest(any(SchemaPath.class), any(NormalizedNode.class));
        doReturn(rpcResultC).when(messageTransformer).toRpcResult(any(NetconfMessage.class), any(SchemaPath.class));
        doReturn(compositeNode).when(messageTransformer).toNotification(any(NetconfMessage.class));
        return messageTransformer;
    }

    public NetconfSessionPreferences getSessionCaps(final boolean addMonitor, final Collection<String> additionalCapabilities) {
        final ArrayList<String> capabilities = Lists.newArrayList(
                XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_0,
                XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_1);

        if(addMonitor) {
            capabilities.add(NetconfMessageTransformUtil.IETF_NETCONF_MONITORING.getNamespace().toString());
        }

        capabilities.addAll(additionalCapabilities);

        return NetconfSessionPreferences.fromStrings(
                capabilities);
    }

    public NetconfDeviceCommunicator getListener() throws Exception {
        final NetconfDeviceCommunicator remoteDeviceCommunicator = mockCloseableClass(NetconfDeviceCommunicator.class);
//        doReturn(Futures.immediateFuture(rpcResult)).when(remoteDeviceCommunicator).sendRequest(any(NetconfMessage.class), any(QName.class));
        return remoteDeviceCommunicator;
    }
}
