/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;

/**
 * Main responsibility of the class is to manage multipart response 
 * for multipart request. It also handles the flow aggregate request
 * and response mapping. 
 * @author avishnoi@in.ibm.com
 *
 */
public class MultipartMessageManager {

    private static Map<TransactionId,Short> txIdTotableIdMap = new ConcurrentHashMap<TransactionId,Short>();
    
    public MultipartMessageManager(){}
    
    public Short getTableIdForTxId(TransactionId id){
        
        return txIdTotableIdMap.get(id);
        
    }
    
    public void setTxIdAndTableIdMapEntry(TransactionId id,Short tableId){
        txIdTotableIdMap.put(id, tableId);
    }
}
