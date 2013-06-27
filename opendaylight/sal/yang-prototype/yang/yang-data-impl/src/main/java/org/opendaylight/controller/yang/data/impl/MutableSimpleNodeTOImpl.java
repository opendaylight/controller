/**
 * 
 */
package org.opendaylight.controller.yang.data.impl;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.MutableSimpleNode;

/**
 * @author michal.rehak
 * @param <T> type of simple node value
 * 
 */
public class MutableSimpleNodeTOImpl<T> extends SimpleNodeModificationTOImpl<T> 
        implements MutableSimpleNode<T> {

    /**
     * @param qname
     * @param parent
     * @param value
     * @param modifyAction
     */
    public MutableSimpleNodeTOImpl(QName qname, CompositeNode parent, T value,
            ModifyAction modifyAction) {
        super(qname, parent, value, modifyAction);
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);
    }
    
    @Override
    public void setModifyAction(ModifyAction action) {
        super.setModificationAction(action);
    }
}
