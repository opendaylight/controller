package org.opendaylight.controller.sal.core.api;

import java.util.EventListener;

import org.opendaylight.yangtools.yang.common.QName;

public interface RpcRegistrationListener extends EventListener {
    
    public void onRpcImplementationAdded(QName name);
    
    public void onRpcImplementationRemoved(QName name);
}
