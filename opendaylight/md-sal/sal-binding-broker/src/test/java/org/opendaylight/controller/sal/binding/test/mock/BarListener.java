package org.opendaylight.controller.sal.binding.test.mock;

import org.opendaylight.yangtools.yang.binding.NotificationListener;

public interface BarListener extends NotificationListener {

    void onBarUpdate(BarUpdate notification);
    
    void onFlowDelete(FlowDelete notification);

}
