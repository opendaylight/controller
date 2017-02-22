/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.YangModelSearchUtils;
import org.opendaylight.mdsal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public abstract class AbstractYangTest {
    protected SchemaContext context;
    protected Map<String, Module> namesToModules; // are module names globally
                                                  // unique?
    protected Module configModule, rpcContextModule, threadsModule,
            threadsJavaModule, bgpListenerJavaModule, ietfInetTypesModule,
            jmxModule, jmxImplModule, testFilesModule, testFiles1Module;

    public static final String EVENTBUS_MXB_NAME = "eventbus";
    public static final String ASYNC_EVENTBUS_MXB_NAME = "async-eventbus";
    public static final String THREADFACTORY_NAMING_MXB_NAME = "threadfactory-naming";
    public static final String THREADPOOL_DYNAMIC_MXB_NAME = "threadpool-dynamic";
    public static final String THREADPOOL_REGISTRY_IMPL_NAME = "threadpool-registry-impl";

    public static final String BGP_LISTENER_IMPL_MXB_NAME = "bgp-listener-impl";

    @Before
    public void loadYangFiles() throws Exception {
        final List<InputStream> yangISs = new ArrayList<>();
        yangISs.addAll(getStreams("/test-config-threads.yang",
                "/test-config-threads-java.yang",
                "/config-bgp-listener-impl.yang", "/ietf-inet-types.yang",
                "/config-jmx-it.yang", "/config-jmx-it-impl.yang",
                "/test-config-files.yang", "/test-config-files1.yang"));

        yangISs.addAll(getConfigApiYangInputStreams());

        this.context = YangParserTestUtils.parseYangStreams(yangISs);
        // close ISs
        for (final InputStream is : yangISs) {
            is.close();
        }
        this.namesToModules = YangModelSearchUtils.mapModulesByNames(this.context
                .getModules());
        this.configModule = this.namesToModules.get(ConfigConstants.CONFIG_MODULE);
        this.rpcContextModule = this.namesToModules.get(ConfigConstants.CONFIG_MODULE);
        this.threadsModule = this.namesToModules
                .get(ConfigConstants.CONFIG_THREADS_MODULE);
        this.threadsJavaModule = this.namesToModules.get("config-threads-java");
        this.bgpListenerJavaModule = this.namesToModules.get("config-bgp-listener-impl");
        this.ietfInetTypesModule = this.namesToModules
                .get(ConfigConstants.IETF_INET_TYPES);
        this.jmxModule = this.namesToModules.get("config-jmx-it");
        this.jmxImplModule = this.namesToModules.get("config-jmx-it-impl");
        this.testFilesModule = this.namesToModules.get("test-config-files");
        this.testFiles1Module = this.namesToModules.get("test-config-files1");

    }

    public static List<InputStream> getConfigApiYangInputStreams() {
        return getStreams("/META-INF/yang/config.yang", "/META-INF/yang/rpc-context.yang");
    }

    public Map<QName, IdentitySchemaNode> mapIdentitiesByQNames(final Module module) {
        final Map<QName, IdentitySchemaNode> result = new HashMap<>();
        for (final IdentitySchemaNode identitySchemaNode : module.getIdentities()) {
            final QName qName = identitySchemaNode.getQName();
            Preconditions.checkArgument(
                    result.containsKey(qName) == false,
                    "Two identities of %s contain same qname %s",
                            module, qName);
            result.put(qName, identitySchemaNode);
        }
        return result;
    }

    protected static List<InputStream> getStreams(final String... paths) {
        final List<InputStream> result = new ArrayList<>();
        for (final String path : paths) {
            final InputStream is = AbstractYangTest.class.getResourceAsStream(path);
            assertNotNull(path + " is null", is);
            result.add(is);
        }
        return result;
    }

    protected Map<QName, ServiceInterfaceEntry>  loadThreadsServiceInterfaceEntries(final String packageName) {
        final Map<IdentitySchemaNode, ServiceInterfaceEntry> identitiesToSIs = new HashMap<>();
        return ServiceInterfaceEntry.create(this.threadsModule, packageName,identitiesToSIs);
    }

    protected Map<String /* identity local name */, ModuleMXBeanEntry> loadThreadsJava(
            final Map<QName, ServiceInterfaceEntry> modulesToSIEs, final String packageName) {
        final Map<String /* identity local name */, ModuleMXBeanEntry> namesToMBEs = ModuleMXBeanEntry
                .create(this.threadsJavaModule, modulesToSIEs, this.context, new TypeProviderWrapper(new TypeProviderImpl
                (this.context)), packageName);
        Assert.assertNotNull(namesToMBEs);
        final Set<String> expectedMXBeanNames = Sets.newHashSet(EVENTBUS_MXB_NAME,
                ASYNC_EVENTBUS_MXB_NAME, THREADFACTORY_NAMING_MXB_NAME,
                THREADPOOL_DYNAMIC_MXB_NAME, THREADPOOL_REGISTRY_IMPL_NAME);
        assertThat(namesToMBEs.keySet(), is(expectedMXBeanNames));
        return namesToMBEs;
    }

}
