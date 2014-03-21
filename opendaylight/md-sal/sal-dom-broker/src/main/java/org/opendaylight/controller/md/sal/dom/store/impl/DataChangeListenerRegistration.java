package org.opendaylight.controller.md.sal.dom.store.impl;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface DataChangeListenerRegistration<L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>>
extends ListenerRegistration<L> {


    @Override
    public L getInstance();

    InstanceIdentifier getPath();

    DataChangeScope getScope();



}
