package org.opendaylight.controller.config.yang.md.sal.binding.statistics;

import org.opendaylight.controller.config.yang.md.sal.binding.impl.DataBrokerImplRuntimeMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.Transactions;
import org.opendaylight.controller.sal.binding.impl.DataBrokerImpl;

public class DataBrokerRuntimeMXBeanImpl extends DataBrokerImpl implements DataBrokerImplRuntimeMXBean {
    
    private Transactions transactions = new Transactions();
    
    @Override
    public Transactions getTransactions() {
        transactions.setCreated(getCreatedTransactionsCount().get());
        transactions.setSubmitted(getSubmittedTransactionsCount().get());
        transactions.setSuccessful(getFinishedTransactionsCount().get());
        transactions.setFailed(getFailedTransactionsCount().get());
        return transactions;
    }
}
