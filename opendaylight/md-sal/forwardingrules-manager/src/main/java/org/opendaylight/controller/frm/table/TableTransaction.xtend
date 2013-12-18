package org.opendaylight.controller.frm.table

import org.opendaylight.controller.frm.AbstractTransaction
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

//import org.opendaylight.yang.gen.v1.urn.opendaylight.table.config.rev131024.Tables;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.table.config.rev131024.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.SalTableService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.UpdateTableInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.table.update.UpdatedTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.table.update.OriginalTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.TableFeatures;
import org.slf4j.LoggerFactory

class TableTransaction extends AbstractTransaction {
    
    @Property
    val SalTableService tableService;
    
    static val LOG = LoggerFactory.getLogger(TableTransaction);
    
    new(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification,SalTableService tableService) {
        super(modification)
        _tableService = tableService;
    }
        
     override add(InstanceIdentifier<?> instanceId, DataObject obj) {
    LOG.info( "Table add hit " ) ;
        if(obj instanceof Table) {
            val table = (obj as Table )
            val nodeInstanceId = instanceId.firstIdentifierOf(Node);
            val builder = new UpdateTableInputBuilder();
            builder.setNode(new NodeRef(nodeInstanceId));
            val ufb = new UpdatedTableBuilder(table);
            builder.setUpdatedTable((ufb.build()));
            LOG.info("Calling Table Service " + builder );     
            _tableService.updateTable(builder.build());
        }
    }
    
    override update(InstanceIdentifier<?> instanceId, DataObject originalObj, DataObject updatedObj) {
     LOG.info("Table update Transaction Callback");
        add( instanceId, updatedObj );
    }
    
   override remove(InstanceIdentifier<?> instanceId, DataObject obj) { 
          // NOOP  
      } 
      
    override validate() throws IllegalStateException {
        TableTransactionValidator.validate(this)
    } 
     
 }