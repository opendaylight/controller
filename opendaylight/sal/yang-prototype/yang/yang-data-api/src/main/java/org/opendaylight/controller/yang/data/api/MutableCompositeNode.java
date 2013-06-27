/**
 * 
 */
package org.opendaylight.controller.yang.data.api;

import java.util.List;


/**
 * @author michal.rehak
 *
 */
public interface MutableCompositeNode extends MutableNode<List<Node<?>>>, CompositeNode {
    
    /**
     * update internal map
     */
    public void init();
}
