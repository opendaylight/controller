/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import java.net.URI;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionCapabilities;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NetconfDeviceSalFacadeTest {

    private RemoteDeviceId deviceId;

    @Mock
    private Broker domBroker;
    @Mock
    private BindingAwareBroker bindingBroker;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private ExecutorService executor;
    @Mock
    private BindingAwareBroker.ProviderContext providerContext;
    @Mock
    private Broker.ProviderSession providerSession;
    @Mock
    private SchemaContext schemaContext;
    @Mock
    private RpcImplementation deviceRpc;
    @Mock
    private CompositeNode compositeNode;
    @Mock
    private DOMMountPointService pointService;
    @Mock
    private DOMMountPointService.DOMMountPointBuilder pointBuilder;
    @Mock
    private ObjectRegistration objectRegistration;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private WriteTransaction writeTransaction;
    @Mock
    private CheckedFuture checkedFuture;
    @Mock
    private ReadWriteTransaction readWriteTransaction;
    @Mock
    private RpcDefinition rpcDef;
    @Mock
    private Module module;

    private RemoteDeviceId remoteDeviceId;
    private NetconfDeviceSalProvider deviceSalProvider;
    private NetconfDeviceSalFacade facade;
    private NetconfSessionCapabilities sessionCapabilities;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        deviceId = new RemoteDeviceId("remoteDevice");
        doReturn(providerContext).when(bindingBroker).registerProvider(any(BindingAwareProvider.class), any(BundleContext.class));
        doReturn("bundleCOntext").when(bundleContext).toString();
        doReturn(providerSession).when(domBroker).registerProvider(any(Provider.class), any(BundleContext.class));
        QName qname = new QName(URI.create("uri"), "lname");
        doReturn(qname).when(rpcDef).getQName();
        Set oper = Sets.newHashSet(rpcDef);
        doReturn(oper).when(schemaContext).getOperations();
        doReturn(qname).when(schemaContext).getQName();
        doReturn(module).when(schemaContext).findModuleByNamespaceAndRevision(any(URI.class), any(Date.class));
        doReturn("prov").when(providerSession).toString();
        doReturn("composite").when(compositeNode).toString();
        doReturn(pointService).when(providerSession).getService(any(Class.class));
        remoteDeviceId = new RemoteDeviceId("remoteDevId");
        deviceSalProvider  = new NetconfDeviceSalProvider(remoteDeviceId, executor);
        sessionCapabilities = NetconfSessionCapabilities.fromStrings(Sets.newHashSet("cap1"));
        doReturn(pointBuilder).when(pointService).createMountPoint(any(YangInstanceIdentifier.class));
        doReturn("schemaContext").when(schemaContext).toString();
        doReturn(pointBuilder).when(pointBuilder).addInitialSchemaContext(any(SchemaContext.class));
        doReturn(pointBuilder).when(pointBuilder).addService(any(Class.class), any(DOMService.class));
        doReturn(objectRegistration).when(pointBuilder).register();
        doReturn("provCtx").when(providerContext).toString();
        doReturn(dataBroker).when(providerContext).getSALService(any(Class.class));
        doReturn(writeTransaction).when(dataBroker).newWriteOnlyTransaction();
        doReturn(new Object()).when(writeTransaction).getIdentifier();
        doNothing().when(writeTransaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(DataObject.class));
        doReturn(checkedFuture).when(writeTransaction).submit();
        doNothing().when(checkedFuture).addListener(any(Runnable.class), any(Executor.class));
        doReturn(readWriteTransaction).when(dataBroker).newReadWriteTransaction();
        doReturn(new Object()).when(readWriteTransaction).getIdentifier();
        doNothing().when(readWriteTransaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(DataObject.class));
        doReturn(checkedFuture).when(readWriteTransaction).submit();
        doReturn(qname).when(compositeNode).getNodeType();
        doNothing().when(writeTransaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test
    public void testRegisterToSal() throws Exception {
        facade = new NetconfDeviceSalFacade(deviceId, domBroker, bindingBroker, bundleContext, executor);
        verify(bindingBroker, times(1)).registerProvider(any(BindingAwareProvider.class), any(BundleContext.class));
        verify(domBroker, times(1)).registerProvider(any(Provider.class), any(BundleContext.class));
    }

    @Test
    public void testDeviceConnected() throws Exception {
        deviceSalProvider.onSessionInitiated(providerSession);
        deviceSalProvider.onSessionInitiated(providerContext);
        facade = new NetconfDeviceSalFacade(deviceId, domBroker, bindingBroker, bundleContext, deviceSalProvider);
        facade.onDeviceConnected(schemaContext, sessionCapabilities, deviceRpc);
        facade.onNotification(compositeNode);
        assertNotNull(deviceSalProvider.getMountInstance());
    }

    @Test
    public void testClose() throws Exception {
        deviceSalProvider.onSessionInitiated(providerSession);
        deviceSalProvider.onSessionInitiated(providerContext);
        facade = new NetconfDeviceSalFacade(deviceId, domBroker, bindingBroker, bundleContext, deviceSalProvider);
        facade.onDeviceDisconnected();
        facade.close();
        assertNotNull(deviceSalProvider.getMountInstance());
        verify(writeTransaction, times(2)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }
}
