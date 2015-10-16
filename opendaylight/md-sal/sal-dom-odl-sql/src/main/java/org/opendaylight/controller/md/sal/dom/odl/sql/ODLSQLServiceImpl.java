/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.odl.sql;

import java.sql.SQLException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.dom.odl.sql.jdbc.JDBCResultSet;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.odl.sql.rev151016.ExecuteQueryInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.odl.sql.rev151016.ExecuteQueryOutput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.odl.sql.rev151016.ExecuteQueryOutputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.odl.sql.rev151016.OdlSqlService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class ODLSQLServiceImpl implements OdlSqlService{

    private final ODLSQLAdapter adapter;

    public ODLSQLServiceImpl(SchemaService schemaService){
        this.adapter = new ODLSQLAdapter();
        schemaService.registerSchemaContextListener(this.adapter);
    }

    public ODLSQLAdapter getAdapter() {
        return this.adapter;
    }

    @Override
    public Future<RpcResult<ExecuteQueryOutput>> executeQuery(ExecuteQueryInput input) {
        ExecuteQueryOutputBuilder output = new ExecuteQueryOutputBuilder();
        output.setResult(executeSQL(input.getOdlsql()));
        RpcResultBuilder<ExecuteQueryOutput> rpcResult = RpcResultBuilder.success(output);
        return rpcResult.buildFuture();
    }

    public String executeSQL(String sql){
        try {
            JDBCResultSet rs = (JDBCResultSet)this.adapter.executeQuery(sql);
            StringBuilder sb = new StringBuilder();
            char qt = '"';
            while(rs.next()){
                sb.append("{");
                for (Column c : rs.getFields()) {
                    sb.append(qt).append(c.toString()).append(qt).append(" : ").append(qt).append(rs.getObject(c.toString())).append(qt);
                }
                sb.append("}");
            }
            return sb.toString();
        } catch (SQLException e) {
            ODLSQLAdapter.log(e);
        }
        return "Error Occured";
    }
}
