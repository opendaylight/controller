package org.opendaylight.controller.config.manager.impl.osgi;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Dictionary;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.BindingContextProvider;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.RefreshingSCPModuleInfoRegistry;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.api.ModuleInfoRegistry;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class RefreshingSCPModuleInfoRegistryTest {
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

        RefreshingSCPModuleInfoRegistry scpreg = new RefreshingSCPModuleInfoRegistry(reg, prov, classLoadingStrat, codecRegistryProvider, ctxt);

        doNothing().when(servReg).unregister();

        YangModuleInfo modInfo = mock(YangModuleInfo.class);
        doReturn("").when(modInfo).toString();
        ObjectRegistration<YangModuleInfo> ymi = mock(ObjectRegistration.class);
        doReturn(ymi).when(reg).registerModuleInfo(modInfo);

        scpreg.registerModuleInfo(modInfo);

        verify(codecRegistryProvider).update(classLoadingStrat, prov);

        scpreg.close();

        Mockito.verify(servReg, Mockito.times(1)).setProperties(any(Dictionary.class));
        Mockito.verify(servReg, Mockito.times(1)).unregister();
    }
}
