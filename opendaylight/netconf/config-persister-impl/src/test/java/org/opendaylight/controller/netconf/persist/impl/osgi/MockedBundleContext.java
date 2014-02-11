/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.persist.impl.osgi;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.api.PropertiesProvider;
import org.opendaylight.controller.netconf.persist.impl.DummyAdapter;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.mockito.Mockito.doReturn;

final class MockedBundleContext {

    @Mock
    private BundleContext context;

    MockedBundleContext(String netconfAddress, String netconfPort) {
        MockitoAnnotations.initMocks(this);
        initContext(netconfAddress, netconfPort);
    }

    public BundleContext getBundleContext() {
        return context;
    }

    private void initContext(String netconfAddress, String netconfPort) {
        initProp(context, ConfigPersisterActivator.IGNORED_MISSING_CAPABILITY_REGEX_SUFFIX, null);

        initPropNoPrefix(context, "netconf.tcp.client.address", netconfAddress);
        initPropNoPrefix(context, "netconf.tcp.client.port", netconfPort);

        initProp(context, "active", "1");
        initProp(context, "1." + ConfigPersisterActivator.STORAGE_ADAPTER_CLASS_PROP_SUFFIX, DummyAdapterWithInitialSnapshot.class.getName());
        initProp(context, "1." + "readonly", "false");
        initProp(context, "1." + ".properties.fileStorage", "target/configuration-persister-test/initial/");

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
            return Lists.newArrayList(getConfigSnapshopt());
        }

        @Override
        public Persister instantiate(PropertiesProvider propertiesProvider) {
            return this;
        }

        public ConfigSnapshotHolder getConfigSnapshopt() {
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
