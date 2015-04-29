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
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

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
        List<InputStream> yangISs = new ArrayList<>();
        yangISs.addAll(getStreams("/test-config-threads.yang",
                "/test-config-threads-java.yang",
                "/config-bgp-listener-impl.yang", "/ietf-inet-types.yang",
                "/config-jmx-it.yang", "/config-jmx-it-impl.yang",
                "/test-config-files.yang", "/test-config-files1.yang"));

        yangISs.addAll(getConfigApiYangInputStreams());

        YangParserImpl parser = new YangParserImpl();
        Set<Module> modulesToBuild = parser.parseYangModelsFromStreams(yangISs);
        // close ISs
        for (InputStream is : yangISs) {
            is.close();
        }
        context = parser.resolveSchemaContext(modulesToBuild);
        namesToModules = YangModelSearchUtils.mapModulesByNames(context
                .getModules());
        configModule = namesToModules.get(ConfigConstants.CONFIG_MODULE);
        rpcContextModule = namesToModules.get(ConfigConstants.CONFIG_MODULE);
        threadsModule = namesToModules
                .get(ConfigConstants.CONFIG_THREADS_MODULE);
        threadsJavaModule = namesToModules.get("config-threads-java");
        bgpListenerJavaModule = namesToModules.get("config-bgp-listener-impl");
        ietfInetTypesModule = namesToModules
                .get(ConfigConstants.IETF_INET_TYPES);
        jmxModule = namesToModules.get("config-jmx-it");
        jmxImplModule = namesToModules.get("config-jmx-it-impl");
        testFilesModule = namesToModules.get("test-config-files");
        testFiles1Module = namesToModules.get("test-config-files1");

    }

    public static List<InputStream> getConfigApiYangInputStreams() {
        return getStreams("/META-INF/yang/config.yang",
                "/META-INF/yang/rpc-context.yang");
    }

    public Map<QName, IdentitySchemaNode> mapIdentitiesByQNames(Module module) {
        Map<QName, IdentitySchemaNode> result = new HashMap<>();
        for (IdentitySchemaNode identitySchemaNode : module.getIdentities()) {
            QName qName = identitySchemaNode.getQName();
            Preconditions.checkArgument(
                    result.containsKey(qName) == false,
                    "Two identities of %s contain same qname %s",
                            module, qName);
            result.put(qName, identitySchemaNode);
        }
        return result;
    }

    protected static List<InputStream> getStreams(String... paths) {
        List<InputStream> result = new ArrayList<>();
        for (String path : paths) {
            InputStream is = AbstractYangTest.class.getResourceAsStream(path);
            assertNotNull(path + " is null", is);
            result.add(is);
        }
        return result;
    }

    protected Map<QName, ServiceInterfaceEntry>  loadThreadsServiceInterfaceEntries(String packageName) {
        Map<IdentitySchemaNode, ServiceInterfaceEntry> identitiesToSIs = new HashMap<>();
        return ServiceInterfaceEntry.create(threadsModule, packageName,identitiesToSIs);
    }

    protected Map<String /* identity local name */, ModuleMXBeanEntry> loadThreadsJava(Map<QName, ServiceInterfaceEntry> modulesToSIEs, String packageName) {
        Map<String /* identity local name */, ModuleMXBeanEntry> namesToMBEs = ModuleMXBeanEntry
                .create(threadsJavaModule, modulesToSIEs, context, new TypeProviderWrapper(new TypeProviderImpl
                (context)), packageName);
        Assert.assertNotNull(namesToMBEs);
        Set<String> expectedMXBeanNames = Sets.newHashSet(EVENTBUS_MXB_NAME,
                ASYNC_EVENTBUS_MXB_NAME, THREADFACTORY_NAMING_MXB_NAME,
                THREADPOOL_DYNAMIC_MXB_NAME, THREADPOOL_REGISTRY_IMPL_NAME);
        assertThat(namesToMBEs.keySet(), is(expectedMXBeanNames));
        return namesToMBEs;
    }

}
