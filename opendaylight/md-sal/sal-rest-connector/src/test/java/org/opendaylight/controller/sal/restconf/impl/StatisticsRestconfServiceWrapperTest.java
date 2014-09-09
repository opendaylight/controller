/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import javax.ws.rs.core.UriInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;

public class StatisticsRestconfServiceWrapperTest {

    private static RestconfImpl mockedRestconfImpl = mock(RestconfImpl.class);
    private static StatisticsRestconfServiceWrapper statisticRestconf;

    @BeforeClass
    public static void initialize() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Constructor<StatisticsRestconfServiceWrapper>[] declaredConstructors;
        declaredConstructors = (Constructor<StatisticsRestconfServiceWrapper>[]) StatisticsRestconfServiceWrapper.class
                .getDeclaredConstructors();
        assertEquals(1, declaredConstructors.length);
        Constructor<StatisticsRestconfServiceWrapper> constructor = declaredConstructors[0];
        constructor.setAccessible(true);
        statisticRestconf = constructor.newInstance(mockedRestconfImpl);
    }

    @Test
    public void callMethods() {
        statisticRestconf.getRoot();
        verify(mockedRestconfImpl).getRoot();

        final Node<?> mockedNode = mock(Node.class);
        statisticRestconf.createConfigurationData(mockedNode);
        verify(mockedRestconfImpl).createConfigurationData(mockedNode);

        String dummyIdentifier = "";
        statisticRestconf.createConfigurationData(dummyIdentifier, mockedNode);
        verify(mockedRestconfImpl).createConfigurationData(dummyIdentifier, mockedNode);

        statisticRestconf.deleteConfigurationData(dummyIdentifier);
        verify(mockedRestconfImpl).deleteConfigurationData(dummyIdentifier);

        final UriInfo mockedUriInfo = mock(UriInfo.class);
        statisticRestconf.getAvailableStreams(mockedUriInfo);
        verify(mockedRestconfImpl).getAvailableStreams(mockedUriInfo);

        statisticRestconf.getOperations(mockedUriInfo);
        verify(mockedRestconfImpl).getOperations(mockedUriInfo);

        statisticRestconf.getOperations(dummyIdentifier, mockedUriInfo);
        verify(mockedRestconfImpl).getOperations(dummyIdentifier, mockedUriInfo);

        CompositeNode mockedCompositeNode = mock(CompositeNode.class);
        statisticRestconf.invokeRpc(dummyIdentifier, mockedCompositeNode, mockedUriInfo);
        verify(mockedRestconfImpl).invokeRpc(dummyIdentifier, mockedCompositeNode, mockedUriInfo);

        statisticRestconf.invokeRpc(dummyIdentifier, dummyIdentifier, mockedUriInfo);
        verify(mockedRestconfImpl).invokeRpc(dummyIdentifier, dummyIdentifier, mockedUriInfo);

        statisticRestconf.readConfigurationData(dummyIdentifier, mockedUriInfo);
        verify(mockedRestconfImpl).readConfigurationData(dummyIdentifier, mockedUriInfo);

        statisticRestconf.readOperationalData(dummyIdentifier, mockedUriInfo);
        verify(mockedRestconfImpl).readOperationalData(dummyIdentifier, mockedUriInfo);

        statisticRestconf.subscribeToStream(dummyIdentifier, mockedUriInfo);
        verify(mockedRestconfImpl).subscribeToStream(dummyIdentifier, mockedUriInfo);

        statisticRestconf.updateConfigurationData(dummyIdentifier, mockedNode);
        verify(mockedRestconfImpl).updateConfigurationData(dummyIdentifier, mockedNode);

        statisticRestconf.getModule(dummyIdentifier, mockedUriInfo);
        verify(mockedRestconfImpl).getModule(dummyIdentifier, mockedUriInfo);

        statisticRestconf.getModules(mockedUriInfo);
        verify(mockedRestconfImpl).getModules(mockedUriInfo);

        statisticRestconf.getModules(dummyIdentifier, mockedUriInfo);
        verify(mockedRestconfImpl).getModules(dummyIdentifier, mockedUriInfo);

        assertEquals(BigInteger.valueOf(2), statisticRestconf.getRpc());
        assertEquals(BigInteger.valueOf(1), statisticRestconf.getOperationalGet());
        assertEquals(BigInteger.valueOf(1), statisticRestconf.getConfigPut());
        assertEquals(BigInteger.valueOf(2), statisticRestconf.getConfigPost());
        assertEquals(BigInteger.valueOf(1), statisticRestconf.getConfigGet());
        assertEquals(BigInteger.valueOf(0), statisticRestconf.getConfigDelete());
    }

}
