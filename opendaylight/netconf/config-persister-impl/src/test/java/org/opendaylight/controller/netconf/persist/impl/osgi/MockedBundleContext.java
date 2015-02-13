/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.persist.impl.osgi;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.api.PropertiesProvider;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.persist.impl.DummyAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

final class MockedBundleContext {
    @Mock
    private BundleContext context;
    @Mock
    private Filter outerFilter, innerFilter;
    @Mock
    private ServiceReference<?> serviceReference;
    @Mock
    private Bundle bundle;
    @Mock
    NetconfOperationServiceFactory serviceFactory;
    @Mock
    private NetconfOperationService service;
    @Mock
    private ServiceRegistration<?> registration;

    MockedBundleContext(long maxWaitForCapabilitiesMillis, long conflictingVersionTimeoutMillis) throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(null).when(context).getProperty(anyString());
        initContext(maxWaitForCapabilitiesMillis, conflictingVersionTimeoutMillis);

        String outerFilterString = "(objectClass=org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory)";
        doReturn(outerFilter).when(context).createFilter(outerFilterString);
        doNothing().when(context).addServiceListener(any(ServiceListener.class), eq(outerFilterString));
        ServiceReference<?>[] toBeReturned = {serviceReference};
        doReturn(toBeReturned).when(context).getServiceReferences(NetconfOperationServiceFactory.class.getName(), null);

        String innerFilterString = "innerfilter";
        doReturn(innerFilterString).when(outerFilter).toString();

        doReturn(innerFilter).when(context).createFilter(ConfigPersisterActivator.getFilterString());
        doReturn(innerFilterString).when(innerFilter).toString();
        doNothing().when(context).addServiceListener(any(ServiceListener.class), eq(innerFilterString));

        doReturn(toBeReturned).when(context).getServiceReferences((String) null, innerFilterString);
        doReturn(bundle).when(serviceReference).getBundle();
        doReturn(context).when(bundle).getBundleContext();
        doReturn("").when(serviceReference).toString();
        doReturn("context").when(context).toString();
        doReturn(serviceFactory).when(context).getService(any(ServiceReference.class));
        doReturn(service).when(serviceFactory).createService(anyString());
        final Capability cap = mock(Capability.class);
        doReturn("cap1").when(cap).getCapabilityUri();
        doReturn(Collections.singleton(cap)).when(serviceFactory).getCapabilities();
        doNothing().when(service).close();
        doReturn("serviceFactoryMock").when(serviceFactory).toString();

        doNothing().when(registration).unregister();
        doReturn(registration).when(context).registerService(
                eq(ConfigPusher.class.getName()), any(Closeable.class),
                any(Dictionary.class));
    }

    public BundleContext getBundleContext() {
        return context;
    }

    private void initContext(long maxWaitForCapabilitiesMillis, long conflictingVersionTimeoutMillis) {
        initProp(context, "active", "1");
        initProp(context, "1." + ConfigPersisterActivator.STORAGE_ADAPTER_CLASS_PROP_SUFFIX, DummyAdapterWithInitialSnapshot.class.getName());
        initProp(context, "1." + "readonly", "false");
        initProp(context, "1." + ".properties.fileStorage", "target/configuration-persister-test/initial/");
        initProp(context, ConfigPersisterActivator.MAX_WAIT_FOR_CAPABILITIES_MILLIS_PROPERTY, String.valueOf(maxWaitForCapabilitiesMillis));
        initProp(context, ConfigPersisterActivator.CONFLICTING_VERSION_TIMEOUT_MILLIS_PROPERTY, String.valueOf(conflictingVersionTimeoutMillis));
    }

    private void initProp(BundleContext context, String key, String value) {
        initPropNoPrefix(context, ConfigPersisterActivator.NETCONF_CONFIG_PERSISTER + "." + key, value);
    }

    private void initPropNoPrefix(BundleContext context, String key, String value) {
        doReturn(value).when(context).getProperty(key);
    }

    public static class DummyAdapterWithInitialSnapshot extends DummyAdapter {

        public static final String CONFIG_SNAPSHOT = "config-snapshot";
        public static String expectedCapability = "cap2";

        @Override
        public List<ConfigSnapshotHolder> loadLastConfigs() throws IOException {
            return Lists.newArrayList(getConfigSnapshot());
        }

        @Override
        public Persister instantiate(PropertiesProvider propertiesProvider) {
            return this;
        }

        public ConfigSnapshotHolder getConfigSnapshot() {
            return new ConfigSnapshotHolder() {
                @Override
                public String getConfigSnapshot() {
                    return "<data><" + CONFIG_SNAPSHOT + "/></data>";
                }

                @Override
                public SortedSet<String> getCapabilities() {
                    TreeSet<String> strings = Sets.newTreeSet();
                    strings.add(expectedCapability);
                    return strings;
                }

                @Override
                public String toString() {
                    return getConfigSnapshot();
                }
            };
        }
    }
}
