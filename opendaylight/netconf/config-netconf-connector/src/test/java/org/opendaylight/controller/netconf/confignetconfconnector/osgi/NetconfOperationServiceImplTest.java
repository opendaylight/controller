/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.config.api.LookupRegistry;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.yangtools.yang.common.QName;

public class NetconfOperationServiceImplTest {

    private static final Date date1970_01_01;

    static {
        try {
            date1970_01_01 = new SimpleDateFormat("yyyy-MM-dd").parse("1970-01-01");
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

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

    @Test
    public void testCheckConsistencyBetweenYangStoreAndConfig_yangStoreMore() throws Exception {
        try {
            NetconfOperationServiceImpl.checkConsistencyBetweenYangStoreAndConfig(mockJmxClient("qname1"),
                    mockYangStoreSnapshot("qname2", "qname1"));
            fail("An exception of type " + IllegalStateException.class + " was expected");
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            Assert.assertThat(
                    message,
                    CoreMatchers
                    .containsString("missing from config subsystem but present in yangstore: [(namespace?revision=1970-01-01)qname2]"));
            Assert.assertThat(
                    message,
                    CoreMatchers
                    .containsString("All modules present in config: [(namespace?revision=1970-01-01)qname1]"));
        }
    }

    private YangStoreContext mockYangStoreSnapshot(final String... qnames) {
        YangStoreContext mock = mock(YangStoreContext.class);

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

    private ModuleMXBeanEntry mockMBeanEntry(final String qname) {
        ModuleMXBeanEntry mock = mock(ModuleMXBeanEntry.class);
        QName q = getQName(qname);
        doReturn(q).when(mock).getYangModuleQName();
        return mock;
    }

    private QName getQName(final String qname) {
        return QName.create(URI.create("namespace"), date1970_01_01, qname);
    }

    private LookupRegistry mockJmxClient(final String... visibleQNames) {
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
