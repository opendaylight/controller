package org.opendaylight.controller.datastore.notification;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.infinispan.NormalizedNodeToTreeCacheCodec;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * @author: syedbahm
 * Date: 4/10/14
 */
public class ListenerRegistrationManager {
  final private ListeningExecutorService asyncNotificationService;
  private TreeMap<String, RegisterListenerNode> registeredListenersMap = new TreeMap<>();
  private static final Logger LOG = LoggerFactory.getLogger(ListenerRegistrationManager.class);

  private ListenerRegistrationManager() {
    asyncNotificationService = null;
  }

  public ListenerRegistrationManager(ListeningExecutorService asyncNotificationService) {
    this.asyncNotificationService = asyncNotificationService;
  }


  public synchronized <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> DataChangeListenerRegistration<L> register(
      final InstanceIdentifier path, L listener, final AsyncDataBroker.DataChangeScope scope, final TreeCache store, final SchemaContext schemaContext) {
    RegisterListenerNode rln = null;
    String instanceIdPath = path.toString();
    if ((rln = registeredListenersMap.get(instanceIdPath)) == null) {
      rln = new RegisterListenerNode(path);
    }

    final DataChangeListenerRegistration<L> reg = rln.registerDataChangeListener(path, listener, scope);

      //here we are holding the reference to the rln node before we return the registered
    registeredListenersMap.put(instanceIdPath, rln);

    //we want to register listener and check that if the path is in cache to send the initial snapshot
    this.asyncNotificationService.submit(new Runnable() {

      @Override
      public void run() {
        Node node = store.getNode(Fqn.fromString(path.toString()));
        if (null != node) {
          //ok we found the node in tree cache -- attempt to send the initial snapshot
          final NormalizedNode<?, ?> normalizedNode = new NormalizedNodeToTreeCacheCodec(schemaContext, store).decode(path, node);
          final Optional<NormalizedNode<?, ?>> currentState = Optional.<NormalizedNode<?, ?>>of(normalizedNode);

          if (currentState.isPresent()) {
            final NormalizedNode<?, ?> data = normalizedNode;

            final DOMImmutableDataChangeEvent event = DOMImmutableDataChangeEvent.builder(scope) //
                .setAfter(data) //
                .addCreated(path, data) //
                .build();
            asyncNotificationService.submit(new ChangeListenerNotifyTask(Collections.singletonList(reg), event));
          }
        } else {
          LOG.debug("No snapshot found for the path {}", path.toString());
        }
      }
    });


    return reg;
  }

  public Map<String, RegisterListenerNode> listeners() {
    return registeredListenersMap;
  }

  public synchronized List<ChangeListenerNotifyTask> prepareNotifyTasks(final WriteDeleteTransactionTracker transactionLog) {
    List<ChangeListenerNotifyTask> notifyTasks = new ArrayList<ChangeListenerNotifyTask>();
    //need to prepare ChangeListenerNotifyTasks

    //ok here we have the registered listeners,transactionLog with original snapshot.
    Set<String> potentialSet = new HashSet<>();

    potentialSet = transactionLog.filterTransactionsPaths(registeredListenersMap.keySet());

    //prepare list of base, first and subtree listeners
    for (String path : potentialSet) {
      RegisterListenerNode rln = registeredListenersMap.get(path);

      Boolean eventScope[] = new Boolean[3];
      eventScope[0]=eventScope[1]=eventScope[2]=false;
      //ok let us get event prepared here
      DOMImmutableDataChangeEvent event = transactionLog.prepareEvent(path,eventScope);
      //BASE level
      prepareNotificationTasksAtLevel(path,rln.getBaseScopeListeners(),eventScope[0], AsyncDataBroker.DataChangeScope.BASE,notifyTasks,event);
      //let us check the first level listeners
      prepareNotificationTasksAtLevel(path,rln.getFirstLevelListeners(),eventScope[1], AsyncDataBroker.DataChangeScope.ONE,notifyTasks,event);
      //let us check the subtree listeners
      prepareNotificationTasksAtLevel(path,rln.getSubTreeLevelListeners(),eventScope[2], AsyncDataBroker.DataChangeScope.SUBTREE,notifyTasks,event);

    }

    return notifyTasks;
  }

  private void  prepareNotificationTasksAtLevel (String path,HashSet<DataChangeListenerRegistration<?>> lSet,
                                                                           boolean eventScope,
                                                                           AsyncDataBroker.DataChangeScope level,
                                                                          List<ChangeListenerNotifyTask> notifyTasks,
                                                                          DOMImmutableDataChangeEvent event){
    if ((lSet != null) && !lSet.isEmpty()) {
      if(eventScope) {
        notifyTasks.add(new ChangeListenerNotifyTask(lSet, event));
      } else {
        LOG.debug("No change found for {} BASE level ", path);
      }
    }else{
      LOG.debug("No listener registered at{} level for BASE notifications",path) ;
    }

  }

  public synchronized ListenableFuture notifyListeners(List<ChangeListenerNotifyTask> notifyTasks) {
    ListenableFuture lastListenableFuture = null;
    if (notifyTasks != null) {
      for (ChangeListenerNotifyTask task : notifyTasks) {
        lastListenableFuture = asyncNotificationService.submit(task);
      }
    }
    return lastListenableFuture;
  }

  public Executor getNotifyExecutor() {
    return asyncNotificationService;
  }
}
