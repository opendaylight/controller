/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import java.util.Set;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacadeFactory;
import org.opendaylight.controller.config.util.capability.Capability;
import org.opendaylight.controller.config.util.capability.ModuleListener;
import org.opendaylight.controller.config.util.capability.YangModuleCapability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.yangtools.yang.model.api.Module;

public class NetconfOperationServiceFactoryImpl implements NetconfOperationServiceFactory {

    private final ConfigSubsystemFacadeFactory configFacadeFactory;

    public NetconfOperationServiceFactoryImpl(ConfigSubsystemFacadeFactory configFacadeFactory) {
        this.configFacadeFactory = configFacadeFactory;
    }

    @Override
    public NetconfOperationServiceImpl createService(String netconfSessionIdForReporting) {
        return new NetconfOperationServiceImpl(configFacadeFactory.createFacade(netconfSessionIdForReporting), netconfSessionIdForReporting);
    }

    @Override
    public Set<Capability> getCapabilities() {
        return configFacadeFactory.getCurrentCapabilities();
    }

    @Override
    public AutoCloseable registerCapabilityListener(final CapabilityListener listener) {
        return configFacadeFactory.getYangStoreService().registerModuleListener(new ModuleListener() {
            @Override
            public void onCapabilitiesChanged(Set<Module> added, Set<Module> removed) {
                listener.onCapabilitiesChanged(
                        transformModulesToCapabilities(added), transformModulesToCapabilities(removed));
            }
        });
    }

    private static final Function<Module, Capability> MODULE_TO_CAPABILITY = new Function<Module, Capability>() {
        @Override
        public Capability apply(final Module module) {
            return new YangModuleCapability(module, module.getSource());
        }
    };

    public static Set<Capability> transformModulesToCapabilities(Set<Module> modules) {
        return Sets.newHashSet(Collections2.transform(modules, MODULE_TO_CAPABILITY));
    }

}
