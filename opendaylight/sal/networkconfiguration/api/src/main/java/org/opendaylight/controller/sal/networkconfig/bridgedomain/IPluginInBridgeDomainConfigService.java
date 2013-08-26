package org.opendaylight.controller.sal.networkconfig.bridgedomain;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.Status;

/**
 * This interface defines bridge domain configuration service methods to be
 * implemented by protocol plugins
 */
public interface IPluginInBridgeDomainConfigService {
    /**
     * Create a Bridge Domain
     *
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Domain
     * @param params Map representation of config name (ConfigConstants) and Parameter value (represented as Object).
     * @return Status.StatusCode.SUCCESS if bridge domain is created successfully. Failure Status otherwise.
     * @note This method will return false if one or more of the supplied params is not supported by the
     * protocol plugin that serves the Node.
     */
    public Status createBridgeDomain(Node node, String bridgeIdentifier, Map<ConfigConstants, Object> params) throws Throwable;

    /**
     * Delete a Bridge Domain
     *
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Domain
     * @return Status.StatusCode.SUCCESS if bridge domain is deleted successfully. Failure Status otherwise.
     */
    public Status deleteBridgeDomain(Node node, String bridgeIdentifier);

    /**
     * Returns the configured Bridge Domains
     *
     * @param node Node serving this configuration service
     * @return List of Bridge Domain Identifiers
     */
    public List<String> getBridgeDomains(Node node);

    /**
     * add Bridge Domain Configuration
     *
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Domain
     * @param params Map representation of config Name (ConfigConstants) and config value(represented as Object).
     * @return Status.StatusCode.SUCCESS if bridge domain configuration is added successfully. Failure Status otherwise.
     * @note This method will return false if one or more of the supplied params is not supported by the
     * protocol plugin that serves the Node.
     */
    public Status addBridgeDomainConfig(Node node, String bridgeIdentifier, Map<ConfigConstants, Object> params);

    /**
     * Delete Bridge Domain Configuration
     *
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Domain
     * @param params Map representation of config name (ConfigConstants) and Parameter value (represented as Object).
     * @return Status.StatusCode.SUCCESS if bridge domain configuration is deleted successfully. Failure Status otherwise.
     * @note This method will return false if one or more of the supplied params is not supported by the
     * protocol plugin that serves the Node.
     */
    public Status removeBridgeDomainConfig(Node node, String bridgeIdentifier, Map<ConfigConstants, Object> params);

    /**
     * Returns Bridge Domain Configurations
     *
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Domain
     * @return Map representation of config Name (ConfigConstants) and config value(represented as Object).
     */

    public Map<ConfigConstants, Object> getBridgeDomainConfigs(Node node, String bridgeIdentifier);

    /**
     * Returns a Node dedicated to a Bridge Domain (if available) that is created using createBridgeDomain.
     * @param configNode Node serving this configuration service.
     * @param bridgeIdentifier Name of the bridge domain that would map to a dedicated Node
     * @return Node dedicated to a bridge domain that is created using createBridgeDomain.
     *         returns null if there is no such dedicated node is available or represented.
     */
    public Node getBridgeDomainNode(Node configNode, String bridgeIdentifier);

    /**
     * Add a port to a bridge domain
     *
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Domain
     * @param portIdentifier String representation of a Port.
     * @param params Map representation of config name (ConfigConstants) and Parameter value (represented as Object).
     * @return Status.StatusCode.SUCCESS if a port is added successfully. Failure Status otherwise.
     * @note This method will return false if one or more of the supplied params is not supported by the
     * protocol plugin that serves the Node.
     */
    public Status addPort(Node node, String bridgeIdentifier, String portIdentifier,
                           Map<ConfigConstants, Object> params);

    /**
     * Delete a Port from a bridge domain
     *
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Domain
     * @param portIdentifier String representation of a Port.
     * @return Status.StatusCode.SUCCESS if a port is added successfully. Failure Status otherwise.
     */
    public Status deletePort(Node node, String bridgeIdentifier, String portIdentifier);

    /**
     * add Port Configuration
     *
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Domain
     * @param portIdentifier String representation of a Port.
     * @param params Map representation of config name (ConfigConstants) and Parameter value (represented as Object).
     * @return Status.StatusCode.SUCCESS if a port configuration is added successfully. Failure Status otherwise.
     * @note This method will return false if one or more of the supplied params is not supported by the
     * protocol plugin that serves the Node.
     */
    public Status addPortConfig(Node node, String bridgeIdentifier, String portIdentifier,
                                 Map<ConfigConstants, Object> params);

    /**
     * Delete Port Configuration
     *
     * @param node Node serving this configuration service
     * @param portIdentifier String representation of a Port.
     * @param config Map representation of ConfigName and Configuration Value in Strings.
     * @return Status.StatusCode.SUCCESS if a port configuration is removed successfully. Failure Status otherwise.
     * @note This method will return false if one or more of the supplied params is not supported by the
     * protocol plugin that serves the Node.
     */
    public Status removePortConfig(Node node, String bridgeIdentifier, String portIdentifier, Map<ConfigConstants, Object> params);

    /**
     * Returns Port Configurations
     *
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Domain
     * @param portIdentifier String representation of a Port.
     * @return Map representation of Configuration Name (ConfigConstants) and Configuration value (represented as Object).
     */
    public Map<ConfigConstants, Object> getPortConfigs(Node node, String bridgeIdentifier, String portIdentifier);


    /**
     * Returns a NodeConnector mapped to a Port (if available) that is created using addPort.
     * @param configNode Node serving this configuration service.
     * @param bridgeIdentifier Name of the bridge domain that would map to a dedicated Node
     * @param portIdentifier String representation of a Port.
     * @return NodeConnector that is mapped to a port created using addPort.
     *         returns null if there is no such nodeConnector is available or mapped.
     */
    public NodeConnector getNodeConnector(Node configNode, String bridgeIdentifier, String portIdentifier);
}