package org.opendaylight.controller.sal.restconf.impl

import org.opendaylight.controller.md.sal.common.api.data.DataReader
import java.net.URI
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.controller.sal.core.api.data.DataBrokerService
import org.opendaylight.controller.sal.core.api.model.SchemaService
import static com.google.common.base.Preconditions.*;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.controller.md.sal.common.api.data.DataModificationTransactionFactory

class BrokerFacade implements DataReader<String, CompositeNode> {

    @Property
    private ConsumerSession context;
    
    @Property
    private DataBrokerService dataService;
    
    @Property
    private SchemaService schemaService;

    @Property
    private extension ControllerContext schemaContext;


    def void init() {
        checkState(dataService !== null)
        checkState(schemaService !== null)
        schemaContext = new ControllerContext();
        schemaContext.schemas = schemaService.globalContext;
    }

    override readConfigurationData(String path) {
        val processedPath = path.removePrefixes();
        return dataService.readConfigurationData(processedPath.toInstanceIdentifier);
    }

    override readOperationalData(String path) {
        val processedPath = path.removePrefixes();
        return dataService.readOperationalData(processedPath.toInstanceIdentifier);
    }
    
    def RpcResult<CompositeNode> invokeRpc(String type,CompositeNode payload) {
        val future = context.rpc(type.toRpcQName(),payload);
        return future.get;
    }
    
    def commitConfigurationDataUpdate(String path, CompositeNode payload) {
        val transaction = dataService.beginTransaction;
        transaction.putConfigurationData(path.toInstanceIdentifier,payload);
        return transaction.commit()
    }
    
    def commitConfigurationDataCreate(String path, CompositeNode payload) {
        val transaction = dataService.beginTransaction;
        transaction.putConfigurationData(path.toInstanceIdentifier,payload);
        return transaction.commit()
    }
    
    private def String removePrefixes(String path) {
        return path;
    }
}
