package org.opendaylight.controller.md.sal.binding.util;

import java.util.concurrent.Future;
import java.util.zip.Checksum;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.mount.MountInstance;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataRoot;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Preconditions;

public abstract class AbstractBindingSalConsumerInstance<D extends DataBrokerService, N extends NotificationService, R extends RpcConsumerRegistry> //
        implements //
        RpcConsumerRegistry, //
        NotificationService, //
        DataBrokerService {

    private final R rpcRegistry;
    private final N notificationBroker;
    private final D dataBroker;

    protected final R getRpcRegistry() {
        return rpcRegistry;
    }

    protected final N getNotificationBroker() {
        return notificationBroker;
    }

    protected final D getDataBroker() {
        return dataBroker;
    }
    
    protected final R getRpcRegistryChecked() {
        Preconditions.checkState(rpcRegistry != null,"Rpc Registry is not available.");
        return rpcRegistry;
    }

    protected final N getNotificationBrokerChecked() {
        Preconditions.checkState(notificationBroker != null,"Notification Broker is not available.");
        return notificationBroker;
    }

    protected final D getDataBrokerChecked() {
        Preconditions.checkState(dataBroker != null, "Data Broker is not available");
        return dataBroker;
    }
    

    protected AbstractBindingSalConsumerInstance(R rpcRegistry, N notificationBroker, D dataBroker) {
        this.rpcRegistry = rpcRegistry;
        this.notificationBroker = notificationBroker;
        this.dataBroker = dataBroker;
    }

    public <T extends RpcService> T getRpcService(Class<T> module) {
        return getRpcRegistryChecked().getRpcService(module);
    }

    @Deprecated
    public <T extends Notification> void addNotificationListener(Class<T> notificationType,
            NotificationListener<T> listener) {
        getNotificationBrokerChecked().addNotificationListener(notificationType, listener);
    }

    @Deprecated
    public void addNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        getNotificationBrokerChecked().addNotificationListener(listener);
    }

    @Deprecated
    public void removeNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        getNotificationBrokerChecked().removeNotificationListener(listener);
    }

    @Deprecated
    public <T extends Notification> void removeNotificationListener(Class<T> notificationType,
            NotificationListener<T> listener) {
        getNotificationBrokerChecked().removeNotificationListener(notificationType, listener);
    }

    public <T extends Notification> Registration<NotificationListener<T>> registerNotificationListener(
            Class<T> notificationType, NotificationListener<T> listener) {
        return getNotificationBrokerChecked().registerNotificationListener(notificationType, listener);
    }

    public Registration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(
            org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        return getNotificationBrokerChecked().registerNotificationListener(listener);
    }

    @Deprecated
    public <T extends DataRoot> T getData(DataStoreIdentifier store, Class<T> rootType) {
        return getDataBrokerChecked().getData(store, rootType);
    }

    @Deprecated
    public <T extends DataRoot> T getData(DataStoreIdentifier store, T filter) {
        return getDataBrokerChecked().getData(store, filter);
    }

    @Deprecated
    public <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, Class<T> rootType) {
        return getDataBrokerChecked().getCandidateData(store, rootType);
    }

    @Deprecated
    public <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, T filter) {
        return getDataBrokerChecked().getCandidateData(store, filter);
    }

    @Deprecated
    public RpcResult<DataRoot> editCandidateData(DataStoreIdentifier store, DataRoot changeSet) {
        return getDataBrokerChecked().editCandidateData(store, changeSet);
    }

    @Deprecated
    public Future<RpcResult<Void>> commit(DataStoreIdentifier store) {
        return getDataBrokerChecked().commit(store);
    }

    @Deprecated
    public DataObject getData(InstanceIdentifier<? extends DataObject> data) {
        return getDataBrokerChecked().getData(data);
    }

    @Deprecated
    public DataObject getConfigurationData(InstanceIdentifier<?> data) {
        return getDataBrokerChecked().getConfigurationData(data);
    }

    public DataModificationTransaction beginTransaction() {
        return getDataBrokerChecked().beginTransaction();
    }

    @Deprecated
    public void registerChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener changeListener) {
        getDataBrokerChecked().registerChangeListener(path, changeListener);
    }

    @Deprecated
    public void unregisterChangeListener(InstanceIdentifier<? extends DataObject> path,
            DataChangeListener changeListener) {
        getDataBrokerChecked().unregisterChangeListener(path, changeListener);
    }

    @Deprecated
    public DataObject readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        return getDataBrokerChecked().readConfigurationData(path);
    }

    public DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        return getDataBrokerChecked().readOperationalData(path);
    }

    @Deprecated
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(
            InstanceIdentifier<? extends DataObject> path, DataChangeListener listener) {
        return getDataBrokerChecked().registerDataChangeListener(path, listener);
    }
}
