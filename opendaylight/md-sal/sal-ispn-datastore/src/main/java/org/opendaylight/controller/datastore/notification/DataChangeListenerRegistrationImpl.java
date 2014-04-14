package org.opendaylight.controller.datastore.notification;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author: syedbahm
 * Date: 4/11/14
 */
public  class DataChangeListenerRegistrationImpl<T extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>>
    implements
    org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration<T> {

  private final AsyncDataBroker.DataChangeScope scope;
  //private ListenerRegistrationNode node;
  private final InstanceIdentifier path;

  T listener ;

  public DataChangeListenerRegistrationImpl(final InstanceIdentifier path,final T listener, final AsyncDataBroker.DataChangeScope scope
                                        ) {
    this.path = path;
    this.scope = scope;
    this.listener = listener;

  }

  @Override
  public AsyncDataBroker.DataChangeScope getScope() {
    return scope;
  }


  @Override
  public T getInstance() {
    return listener;
  }

  @Override
  public InstanceIdentifier getPath() {
    return path;
  }

  @Override
  public void close() {

  }
}