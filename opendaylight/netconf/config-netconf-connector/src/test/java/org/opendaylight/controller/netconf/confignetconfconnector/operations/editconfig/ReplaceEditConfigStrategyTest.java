/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;

import javax.management.Attribute;
import javax.management.ObjectName;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ReplaceEditConfigStrategyTest {

    @Mock
    private ConfigTransactionClient ta;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doNothing().when(ta).destroyConfigBean(anyString(), anyString());
        doReturn(mockON()).when(ta).lookupConfigBean(anyString(), anyString());
        doNothing().when(ta).setAttribute(any(ObjectName.class), anyString(), any(Attribute.class));
    }

    @Test
    public void test() throws Exception {
        ReplaceEditConfigStrategy strat = new ReplaceEditConfigStrategy();

        Map<String, AttributeConfigElement> map = EditConfigTest.getSimpleAttributes();

        doReturn(Sets.newHashSet(mockON(), mockON())).when(ta).lookupConfigBeans();

        strat.executeConfiguration("m1", "i1", map, ta);

        verify(ta).lookupConfigBean(anyString(), anyString());
        verify(ta).setAttribute(any(ObjectName.class), anyString(), any(Attribute.class));
    }

    ObjectName mockON() {
        ObjectName mock = mock(ObjectName.class);
        doReturn("mockON").when(mock).toString();
        return mock;
    }

}
