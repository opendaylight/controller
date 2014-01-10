package org.opendaylight.controller.sal.restconf.impl.test;

import java.util.concurrent.Future;

import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

class DummyMountInstanceImpl implements MountInstance {

    SchemaContext schemaContext;

    public static class Builder {
        SchemaContext schemaContext;

        public Builder setSchemaContext(SchemaContext schemaContext) {
            this.schemaContext = schemaContext;
            return this;
        }

        public MountInstance build() {
            DummyMountInstanceImpl instance = new DummyMountInstanceImpl();
            instance.schemaContext = schemaContext;
            return instance;
        }
    }

    @Override
    public Registration<NotificationListener> addNotificationListener(QName notification, NotificationListener listener) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompositeNode readConfigurationData(InstanceIdentifier path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompositeNode readOperationalData(InstanceIdentifier path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataModificationTransaction beginTransaction() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(InstanceIdentifier path,
            DataChangeListener listener) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SchemaContext getSchemaContext() {
        return schemaContext;
    }

    @Override
    public Future<RpcResult<CompositeNode>> rpc(QName type, CompositeNode input) {
        // TODO Auto-generated method stub
        return null;
    }

}
