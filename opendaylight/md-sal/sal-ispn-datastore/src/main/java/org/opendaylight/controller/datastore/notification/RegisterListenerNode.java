package org.opendaylight.controller.datastore.notification;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;

public class RegisterListenerNode {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterListenerNode.class);

  private final InstanceIdentifier identifier;


  private final HashSet<org.opendaylight.controller.datastore.notification.DataChangeListenerRegistration<?>> baseScopeListeners;
  private final HashSet<org.opendaylight.controller.datastore.notification.DataChangeListenerRegistration<?>> firstLevelListeners;
  private final HashSet<org.opendaylight.controller.datastore.notification.DataChangeListenerRegistration<?>> subTreeLevelListeners;


  RegisterListenerNode(final InstanceIdentifier identifier) {
    this.identifier = identifier;
    this.baseScopeListeners = new HashSet<>();
    this.firstLevelListeners = new HashSet<>();
    this.subTreeLevelListeners = new HashSet<>();
  }


  public Collection<DataChangeListenerRegistration<?>> getListeners() {
    HashSet<org.opendaylight.controller.datastore.notification.DataChangeListenerRegistration<?>> listeners = new HashSet<>();
    listeners.addAll(baseScopeListeners);
    listeners.addAll(firstLevelListeners);
    listeners.addAll(subTreeLevelListeners);
    return (Collection) listeners;
  }


  /**
   * Registers listener on this node.
   *
   * @param path     Full path on which listener is registered.
   * @param listener Listener
   * @param scope    Scope of triggering event.
   * @return
   */
  public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> DataChangeListenerRegistration<L> registerDataChangeListener(final InstanceIdentifier path,
                                                                                                                                                    final L listener, final DataChangeScope scope) {

    DataChangeListenerRegistration<L> listenerReg = new DataChangeListenerRegistration<L>(path, listener, scope, this);
    if (scope == DataChangeScope.BASE) {
      baseScopeListeners.add(listenerReg);
    } else if (scope == DataChangeScope.ONE) {
      firstLevelListeners.add(listenerReg);
    } else {
      subTreeLevelListeners.add(listenerReg);
    }
    ;
    return listenerReg;
  }

  private void removeListener(final DataChangeListenerRegistration<?> listener) {

    baseScopeListeners.remove(listener);
    firstLevelListeners.remove(listener);
    subTreeLevelListeners.remove(listener);

  }

  public HashSet<org.opendaylight.controller.datastore.notification.DataChangeListenerRegistration<?>> getBaseScopeListeners() {
    return baseScopeListeners;
  }

  public HashSet<org.opendaylight.controller.datastore.notification.DataChangeListenerRegistration<?>> getFirstLevelListeners() {
    return firstLevelListeners;
  }

  public HashSet<org.opendaylight.controller.datastore.notification.DataChangeListenerRegistration<?>> getSubTreeLevelListeners() {
    return subTreeLevelListeners;
  }

    public interface DataChangeListenerRegistrationFoo<L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>>
    extends ListenerRegistration<L> {


        @Override
        public L getInstance();

        InstanceIdentifier getPath();

        DataChangeScope getScope();



    }


  public static class DataChangeListenerRegistration<T extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>>
      extends org.opendaylight.controller.datastore.notification.DataChangeListenerRegistration {

      private RegisterListenerNode node;

      public DataChangeListenerRegistration(final InstanceIdentifier path, final T listener, final DataChangeScope scope,
                                          final RegisterListenerNode node) {
      super(path, listener, scope);
      this.node = node;
    }

    protected void removeRegistration() {
      node.removeListener(this);
      node = null;
    }
  }
}
