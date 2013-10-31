/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

import javax.management.openmbean.SimpleType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;

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

    @Test
    public void createRuntimeBean() {
        ChoiceCaseNode caseNode = Mockito.mock(ChoiceCaseNode.class);
        doReturn(new HashSet<LeafSchemaNode>()).when(caseNode).getChildNodes();
        doReturn(new ArrayList<UnknownSchemaNode>()).when(caseNode)
                .getUnknownSchemaNodes();
        Map<String, RuntimeBeanEntry> runtimeBeans = RuntimeBeanEntry
                .extractClassNameToRuntimeBeanMap(PACKAGE_NAME, caseNode, "test-name", new TypeProviderWrapper(new
                        TypeProviderImpl(context)), "test", jmxImplModule);
        assertThat(runtimeBeans.size(), is(1));
        RuntimeBeanEntry runtimeMXBean = runtimeBeans.get("testRuntimeMXBean");
        assertThat(runtimeMXBean.isRoot(), is(true));
        assertThat(runtimeMXBean.getYangName(), is("test-name"));
    }

    @Test
    public void runtimeBeanRPCTest() {
        // create service interfaces
        Map<QName, ServiceInterfaceEntry> modulesToSIEs = ServiceInterfaceEntry
                .create(threadsModule, "packages.sis");
        assertNotNull(modulesToSIEs);

        // create MXBeans map
        Map<String, ModuleMXBeanEntry> namesToMBEs = ModuleMXBeanEntry.create(
                threadsJavaModule, modulesToSIEs, context,
                new TypeProviderWrapper(new TypeProviderImpl(context)),
                PACKAGE_NAME);
        assertThat(namesToMBEs.isEmpty(), is(false));

        // get threadfactory-naming bean
        ModuleMXBeanEntry threadfactoryNamingMXBean = namesToMBEs
                .get(THREADFACTORY_NAMING_MXB_NAME);
        assertNotNull(threadfactoryNamingMXBean);

        // get runtime beans
        Collection<RuntimeBeanEntry> runtimeBeanEntries = threadfactoryNamingMXBean
                .getRuntimeBeans();
        assertThat(runtimeBeanEntries.isEmpty(), is(false));

        // get root runtime bean
        RuntimeBeanEntry threadfactoryRuntimeBeanEntry = getRuntimeBeanEntryByJavaName(
                runtimeBeanEntries, "NamingThreadFactoryRuntimeMXBean");
        assertNotNull(threadfactoryRuntimeBeanEntry);
        assertThat(threadfactoryRuntimeBeanEntry.isRoot(), is(true));

        // get thread runtime bean
        RuntimeBeanEntry runtimeBeanEntry = getRuntimeBeanEntryByJavaName(
                runtimeBeanEntries, THREAD_RUNTIME_BEAN_JAVA_NAME);
        assertNotNull(runtimeBeanEntry);

        // test thread runtime bean properties
        assertThat(runtimeBeanEntry.getJavaNamePrefix(),
                is(THREAD_RUNTIME_BEAN_JAVA_PREFIX));
        assertThat(runtimeBeanEntry.getPackageName(), is(PACKAGE_NAME));
        assertThat(runtimeBeanEntry.getFullyQualifiedName(runtimeBeanEntry
                .getJavaNameOfRuntimeMXBean()), is(PACKAGE_NAME + "."
                + THREAD_RUNTIME_BEAN_JAVA_NAME));
        assertThat(runtimeBeanEntry.getYangName(),
                is(THREAD_RUNTIME_BEAN_YANG_NAME));

        // get thread runtime bean rpcs
        List<RuntimeBeanEntry.Rpc> rpcs = new ArrayList<RuntimeBeanEntry.Rpc>(
                runtimeBeanEntry.getRpcs());
        assertThat(rpcs.size(), is(2));

        // get sleep rpc and test it
        RuntimeBeanEntry.Rpc rpc = getRpcByName(rpcs, SLEEP_RPC_NAME);
        assertNotNull(rpc);
        assertThat(rpc.getYangName(), is(SLEEP_RPC_NAME));

        assertThat(((JavaAttribute)rpc.getReturnType()).getType().getFullyQualifiedName().endsWith(SLEEP_RPC_OUTPUT),  is(true));

        // get sleep rpc input attribute and test it
        List<JavaAttribute> attributes = rpc.getParameters();
        assertThat(attributes.size(), is(1));
        JavaAttribute attribute = attributes.get(0);
        assertThat(attribute.getAttributeYangName(), is(SLEEP_RPC_INPUT_NAME));
        assertThat(attribute.getType().getName(), is(SLEEP_RPC_INPUT_TYPE));
        assertThat(attribute.getLowerCaseCammelCase(), is(SLEEP_RPC_INPUT_NAME));
        assertThat(attribute.getUpperCaseCammelCase(), is("Millis"));
        assertNull(attribute.getNullableDefault());
        assertNull(attribute.getNullableDescription());
        assertThat(attribute.getOpenType(), is(SimpleType.class));
    }

    private RuntimeBeanEntry getRuntimeBeanEntryByJavaName(
            final Collection<RuntimeBeanEntry> runtimeBeanEntries,
            String javaName) {
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
            final List<RuntimeBeanEntry.Rpc> rpcs, String name) {
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
