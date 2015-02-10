/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Collections;
import java.util.Map;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.ServiceReferenceReadableRegistry;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.InstanceConfigElementResolved;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleElementDefinition;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleElementResolved;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ServiceRegistryWrapper;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Services;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.ValidateTest;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfigXmlParser.EditConfigExecution;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreContext;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;

public class EditConfigTest {

    @Mock
    private YangStoreContext yangStoreSnapshot;
    @Mock
    private TransactionProvider provider;
    @Mock
    private ConfigRegistryClient configRegistry;
    @Mock
    private ConfigTransactionClient configTransactionClient;
    @Mock
    private ObjectName mockOn;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        doReturn("mockON").when(mockOn).toString();
        doReturn(mockOn).when(provider).getTestTransaction();
        doNothing().when(provider).validateTestTransaction(any(ObjectName.class));

        doReturn(mockOn).when(provider).getTestTransaction();
        doNothing().when(provider).abortTestTransaction(any(ObjectName.class));
        doReturn(mockOn).when(provider).getOrCreateTransaction();

        doNothing().when(provider).wipeTestTransaction(any(ObjectName.class));

        doReturn(configTransactionClient).when(configRegistry).getConfigTransactionClient(any(ObjectName.class));
        doReturn("mockConfigTransactionClient").when(configTransactionClient).toString();

        doReturn(mockOn).when(configTransactionClient).lookupConfigBean(anyString(), anyString());
    }

    @Test
    public void test() throws NetconfDocumentedException, ValidationException {
        EditConfig edit = new EditConfig(yangStoreSnapshot, provider, configRegistry,
                ValidateTest.NETCONF_SESSION_ID_FOR_REPORTING);
        EditConfigStrategy editStrat = mock(EditConfigStrategy.class);

        doNothing().when(editStrat).executeConfiguration(anyString(), anyString(), anyMapOf(String.class, AttributeConfigElement.class),
                any(ConfigTransactionClient.class), any(ServiceRegistryWrapper.class));

        EditConfigExecution editConfigExecution = mockExecution(editStrat);

        edit.getResponseInternal(XmlUtil.newDocument(), editConfigExecution);

        verify(provider).getTestTransaction();
        verify(provider).validateTestTransaction(mockOn);
        verify(provider).abortTestTransaction(mockOn);

        verify(provider).getOrCreateTransaction();

        // For every instance execute strat
        verify(editStrat, times(2/* Test */+ 2/* Set */ + 2/*Handle missing instance Test*/ + 2 /*Handle missing instance Set*/)).executeConfiguration(anyString(),
                anyString(), anyMapOf(String.class, AttributeConfigElement.class),
                any(ConfigTransactionClient.class), any(ServiceRegistryWrapper.class));
    }

    private EditConfigExecution mockExecution(EditConfigStrategy editStrat) throws NetconfDocumentedException {
        EditConfigExecution mock = mock(EditConfigExecution.class);
        doReturn(getMapping(editStrat)).when(mock).getResolvedXmlElements(any(ConfigTransactionClient.class));
        doReturn(getMappingDefinition(editStrat)).when(mock).getModulesDefinition(any(ConfigTransactionClient.class));
        doReturn(EditStrategyType.merge).when(mock).getDefaultStrategy();
        doReturn(true).when(mock).shouldSet();
        doReturn(true).when(mock).shouldTest();
        doReturn(mockServices()).when(mock).getServiceRegistryWrapper(any(ConfigTransactionClient.class));
        doReturn(new Services()).when(mock).getServices();
        return mock;
    }

    private Object getMappingDefinition(EditConfigStrategy editStrat) {
        Map<String, Multimap<String, ModuleElementDefinition>> result = Maps.newHashMap();

        Multimap<String, ModuleElementDefinition> innerMultimap = HashMultimap.create();
        Map<String, AttributeConfigElement> attributes = getSimpleAttributes();

        ModuleElementDefinition mockedDefinition = mock(ModuleElementDefinition.class);
        doReturn(editStrat).when(mockedDefinition).getEditStrategy();
        doReturn("i1").when(mockedDefinition).getInstanceName();
        innerMultimap.put("m1", mockedDefinition);

        ModuleElementDefinition mockedDefinition2 = mock(ModuleElementDefinition.class);
        doReturn(editStrat).when(mockedDefinition2).getEditStrategy();
        doReturn("i2").when(mockedDefinition2).getInstanceName();
        innerMultimap.put("m1", mockedDefinition2);

        result.put("n1", innerMultimap);

        return result;
    }

    private static ServiceReferenceReadableRegistry mockServiceRegistry() {
        ServiceReferenceReadableRegistry mock = mock(ServiceReferenceReadableRegistry.class);
        doReturn(
                Collections.emptyMap())
                .when(mock).getServiceMapping();
        doReturn("mockedServiceReg").when(mock).toString();

        return mock;
    }

    static ServiceRegistryWrapper mockServices() {
        return new ServiceRegistryWrapper(mockServiceRegistry());
    }

    private Map<String, Multimap<String, ModuleElementResolved>> getMapping(EditConfigStrategy editStrat) {
        Map<String, Multimap<String, ModuleElementResolved>> result = Maps.newHashMap();

        Multimap<String, ModuleElementResolved> innerMultimap = HashMultimap.create();
        Map<String, AttributeConfigElement> attributes = getSimpleAttributes();

        InstanceConfigElementResolved ice1 = mock(InstanceConfigElementResolved.class);
        doReturn(attributes).when(ice1).getConfiguration();
        doReturn(editStrat).when(ice1).getEditStrategy();
        innerMultimap.put("m1", new ModuleElementResolved("i1", ice1));

        InstanceConfigElementResolved ice2 = mock(InstanceConfigElementResolved.class);
        doReturn(attributes).when(ice2).getConfiguration();
        doReturn(editStrat).when(ice2).getEditStrategy();
        innerMultimap.put("m1", new ModuleElementResolved("i2", ice2));

        result.put("n1", innerMultimap);

        return result;
    }

    static Map<String, AttributeConfigElement> getSimpleAttributes() {
        Map<String, AttributeConfigElement> attributes = Maps.newHashMap();
        AttributeConfigElement ace1 = mock(AttributeConfigElement.class);
        doReturn("abcd").when(ace1).getResolvedDefaultValue();
        doReturn(Optional.<String> of("abc")).when(ace1).getResolvedValue();
        doReturn("mockedAce1").when(ace1).toString();
        doReturn("jmxNameAce1").when(ace1).getJmxName();
        attributes.put("a1", ace1);
        return attributes;
    }

}
