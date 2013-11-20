package org.opendaylight.controller.md.inventory.manager;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.Port;

/**
 * Interface that describes methods for updating Port database.
 */
public interface IPortUpdateImpl {

    /**
     * Updates the Port Database Switch updates the Ports as it gets the Events
     */
    public void updatePortDb(Port portUpdateDataObject);

}
