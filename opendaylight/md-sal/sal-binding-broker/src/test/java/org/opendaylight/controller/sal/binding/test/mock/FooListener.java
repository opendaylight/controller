package org.opendaylight.controller.sal.binding.test.mock;

import org.opendaylight.yangtools.yang.binding.NotificationListener;

public interface FooListener extends NotificationListener {

    void onFooUpdate(FooUpdate notification);
    
}
