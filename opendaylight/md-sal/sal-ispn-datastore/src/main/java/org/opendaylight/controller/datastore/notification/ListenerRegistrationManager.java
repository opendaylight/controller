package org.opendaylight.controller.datastore.notification;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.*;

/**
 * @author: syedbahm
 * Date: 4/10/14
 */
public class ListenerRegistrationManager {
   final private ListeningExecutorService asyncNotificationService;
   private TreeMap<String,RegisterListenerNode> registeredListenersMap = new TreeMap<>();
   private ListenerRegistrationManager(){
       asyncNotificationService = null;
   }
   public ListenerRegistrationManager(ListeningExecutorService asyncNotificationService){
     this.asyncNotificationService = asyncNotificationService;
   }


  public synchronized <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> DataChangeListenerRegistration<L> register(InstanceIdentifier path, L listener, AsyncDataBroker.DataChangeScope scope) {
      RegisterListenerNode rln = null;
      String instanceIdPath  = path.toString();
      if ((rln = registeredListenersMap.get(instanceIdPath)) == null){
           rln = new RegisterListenerNode(path);
      }
      DataChangeListenerRegistration<L>returnable= rln.registerDataChangeListener(path,listener,scope);

      //here we are holding the reference to the rln node before we return the registered
      registeredListenersMap.put(instanceIdPath,rln);

      return  returnable;
  }

  public Map<String,RegisterListenerNode> listeners(){
      return registeredListenersMap;
  }

  public List<ChangeListenerNotifyTask> prepareNotifyTasks(final WriteDeleteTransactionTracker transactionLog){
    //need to prepare ChangeListenerNotifyTasks
    if(transactionLog.isLocked()){
      //ok here we have the registered listeners,transactionLog with original snapshot.
      synchronized(registeredListenersMap){
        Set<String> potential = new HashSet<>();

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
