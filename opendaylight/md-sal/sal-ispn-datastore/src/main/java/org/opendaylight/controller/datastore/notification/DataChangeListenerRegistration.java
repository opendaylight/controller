package org.opendaylight.controller.datastore.notification;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author: syedbahm
 * Date: 4/11/14
 */
public class DataChangeListenerRegistration<T extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>>
    implements ListenerRegistration<T> {

  private final AsyncDataBroker.DataChangeScope scope;
  ;
  private final InstanceIdentifier path;

  T listener;

  public DataChangeListenerRegistration(final InstanceIdentifier path, final T listener, final AsyncDataBroker.DataChangeScope scope
  ) {
    this.path = path;
    this.scope = scope;
    this.listener = listener;

  }


  public AsyncDataBroker.DataChangeScope getScope() {
    return scope;
  }



  public T getInstance() {
    return listener;
  }

  public InstanceIdentifier getPath() {
    return path;
  }

  @Override
  public void close() {

  }
}