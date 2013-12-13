/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.opendaylight.controller.config.api.LookupRegistry;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.yangtools.yang.common.QName;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class NetconfOperationServiceImplTest {

    private Date date = new Date(0);

    @Test
    public void testCheckConsistencyBetweenYangStoreAndConfig_ok() throws Exception {
        NetconfOperationServiceImpl.checkConsistencyBetweenYangStoreAndConfig(
                mockJmxClient("qname1", "qname2"),
                mockYangStoreSnapshot("qname2", "qname1"));
    }

    @Test
    public void testCheckConsistencyBetweenYangStoreAndConfig_ok2() throws Exception {
        NetconfOperationServiceImpl.checkConsistencyBetweenYangStoreAndConfig(
                mockJmxClient("qname1", "qname2", "qname4", "qname5"),
                mockYangStoreSnapshot("qname2", "qname1"));
    }

    @Test
    public void testCheckConsistencyBetweenYangStoreAndConfig_ok3() throws Exception {
        NetconfOperationServiceImpl.checkConsistencyBetweenYangStoreAndConfig(
                mockJmxClient(),
                mockYangStoreSnapshot());
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckConsistencyBetweenYangStoreAndConfig_yangStoreMore() throws Exception {
        try {
            NetconfOperationServiceImpl.checkConsistencyBetweenYangStoreAndConfig(mockJmxClient("qname1"),
                    mockYangStoreSnapshot("qname2", "qname1"));
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            Assert.assertThat(
                    message,
                    JUnitMatchers
                            .containsString(" missing from config subsystem but present in yangstore: [(namespace?revision=1970-01-01)qname2]"));
            Assert.assertThat(
                    message,
                    JUnitMatchers
                            .containsString("All modules present in config: [(namespace?revision=1970-01-01)qname1]"));
            throw e;
        }
    }

    private YangStoreSnapshot mockYangStoreSnapshot(String... qnames) {
        YangStoreSnapshot mock = mock(YangStoreSnapshot.class);

        Map<String, Map<String, ModuleMXBeanEntry>> map = Maps.newHashMap();

        Map<String, ModuleMXBeanEntry> innerMap = Maps.newHashMap();

        int i = 1;
        for (String qname : qnames) {
            innerMap.put(Integer.toString(i++), mockMBeanEntry(qname));
        }

        map.put("1", innerMap);

        doReturn(map).when(mock).getModuleMXBeanEntryMap();

        return mock;
    }

    private ModuleMXBeanEntry mockMBeanEntry(String qname) {
        ModuleMXBeanEntry mock = mock(ModuleMXBeanEntry.class);
        QName q = getQName(qname);
        doReturn(q).when(mock).getYangModuleQName();
        return mock;
    }

    private QName getQName(String qname) {
        return new QName(URI.create("namespace"), date, qname);
    }

    private LookupRegistry mockJmxClient(String... visibleQNames) {
        LookupRegistry mock = mock(LookupRegistry.class);
        Set<String> qnames = Sets.newHashSet();
        for (String visibleQName : visibleQNames) {
            QName q = getQName(visibleQName);
            qnames.add(q.toString());
        }
        doReturn(qnames).when(mock).getAvailableModuleFactoryQNames();
        return mock;
    }
}
