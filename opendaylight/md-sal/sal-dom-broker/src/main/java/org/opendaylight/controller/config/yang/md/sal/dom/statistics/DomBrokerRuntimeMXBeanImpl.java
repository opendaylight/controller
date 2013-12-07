package org.opendaylight.controller.config.yang.md.sal.dom.statistics;

import org.opendaylight.controller.config.yang.md.sal.dom.impl.Data;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplRuntimeMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.Transactions;
import org.opendaylight.controller.sal.dom.broker.DataBrokerImpl;

public class DomBrokerRuntimeMXBeanImpl implements
        DomBrokerImplRuntimeMXBean {
    
    private final DataBrokerImpl dataService;
    private final Transactions transactions = new Transactions();
    private final Data data = new Data();
    
    public DomBrokerRuntimeMXBeanImpl(DataBrokerImpl dataService) {
        this.dataService = dataService; 
    }

    public Transactions getTransactions() {
        transactions.setCreated(dataService.getCreatedTransactionsCount().get());
        transactions.setSubmitted(dataService.getSubmittedTransactionsCount().get());
        transactions.setSuccessful(dataService.getFinishedTransactionsCount().get());
        transactions.setFailed(dataService.getFailedTransactionsCount().get());
        return transactions;
    }

    @Override
    public Data getData() {
        transactions.setCreated(dataService.getCreatedTransactionsCount().get());
        transactions.setSubmitted(dataService.getSubmittedTransactionsCount().get());
        transactions.setSuccessful(dataService.getFinishedTransactionsCount().get());
        transactions.setFailed(dataService.getFailedTransactionsCount().get());
        data.setTransactions(transactions);
        return data;
    }
}
