package org.opendaylight.controller.networkconfig.neutron;

import java.util.List;

/**
 * This interface defines the methods for CRUD of Neutron objects
 *
 */

public interface INeutronCRUD<N extends INeutronObject> {
    /**
     * Applications call this interface method to determine if a particular
     * Neutron object exists
     *
     * @param uuid
     *            UUID of the Neutron object
     * @return boolean
     */

    public boolean exists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Neutron object exists
     *
     * @param uuid
     *            UUID of the Neutron object
     * @return Neutron object
     */

    public N get(String uuid);

    /**
     * Applications call this interface method to return all Neutron objects
     *
     * @return List of Neutron objects
     */

    public List<N> getAll();

    /**
     * Applications call this interface method to add a Neutron object to the
     * concurrent map
     *
     * @param input
     *            Neutron object
     * @return boolean on whether the Neutron object was added or not
     */

    public boolean add(N input);

    /**
     * Applications call this interface method to remove a Neutron object from the
     * concurrent map
     *
     * @param uuid
     *            identifier for the Neutron object
     * @return boolean on whether the Neutron object was removed or not
     */

    public boolean remove(String uuid);

    /**
     * Applications call this interface method to edit a Neutron object
     *
     * @param uuid
     *            identifier of the Neutron object
     * @param delta
     *            Neutron object containing changes to apply
     * @return boolean on whether the Neutron object was updated or not
     */

    public boolean update(String uuid, N delta);
}
