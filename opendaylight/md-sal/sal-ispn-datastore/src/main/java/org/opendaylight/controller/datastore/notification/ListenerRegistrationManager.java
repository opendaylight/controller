package org.opendaylight.controller.datastore.notification;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
//import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.Map;

/**
 * @author: syedbahm
 * Date: 4/10/14
 */
public class ListenerRegistrationManager {

   private Map<InstanceIdentifier,RegisterListenerNode> mapRegisteredListeners;

   public ListenerRegistrationManager(){

   }


  public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> DataChangeListenerRegistration<L> register(InstanceIdentifier path, L listener, AsyncDataBroker.DataChangeScope scope) {
      RegisterListenerNode rln = null;
      if ((rln = mapRegisteredListeners.get(path)) == null){
           rln = new RegisterListenerNode(path);
      }

        return  rln.registerDataChangeListener(path,listener,scope);
  }

}
