package org.opendaylight.controller.config.persist.api;

import java.util.SortedSet;

public interface ConfigSnapshotHolder {

    /**
     * Get part of get-config document that contains just
     */
    String getConfigSnapshot();


    /**
     * Get only required capabilities referenced by the snapshot.
     */
    SortedSet<String> getCapabilities();

}
