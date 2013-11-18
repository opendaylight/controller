package org.opendaylight.controller.forwardingrulesmanager.consumer.impl;

import java.util.List;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Interface that describes methods for accessing the flows database.
 */
public interface IForwardingRulesManager {

    /**
     * Returns the specifications of all the flows configured for all the
     * switches on the current container
     *
     * @return the list of flow configurations present in the database
     */
    public List<DataObject> get();

    /**
     * Returns the specification of the flow configured for the given network
     * node on the current container
     *
     * @param name
     *            the flow name
     * @param n
     *            the network node identifier
     * @return the {@code FlowConfig} object
     */
    public DataObject getWithName(String name, Node n);

}
