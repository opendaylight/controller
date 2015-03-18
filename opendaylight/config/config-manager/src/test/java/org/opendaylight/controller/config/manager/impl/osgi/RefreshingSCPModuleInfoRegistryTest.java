package org.opendaylight.controller.config.manager.impl.osgi;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.BindingContextProvider;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.RefreshingSCPModuleInfoRegistry;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.api.ModuleInfoRegistry;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;

public class RefreshingSCPModuleInfoRegistryTest {
    @Test
    public void testConstructor() throws Exception {
        ModuleInfoRegistry reg = mock(ModuleInfoRegistry.class);
        SchemaContextProvider prov = mock(SchemaContextProvider.class);
        doReturn("string").when(prov).toString();

        final ClassLoadingStrategy classLoadingStrat = mock(ClassLoadingStrategy.class);
        final BindingContextProvider codecRegistryProvider = mock(BindingContextProvider.class);
        doNothing().when(codecRegistryProvider).update(classLoadingStrat, prov);

        RefreshingSCPModuleInfoRegistry scpreg = new RefreshingSCPModuleInfoRegistry(reg, prov, classLoadingStrat, codecRegistryProvider);

        YangModuleInfo modInfo = mock(YangModuleInfo.class);
        doReturn("").when(modInfo).toString();
        ObjectRegistration<YangModuleInfo> ymi = mock(ObjectRegistration.class);
        doReturn(ymi).when(reg).registerModuleInfo(modInfo);

        scpreg.registerModuleInfo(modInfo);

        verify(codecRegistryProvider).update(classLoadingStrat, prov);
    }
}
