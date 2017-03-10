/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Sets;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.SimpleType;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListDependenciesAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TypedAttribute;
import org.opendaylight.mdsal.binding.model.api.Type;
import org.opendaylight.mdsal.binding.model.util.Types;
import org.opendaylight.mdsal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.RevisionAwareXPath;

public class ModuleMXBeanEntryTest extends AbstractYangTest {

    public static final String PACKAGE_NAME = "pack2";

    protected static final URI THREADS_NAMESPACE;
    protected static final Date THREADS_REVISION_DATE;

    static {
        try {
            THREADS_NAMESPACE = new URI(ConfigConstants.CONFIG_NAMESPACE
                    + ":threads");
        } catch (final URISyntaxException e) {
            throw new Error(e);
        }
        final SimpleDateFormat revisionFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            THREADS_REVISION_DATE = revisionFormat.parse("2013-04-09");
        } catch (final ParseException e) {
            throw new Error(e);
        }
    }

    protected Map<QName, ServiceInterfaceEntry> modulesToSIEs;


    @Before
    public void setUp() {
        this.modulesToSIEs = loadThreadsServiceInterfaceEntries("packages.sis");
    }


    protected Map<String /* identity local name */, ModuleMXBeanEntry> loadThreadsJava() {
        return loadThreadsJava(this.modulesToSIEs, PACKAGE_NAME);
    }

    @Test
    public void test_jmxImplModule() {
        final Map<IdentitySchemaNode, ServiceInterfaceEntry> identitiesToSIs = new HashMap<>();
        final Map<QName, ServiceInterfaceEntry> modulesToSIEs = ServiceInterfaceEntry
                .create(this.threadsModule, PACKAGE_NAME,identitiesToSIs);
        modulesToSIEs.putAll(ServiceInterfaceEntry.create(this.jmxModule,
                PACKAGE_NAME,identitiesToSIs));
        final Map<String /* identity local name */, ModuleMXBeanEntry> namesToMBEs = ModuleMXBeanEntry
                .create(this.jmxImplModule, modulesToSIEs, this.context, new TypeProviderWrapper(new TypeProviderImpl(this.context))
                , PACKAGE_NAME);
        final Map<String, AttributeIfc> attributes = namesToMBEs.get("impl-netconf")
                .getAttributes();

        assertCorrectAttributesSize(namesToMBEs, attributes);

        //
        final DependencyAttribute threadFactoryAttribute = (DependencyAttribute) attributes
                .get("thread-factory");
        assertNotNull(threadFactoryAttribute);
        assertFalse(threadFactoryAttribute.getDependency().isMandatory());
        assertThat(threadFactoryAttribute.getDependency().getSie()
                .getTypeName(), is("ThreadFactoryServiceInterface"));
        assertThat(threadFactoryAttribute.getAttributeYangName(),
                is("thread-factory"));
        assertThat(threadFactoryAttribute.getLowerCaseCammelCase(),
                is("threadFactory"));
        assertThat(threadFactoryAttribute.getUpperCaseCammelCase(),
                is("ThreadFactory"));
        assertThat(threadFactoryAttribute.getOpenType(), isA(SimpleType.class));
        assertNull(threadFactoryAttribute.getNullableDefault());
        assertNull(threadFactoryAttribute.getNullableDescription());
        assertThat(threadFactoryAttribute.getType().getName(), is("ObjectName"));
    }

    private void assertCorrectAttributesSize(final Map<String, ModuleMXBeanEntry> namesToMBEs, final Map<String, AttributeIfc> attributes) {
        assertEquals(14, attributes.size());
        assertEquals(1, namesToMBEs.get("impl-netconf").getRuntimeBeans().size());
        assertEquals(2, namesToMBEs.get("impl-netconf").getRuntimeBeans().iterator().next().getAttributes().size());

        assertEquals(4, namesToMBEs.get("impl").getAttributes().size());
        assertEquals(1, namesToMBEs.get("impl").getRuntimeBeans().size());
        assertEquals(1, namesToMBEs.get("impl").getRuntimeBeans().iterator().next().getAttributes().size());
    }

    protected RuntimeBeanEntry findFirstByYangName(
            final Collection<RuntimeBeanEntry> runtimeBeans, final String yangName) {
        for (final RuntimeBeanEntry rb : runtimeBeans) {
            if (yangName.equals(rb.getYangName())) {
                return rb;
            }
        }
        throw new IllegalArgumentException("Yang name not found:" + yangName
                + " in " + runtimeBeans);
    }

    protected RuntimeBeanEntry findFirstByNamePrefix(final Collection<RuntimeBeanEntry> runtimeBeans, final String namePrefix) {
        for (final RuntimeBeanEntry rb : runtimeBeans) {
            if (namePrefix.equals(rb.getJavaNamePrefix())) {
                return rb;
            }
        }

        throw new IllegalArgumentException("Name prefix not found:" + namePrefix
            + " in " + runtimeBeans);
    }

    @Test
    public void testGetWhenConditionMatcher() {
        assertMatches("config",
                "/config:modules/config:module/config:type = 'threadpool-dynamic'");
        assertMatches("ns",
                "/ns:modules/ns:module/ns:type = 'threadpool-dynamic'");
        assertMatches("config",
                "/config:modules/config:module/config:type=\"threadpool-dynamic\"");
    }

    private void assertMatches(final String prefix, final String input) {
        final RevisionAwareXPath whenConstraint = mock(RevisionAwareXPath.class);
        doReturn(input).when(whenConstraint).toString();
        final Matcher output = ModuleMXBeanEntryBuilder.getWhenConditionMatcher(prefix,
                whenConstraint);
        assertTrue(output.matches());
        assertEquals("threadpool-dynamic", output.group(1));
    }

    @Test
    public void testThreadsJava() {
        final Map<String /* identity local name */, ModuleMXBeanEntry> namesToMBEs = loadThreadsJava();

        { // check threadpool-dynamic
            final ModuleMXBeanEntry dynamicThreadPool = namesToMBEs
                    .get(THREADPOOL_DYNAMIC_MXB_NAME);
            final Map<String, AttributeIfc> attributes = dynamicThreadPool
                    .getAttributes();
            // core-size, keepalive, maximum-size
            // threadfactory
            final Set<String> longAttribs = Sets.newHashSet("core-size",
                    "maximum-size");
            for (final String longAttrib : longAttribs) {

                final TypedAttribute attribute = (TypedAttribute) attributes
                        .get(longAttrib);
                assertThat("Failed to check " + longAttrib,
                        attribute.getType(),
                        is((Type) Types.typeForClass(Long.class)));
            }
            // check dependency on thread factory
            final QName threadfactoryQName = QName.create(THREADS_NAMESPACE,
                    THREADS_REVISION_DATE, "threadfactory");
            final ServiceInterfaceEntry threadFactorySIEntry = this.modulesToSIEs
                    .get(threadfactoryQName);
            assertNotNull(threadFactorySIEntry);
            final boolean expectedMandatory = true;
            final TypedAttribute actualThreadFactory = (TypedAttribute) attributes
                    .get("threadfactory");

            final DataSchemaNode mockedDataSchemaNode = mock(DataSchemaNode.class);
            doReturn(Collections.emptyList()).when(mockedDataSchemaNode)
            .getUnknownSchemaNodes();
            doReturn(threadfactoryQName).when(mockedDataSchemaNode).getQName();
            final AttributeIfc expectedDependencyAttribute = new DependencyAttribute(
                    mockedDataSchemaNode, threadFactorySIEntry,
                    expectedMandatory, "threadfactory description");
            assertThat(actualThreadFactory, is(expectedDependencyAttribute));
            assertThat(
                    dynamicThreadPool
                    .getFullyQualifiedName("DynamicThreadPoolModuleMXBean"),
                    is(PACKAGE_NAME + ".DynamicThreadPoolModuleMXBean"));
            assertThat(dynamicThreadPool.getNullableDescription(),
                    is("threadpool-dynamic description"));
            assertThat(dynamicThreadPool.getYangModuleName(),
                    is("config-threads-java"));
            assertThat(dynamicThreadPool.getYangModuleLocalname(),
                    is(THREADPOOL_DYNAMIC_MXB_NAME));

            // check root runtime bean
            final Collection<RuntimeBeanEntry> runtimeBeans = dynamicThreadPool
                    .getRuntimeBeans();
            assertThat(runtimeBeans.size(), is(1));
            final RuntimeBeanEntry rootRB = findFirstByYangName(runtimeBeans,
                    THREADPOOL_DYNAMIC_MXB_NAME);
            assertThat(rootRB.isRoot(), is(true));
            assertThat(rootRB.getAttributes().size(), is(1));
            final JavaAttribute attribute = (JavaAttribute) rootRB.getAttributes()
                    .iterator().next();
            assertThat(attribute.getAttributeYangName(), is("created-sessions"));
            assertThat(rootRB.getYangName(), is(THREADPOOL_DYNAMIC_MXB_NAME));
            assertThat(attribute.getType().getFullyQualifiedName(),
                    is(Long.class.getName()));
        }
        {// check threadfactory-naming
            final ModuleMXBeanEntry threadFactoryNaming = namesToMBEs
                    .get(THREADFACTORY_NAMING_MXB_NAME);
            final Collection<RuntimeBeanEntry> runtimeBeans = threadFactoryNaming
                    .getRuntimeBeans();
            assertThat(runtimeBeans.size(), is(4));
            {
                final RuntimeBeanEntry threadRB = findFirstByYangName(runtimeBeans,
                        "thread");
                assertNotNull(threadRB);
                assertFalse(threadRB.isRoot());
                assertEquals("name", threadRB.getKeyYangName().get());
                assertEquals("Name", threadRB.getKeyJavaName().get());
                assertThat(threadRB.getAttributes().size(), is(1));
                final AttributeIfc threadNameAttr = threadRB.getAttributes()
                        .iterator().next();
                assertThat(threadNameAttr.getAttributeYangName(), is("name"));
                assertTrue(threadNameAttr instanceof JavaAttribute);
                assertThat(((JavaAttribute) threadNameAttr).getType()
                        .getFullyQualifiedName(), is(String.class.getName()));
                assertThat(threadRB.getRpcs().size(), is(2));
            }
            {
                final RuntimeBeanEntry streamRB = findFirstByNamePrefix(runtimeBeans,
                        "ThreadStream");
                assertNotNull(streamRB);
                assertFalse(streamRB.getKeyYangName().isPresent());
                assertFalse(streamRB.getKeyJavaName().isPresent());
                final Map<String, AttributeIfc> attributeMap = streamRB
                        .getYangPropertiesToTypesMap();
                assertEquals(4, attributeMap.size());

                final TOAttribute toAttr = (TOAttribute) attributeMap.get("peer");
                assertNotNull(toAttr);
                assertThat(toAttr.getAttributeYangName(), is("peer"));
                assertThat(toAttr.getLowerCaseCammelCase(), is("peer"));
                assertThat(toAttr.getUpperCaseCammelCase(), is("Peer"));
                assertThat(toAttr.getOpenType(), isA(CompositeType.class));
                Set<String> propsExpected = new HashSet<>(2);
                propsExpected.add("port");
                propsExpected.add("core-size");
                assertThat(toAttr.getYangPropertiesToTypesMap().keySet(),
                        is(propsExpected));
                propsExpected = new HashSet<>(2);
                propsExpected.add("Port");
                propsExpected.add("CoreSize");
                assertThat(
                        toAttr.getCapitalizedPropertiesToTypesMap().keySet(),
                        is(propsExpected));
                propsExpected = new HashSet<>(2);
                propsExpected.add("port");
                propsExpected.add("coreSize");
                assertThat(toAttr.getJmxPropertiesToTypesMap().keySet(),
                        is(propsExpected));

                final JavaAttribute timestampAttr = (JavaAttribute) attributeMap
                        .get("timestamp");
                assertNotNull(timestampAttr);

                final JavaAttribute stateAttr = (JavaAttribute) attributeMap
                        .get("state");
                assertNotNull(stateAttr);

                final ListAttribute innerStream = (ListAttribute) attributeMap
                        .get("inner-stream-list");
                assertNotNull(innerStream);
                assertThat(innerStream.getAttributeYangName(),
                        is("inner-stream-list"));
                assertThat(innerStream.getLowerCaseCammelCase(),
                        is("innerStreamList"));
                assertThat(innerStream.getUpperCaseCammelCase(),
                        is("InnerStreamList"));
                assertThat(innerStream.getOpenType(), isA(ArrayType.class));

            }

        }
        { // test multiple dependencies
            final ModuleMXBeanEntry threadPoolRegistry = namesToMBEs.get(THREADPOOL_REGISTRY_IMPL_NAME);
            final Map<String, AttributeIfc> attributes = threadPoolRegistry.getAttributes();
            assertEquals(1, attributes.size());
            final AttributeIfc threadpoolsAttr = attributes.get("threadpools");
            assertNotNull(threadpoolsAttr);
            assertTrue(threadpoolsAttr instanceof ListDependenciesAttribute);
            final ListDependenciesAttribute threadpools = (ListDependenciesAttribute) threadpoolsAttr;
        }
    }

}
