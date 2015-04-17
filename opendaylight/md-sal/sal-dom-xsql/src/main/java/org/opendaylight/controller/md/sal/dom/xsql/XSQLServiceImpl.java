package org.opendaylight.controller.md.sal.dom.xsql;

import java.sql.SQLException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.dom.xsql.jdbc.JDBCResultSet;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.ExecuteQueryInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.ExecuteQueryOutput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.ExecuteQueryOutputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.XSQLService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class XSQLServiceImpl implements XSQLService{

    @Override
    public Future<RpcResult<ExecuteQueryOutput>> executeQuery(ExecuteQueryInput input) {
        ExecuteQueryOutputBuilder output = new ExecuteQueryOutputBuilder();
        output.setResult(executeSQL(input.getSql()));
        RpcResultBuilder<ExecuteQueryOutput> rpcResult = RpcResultBuilder.success(output);
        return rpcResult.buildFuture();
    }
    public String executeSQL(String sql){
        try {
            JDBCResultSet rs = (JDBCResultSet)XSQLAdapter.getInstance().executeQuery(sql);
            StringBuilder sb = new StringBuilder();
            char qt = '"';
            while(rs.next()){
                sb.append("{");
                for (XSQLColumn c : rs.getFields()) {
                    sb.append(qt).append(c.toString()).append(qt).append(" : ").append(qt).append(rs.getObject(c.toString())).append(qt);
                }
                sb.append("}");
            }
            return sb.toString();
        } catch (SQLException e) {
            XSQLAdapter.log(e);
        }
        return "Error Occured";
    }
}
