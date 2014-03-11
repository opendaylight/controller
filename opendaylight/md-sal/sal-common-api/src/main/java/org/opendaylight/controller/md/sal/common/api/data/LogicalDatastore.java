package org.opendaylight.controller.md.sal.common.api.data;

public enum LogicalDatastore {

    /**
     * Logical Datastore representing operational state of the system
     * and it's components
     *
     * This datastore is used to describe operational state of
     * the system and it's operation related data.
     *
     */
    OPERATIONAL,
    /**
     * Logical Datastore representing configuration state of the system
     * and it's components.
     *
     * This datastore is used to describe intended state of
     * the system and intended operation mode.
     *
     */
    CONFIGURATION

}
