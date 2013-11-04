package org.opendaylight.controller.md.compatibility.switchmanager

import org.opendaylight.controller.switchmanager.ISwitchManager
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.controller.sal.core.Property
import java.util.List
import org.opendaylight.controller.switchmanager.SpanConfig
import org.opendaylight.controller.switchmanager.SubnetConfig
import org.opendaylight.controller.sal.core.Node
import java.net.InetAddress

class CompatibleSwitchManager extends ConfigurableSwitchManager implements ISwitchManager {

    override addNodeConnectorProp(NodeConnector nodeConnector, Property prop) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")

    }

    override addPortsToSubnet(String name, List<String> nodeConnectors) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")

    }

    override createProperty(String propName, String propValue) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override doesNodeConnectorExist(NodeConnector nc) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getControllerMAC() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getControllerProperties() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getControllerProperty(String propertyName) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getNetworkDevices() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getNodeConnector(Node node, String nodeConnectorName) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getNodeConnectorProp(NodeConnector nodeConnector, String propName) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getNodeConnectorProps(NodeConnector nodeConnector) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getNodeConnectors(Node node) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getNodeDescription(Node node) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getNodeMAC(Node node) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getNodeProp(Node node, String propName) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getNodeProps(Node node) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getNodes() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getPhysicalNodeConnectors(Node node) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getSpanPorts(Node node) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getSubnetByNetworkAddress(InetAddress networkAddress) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getUpNodeConnectors(Node node) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override isNodeConnectorEnabled(NodeConnector nodeConnector) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override isSpecial(NodeConnector p) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override modifySubnet(SubnetConfig configObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override removeControllerProperty(String propertyName) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override removeNodeAllProps(Node node) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override removeNodeConnectorAllProps(NodeConnector nodeConnector) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override removeNodeConnectorProp(NodeConnector nc, String propName) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override removeNodeProp(Node node, String propName) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override removePortsFromSubnet(String name, List<String> nodeConnectors) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override removeSubnet(String name) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override saveSwitchConfig() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override setControllerProperty(Property property) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override setNodeProp(Node node, Property prop) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override removeSpanConfig(SpanConfig cfgObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

}
