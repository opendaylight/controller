package org.opendaylight.controller.sal.networkconfig.bridgedomain.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IBridgeDomainConfigService;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IPluginInBridgeDomainConfigService;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainConfigService implements IBridgeDomainConfigService {
    protected static final Logger logger = LoggerFactory
            .getLogger(BridgeDomainConfigService.class);
    private ConcurrentMap<String, IPluginInBridgeDomainConfigService> pluginService =
            new ConcurrentHashMap<String, IPluginInBridgeDomainConfigService>();

    void setPluginInService (Map props, IPluginInBridgeDomainConfigService s) {
        String type = null;
        Object value = props.get(GlobalConstants.PROTOCOLPLUGINTYPE.toString());
        if (value instanceof String) {
            type = (String) value;
        }
        if (type == null) {
            logger.error("Received a PluginInConnectionService without any "
                    + "protocolPluginType provided");
        } else {
            this.pluginService.put(type, s);
        }
    }

    void unsetPluginInService(Map props, IPluginInBridgeDomainConfigService s) {
        String type = null;

        Object value = props.get(GlobalConstants.PROTOCOLPLUGINTYPE.toString());
        if (value instanceof String) {
            type = (String) value;
        }
        if (type == null) {
            logger.error("Received a PluginInConnectionService without any "
                    + "protocolPluginType provided");
        } else if (this.pluginService.get(type).equals(s)) {
            this.pluginService.remove(type);
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        if (this.pluginService != null) {
            this.pluginService.clear();
        }
    }

    @Override
    public Status createBridgeDomain(Node node, String bridgeIdentifier, Map<ConfigConstants, Object> params)
            throws Throwable {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(node.getType());
            if (plugin != null) {
                return plugin.createBridgeDomain(node, bridgeIdentifier, params);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Requested Plugin Service Not available");
    }

    @Override
    public Status deleteBridgeDomain(Node node, String bridgeIdentifier) {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(node.getType());
            if (plugin != null) {
                return plugin.deleteBridgeDomain(node, bridgeIdentifier);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Requested Plugin Service Not available");
    }

    @Override
    public List<String> getBridgeDomains(Node node) {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(node.getType());
            if (plugin != null) {
                return plugin.getBridgeDomains(node);
            }
        }
        return null;
    }

    @Override
    public Status addBridgeDomainConfig(Node node, String bridgeIdentifier, Map<ConfigConstants, Object> params) {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(node.getType());
            if (plugin != null) {
                return plugin.addBridgeDomainConfig(node, bridgeIdentifier, params);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Requested Plugin Service Not available");
    }

    @Override
    public Status removeBridgeDomainConfig(Node node, String bridgeIdentifier, Map<ConfigConstants, Object> params) {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(node.getType());
            if (plugin != null) {
                return plugin.removeBridgeDomainConfig(node, bridgeIdentifier, params);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Requested Plugin Service Not available");
    }

    @Override
    public Map<ConfigConstants, Object> getBridgeDomainConfigs(Node node, String bridgeIdentifier) {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(node.getType());
            if (plugin != null) {
                return plugin.getBridgeDomainConfigs(node, bridgeIdentifier);
            }
        }
        return null;
    }

    @Override
    public Node getBridgeDomainNode(Node configNode, String bridgeIdentifier) {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(configNode.getType());
            if (plugin != null) {
                return plugin.getBridgeDomainNode(configNode, bridgeIdentifier);
            }
        }
        return null;
    }

    @Override
    public Status addPort(Node node, String bridgeIdentifier, String portIdentifier, Map<ConfigConstants, Object> params) {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(node.getType());
            if (plugin != null) {
                return plugin.addPort(node, bridgeIdentifier, portIdentifier, params);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Requested Plugin Service Not available");
    }

    @Override
    public Status deletePort(Node node, String bridgeIdentifier, String portIdentifier) {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(node.getType());
            if (plugin != null) {
                return plugin.deletePort(node, bridgeIdentifier, portIdentifier);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Requested Plugin Service Not available");
    }

    @Override
    public Status addPortConfig(Node node, String bridgeIdentifier, String portIdentifier,
            Map<ConfigConstants, Object> params) {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(node.getType());
            if (plugin != null) {
                return plugin.addPortConfig(node, bridgeIdentifier, portIdentifier, params);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Requested Plugin Service Not available");
    }

    @Override
    public Status removePortConfig(Node node, String bridgeIdentifier, String portIdentifier,
            Map<ConfigConstants, Object> params) {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(node.getType());
            if (plugin != null) {
                return plugin.removePortConfig(node, bridgeIdentifier, portIdentifier, params);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Requested Plugin Service Not available");
    }

    @Override
    public Map<ConfigConstants, Object> getPortConfigs(Node node, String bridgeIdentifier, String portIdentifier) {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(node.getType());
            if (plugin != null) {
                return plugin.getPortConfigs(node, bridgeIdentifier, portIdentifier);
            }
        }
        return null;
    }

    @Override
    public NodeConnector getNodeConnector(Node configNode, String bridgeIdentifier, String portIdentifier) {
        if (pluginService != null) {
            IPluginInBridgeDomainConfigService plugin = this.pluginService.get(configNode.getType());
            if (plugin != null) {
                return plugin.getNodeConnector(configNode, bridgeIdentifier, portIdentifier);
            }
        }
        return null;
    }
}