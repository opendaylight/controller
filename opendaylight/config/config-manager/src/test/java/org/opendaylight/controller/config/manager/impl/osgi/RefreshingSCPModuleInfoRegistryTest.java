package org.opendaylight.controller.config.manager.impl.osgi;

import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import org.eclipse.osgi.internal.serviceregistry.ServiceRegistrationImpl;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Any;
import org.mockito.verification.VerificationMode;
import org.opendaylight.controller.config.manager.impl.jmx.ServiceReference;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.RefreshingSCPModuleInfoRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.rpc.context.rev130617.$YangModuleInfoImpl;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.sal.binding.generator.api.ModuleInfoRegistry;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.*;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class RefreshingSCPModuleInfoRegistryTest {
    @Test
    public void testConstructor() throws Exception {
        ModuleInfoRegistry reg = mock(ModuleInfoRegistry.class);
        SchemaContextProvider prov = mock(SchemaContextProvider.class);
        doReturn("string").when(prov).toString();

        BundleContext ctxt = mock(BundleContext.class);
        Dictionary dict = new Hashtable();
        ServiceRegistration servReg = mock(ServiceRegistration.class);
        doReturn(servReg).when(ctxt).registerService(Mockito.any(Class.class), Mockito.any(SchemaContextProvider.class), Mockito.any(Dictionary.class));
        doReturn(servReg).when(ctxt).registerService(Mockito.anyString(), Mockito.any(Object.class), Mockito.any(Dictionary.class));
        RefreshingSCPModuleInfoRegistry scpreg = new RefreshingSCPModuleInfoRegistry(reg, prov, ctxt);

        doNothing().when(servReg).setProperties(null);
        doNothing().when(servReg).unregister();
        scpreg.close();
        Mockito.verify(servReg, Mockito.times(1)).unregister();

        YangModuleInfo modInfo = mock(YangModuleInfo.class);
        doReturn("").when(modInfo).toString();
        ObjectRegistration<YangModuleInfo> ymi = mock(ObjectRegistration.class);
        doReturn(ymi).when(reg).registerModuleInfo(modInfo);
        scpreg.registerModuleInfo(modInfo);
        Mockito.verify(servReg, Mockito.times(1)).setProperties(null);
    }
}
