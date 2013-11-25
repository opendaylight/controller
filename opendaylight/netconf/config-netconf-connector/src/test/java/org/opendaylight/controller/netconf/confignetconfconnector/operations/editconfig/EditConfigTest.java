/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Config;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.InstanceConfigElementResolved;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleElementResolved;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.ValidateTest;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfigXmlParser.EditConfigExecution;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;

import javax.management.ObjectName;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class EditConfigTest {

    @Mock
    private YangStoreSnapshot yangStoreSnapshot;
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
    public void test() throws NetconfDocumentedException {
        EditConfig edit = new EditConfig(yangStoreSnapshot, provider, configRegistry,
                ValidateTest.NETCONF_SESSION_ID_FOR_REPORTING);
        EditConfigStrategy editStrat = mock(EditConfigStrategy.class);
        doNothing().when(editStrat).executeConfiguration(anyString(), anyString(), anyMap(),
                any(ConfigTransactionClient.class));
        Map<String, Multimap<String, ModuleElementResolved>> resolvedXmlElements = getMapping(editStrat);

        Config cfg = mock(Config.class);
        XmlElement xmlElement = mock(XmlElement.class);
        Set<ObjectName> instancesForFillingServiceRefMapping = Collections.emptySet();
        EditStrategyType defaultStrategy = EditStrategyType.getDefaultStrategy();
        doReturn(resolvedXmlElements).when(cfg).fromXml(xmlElement, instancesForFillingServiceRefMapping, defaultStrategy);

        EditConfigExecution editConfigExecution = new EditConfigExecution(null, cfg, xmlElement,
                EditConfigXmlParser.TestOption.testThenSet, instancesForFillingServiceRefMapping, defaultStrategy);

        edit.getResponseInternal(XmlUtil.newDocument(), editConfigExecution);

        verify(provider).getTestTransaction();
        verify(provider).validateTestTransaction(mockOn);
        verify(provider).abortTestTransaction(mockOn);

        verify(provider).getOrCreateTransaction();

        // For every instance execute strat
        verify(editStrat, times(2/* Test */+ 2/* Set */)).executeConfiguration(anyString(), anyString(), anyMap(),
                any(ConfigTransactionClient.class));
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
