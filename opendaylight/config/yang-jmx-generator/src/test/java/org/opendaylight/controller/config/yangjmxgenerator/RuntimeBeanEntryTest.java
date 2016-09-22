/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.management.openmbean.SimpleType;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

public class RuntimeBeanEntryTest extends AbstractYangTest {

    public static final String PACKAGE_NAME = "packages.sis";
    public static final String THREADFACTORY_NAMING_MXB_NAME = "threadfactory-naming";
    public static final String THREAD_RUNTIME_BEAN_JAVA_NAME = "ThreadRuntimeMXBean";
    public static final String THREAD_RUNTIME_BEAN_JAVA_PREFIX = "Thread";
    public static final String THREAD_RUNTIME_BEAN_YANG_NAME = "thread";
    public static final String SLEEP_RPC_NAME = "sleep";
    public static final String SLEEP_RPC_OUTPUT = "ThreadState";
    public static final String SLEEP_RPC_INPUT_NAME = "millis";
    public static final String SLEEP_RPC_INPUT_TYPE = "Long";
    private static final Map<IdentitySchemaNode, ServiceInterfaceEntry> identitiesToSIs = new HashMap<>();

    @Test
    public void createRuntimeBean() {
        ChoiceCaseNode caseNode = Mockito.mock(ChoiceCaseNode.class);
        doReturn(new HashSet<LeafSchemaNode>()).when(caseNode).getChildNodes();
        doReturn(new ArrayList<UnknownSchemaNode>()).when(caseNode)
                .getUnknownSchemaNodes();
        Map<String, RuntimeBeanEntry> runtimeBeans = RuntimeBeanEntry
                .extractClassNameToRuntimeBeanMap(PACKAGE_NAME, caseNode, "test-name", new TypeProviderWrapper(new
                        TypeProviderImpl(context)), "test", jmxImplModule, context);
        assertEquals(1, runtimeBeans.size());
        RuntimeBeanEntry runtimeMXBean = runtimeBeans.get("testRuntimeMXBean");
        assertTrue(runtimeMXBean.isRoot());
        assertEquals("test-name", runtimeMXBean.getYangName());
    }

    @Test
    public void runtimeBeanRPCTest() {
        // create service interfaces
        Map<QName, ServiceInterfaceEntry> modulesToSIEs = ServiceInterfaceEntry
                .create(threadsModule, "packages.sis",identitiesToSIs);
        assertNotNull(modulesToSIEs);

        // create MXBeans map
        Map<String, ModuleMXBeanEntry> namesToMBEs = ModuleMXBeanEntry.create(
                threadsJavaModule, modulesToSIEs, context,
                new TypeProviderWrapper(new TypeProviderImpl(context)),
                PACKAGE_NAME);
        assertFalse(namesToMBEs.isEmpty());

        // get threadfactory-naming bean
        ModuleMXBeanEntry threadfactoryNamingMXBean = namesToMBEs
                .get(THREADFACTORY_NAMING_MXB_NAME);
        assertNotNull(threadfactoryNamingMXBean);

        // get runtime beans
        Collection<RuntimeBeanEntry> runtimeBeanEntries = threadfactoryNamingMXBean
                .getRuntimeBeans();
        assertFalse(runtimeBeanEntries.isEmpty());

        // get root runtime bean
        RuntimeBeanEntry threadfactoryRuntimeBeanEntry = getRuntimeBeanEntryByJavaName(
                runtimeBeanEntries, "NamingThreadFactoryRuntimeMXBean");
        assertNotNull(threadfactoryRuntimeBeanEntry);
        assertTrue(threadfactoryRuntimeBeanEntry.isRoot());

        // get thread runtime bean
        RuntimeBeanEntry runtimeBeanEntry = getRuntimeBeanEntryByJavaName(
                runtimeBeanEntries, THREAD_RUNTIME_BEAN_JAVA_NAME);
        assertNotNull(runtimeBeanEntry);

        // test thread runtime bean properties
        assertEquals(THREAD_RUNTIME_BEAN_JAVA_PREFIX, runtimeBeanEntry.getJavaNamePrefix());
        assertEquals(PACKAGE_NAME, runtimeBeanEntry.getPackageName());
        assertEquals(PACKAGE_NAME + "." + THREAD_RUNTIME_BEAN_JAVA_NAME,
            runtimeBeanEntry.getFullyQualifiedName(runtimeBeanEntry
                .getJavaNameOfRuntimeMXBean()));
        assertEquals(THREAD_RUNTIME_BEAN_YANG_NAME, runtimeBeanEntry.getYangName());

        // get thread runtime bean rpcs
        List<RuntimeBeanEntry.Rpc> rpcs = new ArrayList<>(
                runtimeBeanEntry.getRpcs());
        assertEquals(2, rpcs.size());

        // get sleep rpc and test it
        RuntimeBeanEntry.Rpc rpc = getRpcByName(rpcs, SLEEP_RPC_NAME);
        assertNotNull(rpc);
        assertEquals(SLEEP_RPC_NAME, rpc.getYangName());

        assertTrue(((JavaAttribute)rpc.getReturnType()).getType().getFullyQualifiedName().endsWith(SLEEP_RPC_OUTPUT));

        // get sleep rpc input attribute and test it
        List<JavaAttribute> attributes = rpc.getParameters();
        assertEquals(1, attributes.size());
        JavaAttribute attribute = attributes.get(0);
        assertEquals(SLEEP_RPC_INPUT_NAME, attribute.getAttributeYangName());
        assertEquals(SLEEP_RPC_INPUT_TYPE, attribute.getType().getName());
        assertEquals(SLEEP_RPC_INPUT_NAME, attribute.getLowerCaseCammelCase());
        assertEquals("Millis", attribute.getUpperCaseCammelCase());
        assertNull(attribute.getNullableDefault());
        assertNull(attribute.getNullableDescription());
        assertTrue(attribute.getOpenType() instanceof SimpleType);
    }

    private RuntimeBeanEntry getRuntimeBeanEntryByJavaName(
            final Collection<RuntimeBeanEntry> runtimeBeanEntries,
            final String javaName) {
        if (runtimeBeanEntries != null && !runtimeBeanEntries.isEmpty()) {
            for (RuntimeBeanEntry runtimeBeanEntry : runtimeBeanEntries) {
                if (runtimeBeanEntry.getJavaNameOfRuntimeMXBean().equals(
                        javaName)) {
                    return runtimeBeanEntry;
                }
            }
        }
        return null;
    }

    private RuntimeBeanEntry.Rpc getRpcByName(
            final List<RuntimeBeanEntry.Rpc> rpcs, final String name) {
        if (rpcs != null && !rpcs.isEmpty()) {
            for (RuntimeBeanEntry.Rpc rpc : rpcs) {
                if (rpc.getName().equals(name)) {
                    return rpc;
                }
            }
        }
        return null;
    }

}
