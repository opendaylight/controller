package org.opendaylight.controller.config.persist.mapping;

import com.google.common.collect.Sets;
import java.util.Set;
import org.opendaylight.controller.config.persist.mapping.osgi.YangStoreService;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.config.util.capability.Capability;
import org.opendaylight.controller.config.util.capability.YangModuleCapability;
import org.opendaylight.yangtools.yang.model.api.Module;

public class ConfigSubsystemFacadeFactory {

    private ConfigRegistryClient cfgRegClient;
    private ConfigRegistryJMXClient cfgRegClientNoNotifications;
    private YangStoreService yangStoreService;

    public ConfigSubsystemFacadeFactory(final ConfigRegistryClient cfgRegClient, final ConfigRegistryJMXClient jmxClientNoNotifications, final YangStoreService yangStoreService) {
        this.cfgRegClient = cfgRegClient;
        this.cfgRegClientNoNotifications = jmxClientNoNotifications;
        this.yangStoreService = yangStoreService;
    }

    /**
     * Create new instance of ConfigSubsystemFacade. Each instance works with a dedicated transaction provider, making
     * the instances suitable for facade-per-client use.
     */
    public ConfigSubsystemFacade createFacade(final String id) {
        return new ConfigSubsystemFacade(cfgRegClient, cfgRegClientNoNotifications, yangStoreService, id);
    }

    public YangStoreService getYangStoreService() {
        return yangStoreService;
    }

    public Set<Capability> getCurrentCapabilities() {
        Set<Module> modules = yangStoreService.getModules();
        final Set<Capability> capabilities = Sets.newHashSet();
        for (Module module : modules) {
            capabilities.add(new YangModuleCapability(module, yangStoreService.getModuleSource(module)));
        }

        return capabilities;
    }


}
