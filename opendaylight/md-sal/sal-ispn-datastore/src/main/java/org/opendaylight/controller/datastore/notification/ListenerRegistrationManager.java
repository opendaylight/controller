package org.opendaylight.controller.datastore.notification;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;

/**
 * @author: syedbahm
 * Date: 4/10/14
 */
public class ListenerRegistrationManager {
   final private ListeningExecutorService asyncNotificationService;
   private Map<InstanceIdentifier,RegisterListenerNode> registeredListenersMap = new ConcurrentHashMap<>();
   private ListenerRegistrationManager(){
       asyncNotificationService = null;
   }
   public ListenerRegistrationManager(ListeningExecutorService asyncNotificationService){
     this.asyncNotificationService = asyncNotificationService;
   }


  public synchronized <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> DataChangeListenerRegistration<L> register(InstanceIdentifier path, L listener, AsyncDataBroker.DataChangeScope scope) {
      RegisterListenerNode rln = null;
      if ((rln = registeredListenersMap.get(path)) == null){
           rln = new RegisterListenerNode(path);
      }
      DataChangeListenerRegistration<L>returnable= rln.registerDataChangeListener(path,listener,scope);

      //here we are holding the reference to the rln node before we return the registered
      registeredListenersMap.put(path,rln);

      return  returnable;
  }

  public Map<InstanceIdentifier,RegisterListenerNode> listeners(){
      return registeredListenersMap;
  }

  public List<ChangeListenerNotifyTask> prepareNotifyTasks(final WriteDeleteTransactionTracker transactionLog){
    //need to prepare ChangeListenerNotifyTasks
    if(transactionLog.isLocked()){
      //ok here we have the registered listeners,transactionLog with original snapshot.
      synchronized(registeredListenersMap){
        Set<InstanceIdentifier> potential = new HashSet<>();
        transactionLog.preparePotentialTasks(registeredListenersMap.keySet());
      }
    }
    //work in progress..
     return null;
  }

  public synchronized ListenableFuture notifyListeners(List<ChangeListenerNotifyTask> notifyTasks){
     ListenableFuture lastListenableFuture = null;
     if(notifyTasks!=null){
       for(ChangeListenerNotifyTask task:notifyTasks){
            lastListenableFuture = asyncNotificationService.submit(task);
       }
     }
     return lastListenableFuture;
  }

}
