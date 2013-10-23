package org.opendaylight.controller.sal.binding.test.mock;

import java.util.concurrent.Future;

import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;

public interface FooService extends RpcService {
    
    Future<RpcResult<Void>> foo();
    
    Future<RpcResult<Void>> simple(SimpleInput obj);
    
    Future<RpcResult<Void>> inheritedContext(InheritedContextInput obj);

}
