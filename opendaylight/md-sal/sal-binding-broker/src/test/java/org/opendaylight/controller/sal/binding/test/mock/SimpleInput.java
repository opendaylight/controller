package org.opendaylight.controller.sal.binding.test.mock;

import org.opendaylight.yangtools.yang.binding.Augmentable;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.annotations.RoutingContext;

public interface SimpleInput extends DataObject,Augmentable<SimpleInput> {

    @RoutingContext(BaseIdentity.class)
    InstanceIdentifier getIdentifier();
}
