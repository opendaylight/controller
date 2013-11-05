package org.opendaylight.controller.md.compatibility.switchmanager

import org.opendaylight.controller.switchmanager.ISwitchManager
import org.opendaylight.controller.switchmanager.SpanConfig
import org.opendaylight.controller.switchmanager.SwitchConfig
import org.opendaylight.controller.switchmanager.SubnetConfig

/**
 * 
 * THis methods should be backed by config subsystem.
 * 
 */
abstract class ConfigurableSwitchManager implements ISwitchManager {

    override saveSwitchConfig() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override removeSpanConfig(SpanConfig cfgObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override addSubnet(SubnetConfig configObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")

    }

    final override addSpanConfig(SpanConfig configObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")

    }

    final override getSpanConfigList() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")

    }

    final override updateSwitchConfig(SwitchConfig cfgObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")

    }

    final override updateNodeConfig(SwitchConfig switchConfig) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")

    }

    final override getSubnetConfig(String subnet) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    final override removeNodeConfig(String nodeId) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    final override removeSubnet(SubnetConfig configObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    final override getSubnetsConfigList() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    final override getSwitchConfig(String nodeId) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override modifySubnet(SubnetConfig configObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
}
