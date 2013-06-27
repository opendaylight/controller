/**
 * 
 */
package org.opendaylight.controller.yang.data.impl;

import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.NodeModification;

/**
 * @author michal.rehak
 * 
 */
public class CompositeNodeModificationTOImpl extends CompositeNodeTOImpl
        implements NodeModification {

    private ModifyAction modifyAction;

    /**
     * @param qname
     * @param parent
     * @param value
     * @param modifyAction
     */
    public CompositeNodeModificationTOImpl(QName qname, CompositeNode parent,
            List<Node<?>> value, ModifyAction modifyAction) {
        super(qname, parent, value);
        this.modifyAction = modifyAction;
    }

    /**
     * @return modification action
     * @see org.opendaylight.controller.yang.data.impl.NodeModificationSupport#getModificationAction()
     */
    @Override
    public ModifyAction getModificationAction() {
        return modifyAction;
    }

    /**
     * @param modifyAction
     *            the modifyAction to set
     */
    protected void setModificationAction(ModifyAction modifyAction) {
        this.modifyAction = modifyAction;
    }

}
