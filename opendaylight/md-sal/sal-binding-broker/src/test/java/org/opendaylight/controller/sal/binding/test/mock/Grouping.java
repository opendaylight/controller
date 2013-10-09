package org.opendaylight.controller.sal.binding.test.mock;

import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.annotations.RoutingContext;

public interface Grouping {

    @RoutingContext(BaseIdentity.class)
    InstanceIdentifier<?> getInheritedIdentifier();
}
