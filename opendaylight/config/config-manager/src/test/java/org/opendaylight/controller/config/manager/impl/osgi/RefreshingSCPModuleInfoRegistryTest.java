/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl.osgi;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.util.Dictionary;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.BindingContextProvider;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.RefreshingSCPModuleInfoRegistry;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.api.ModuleInfoRegistry;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class RefreshingSCPModuleInfoRegistryTest {

    @Mock
    SchemaSourceProvider<YangTextSchemaSource> sourceProvider;

    @Before public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConstructor() throws Exception {
        ModuleInfoRegistry reg = mock(ModuleInfoRegistry.class);
        SchemaContextProvider prov = mock(SchemaContextProvider.class);
        doReturn("string").when(prov).toString();
        BundleContext ctxt = mock(BundleContext.class);
        ServiceRegistration<?> servReg = mock(ServiceRegistration.class);
        doReturn(servReg).when(ctxt).registerService(any(Class.class), any(SchemaContextProvider.class), any(Dictionary.class));
        doReturn(servReg).when(ctxt).registerService(Mockito.anyString(), any(Object.class), any(Dictionary.class));
        doNothing().when(servReg).setProperties(any(Dictionary.class));

        final ClassLoadingStrategy classLoadingStrat = mock(ClassLoadingStrategy.class);
        final BindingContextProvider codecRegistryProvider = mock(BindingContextProvider.class);
        doNothing().when(codecRegistryProvider).update(classLoadingStrat, prov);
        final BindingRuntimeContext bindingRuntimeContext = mock(BindingRuntimeContext.class);
        doReturn("B-runtime-context").when(bindingRuntimeContext).toString();
        doReturn(bindingRuntimeContext).when(codecRegistryProvider).getBindingContext();

        RefreshingSCPModuleInfoRegistry scpreg = new RefreshingSCPModuleInfoRegistry(reg, prov, classLoadingStrat, sourceProvider, codecRegistryProvider, ctxt);

        doNothing().when(servReg).unregister();

        YangModuleInfo modInfo = mock(YangModuleInfo.class);
        doReturn("").when(modInfo).toString();
        ObjectRegistration<YangModuleInfo> ymi = mock(ObjectRegistration.class);
        doReturn(ymi).when(reg).registerModuleInfo(modInfo);

        scpreg.registerModuleInfo(modInfo);
        scpreg.updateService();

        verify(codecRegistryProvider).update(classLoadingStrat, prov);

        scpreg.close();

        Mockito.verify(servReg, Mockito.times(1)).setProperties(any(Dictionary.class));
        Mockito.verify(servReg, Mockito.times(1)).unregister();
    }
}
