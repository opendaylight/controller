/**
 * 
 */
package org.opendaylight.controller.yang.data.impl;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;

/**
 * @author michal.rehak
 * @param <T> type of node value
 * 
 */
public class SimpleNodeModificationTOImpl<T> extends SimpleNodeTOImpl<T> {

    /**
     * @param qname
     * @param parent
     * @param value
     * @param modifyAction 
     */
    public SimpleNodeModificationTOImpl(QName qname, CompositeNode parent,
            T value, ModifyAction modifyAction) {
        super(qname, parent, value);
        setModificationAction(modifyAction);
    }
}
