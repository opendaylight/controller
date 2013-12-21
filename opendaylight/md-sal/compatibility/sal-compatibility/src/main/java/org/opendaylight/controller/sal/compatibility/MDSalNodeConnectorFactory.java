package org.opendaylight.controller.sal.compatibility;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.INodeConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MDSalNodeConnectorFactory implements INodeConnectorFactory{
    private Logger logger = LoggerFactory.getLogger(MDSalNodeConnectorFactory.class);

    @Override
    public NodeConnector fromStringNoNode(String type, String id, Node node) {
        try {
            return new NodeConnector(type, id, node);
        } catch (ConstructionException e) {
            logger.error("Could not construct NodeConnector", e);
        }
        return null;

    }
}
