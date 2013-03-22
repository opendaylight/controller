/**
 *
 */
package org.openflow.example;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 *
 */
public interface SelectListener {
    /**
     * Tell the select listener that an event took place on the passed object
     * @param key the key used on the select
     * @param arg some parameter passed by the caller when registering
     * @throws IOException
     */
    void handleEvent(SelectionKey key, Object arg) throws IOException;
}
