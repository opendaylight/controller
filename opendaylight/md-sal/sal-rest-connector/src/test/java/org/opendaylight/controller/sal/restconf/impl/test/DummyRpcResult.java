package org.opendaylight.controller.sal.restconf.impl.test;

import java.util.Collection;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class DummyRpcResult implements RpcResult<TransactionStatus> {
    
    private final boolean isSuccessful;
    private final TransactionStatus result;
    private final Collection<RpcError> errors;
    
    public DummyRpcResult() {
        isSuccessful = false;
        result = null;
        errors = null;
    }
    
    private DummyRpcResult(Builder builder) {
        isSuccessful = builder.isSuccessful;
        result = builder.result;
        errors = builder.errors;
    }
    
    public static Builder builder() {
        return new DummyRpcResult.Builder();
    }

    @Override
    public boolean isSuccessful() {
        return isSuccessful;
    }

    @Override
    public TransactionStatus getResult() {
        return result;
    }

    @Override
    public Collection<RpcError> getErrors() {
        return errors;
    }
    
    public static class Builder {
        private boolean isSuccessful;
        private TransactionStatus result;
        private Collection<RpcError> errors;
        
        public Builder isSuccessful(boolean isSuccessful) {
            this.isSuccessful = isSuccessful;
            return this;
        }
        
        public Builder result(TransactionStatus result) {
            this.result = result;
            return this;
        }
        
        public Builder errors(Collection<RpcError> errors) {
            this.errors = errors;
            return this;
        }
        
        public RpcResult<TransactionStatus> build() {
            return new DummyRpcResult(this);
        }
        
    }

}
