/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import org.opendaylight.controller.config.yang.md.sal.binding.impl.Data;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.DataBrokerImplRuntimeMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.DataBrokerImplRuntimeRegistration;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.DataBrokerImplRuntimeRegistrator;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.Transactions;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentConnector;

public class RootDataBrokerImpl extends DataBrokerImpl implements DataBrokerImplRuntimeMXBean {

    private final Transactions transactions = new Transactions();
    private final Data data = new Data();
    private BindingIndependentConnector bindingIndependentConnector;
    private DataBrokerImplRuntimeRegistration runtimeBeanRegistration;

    public BindingIndependentConnector getBindingIndependentConnector() {
        return bindingIndependentConnector;
    }

    public Transactions getTransactions() {
        transactions.setCreated(getCreatedTransactionsCount().get());
        transactions.setSubmitted(getSubmittedTransactionsCount().get());
        transactions.setSuccessful(getFinishedTransactionsCount().get());
        transactions.setFailed(getFailedTransactionsCount().get());
        return transactions;
    }

    @Override
    public Data getData() {
        data.setTransactions(getTransactions());
        return data;
    }

    public void setBindingIndependentConnector(BindingIndependentConnector runtimeMapping) {
        this.bindingIndependentConnector = runtimeMapping;
    }

    public void registerRuntimeBean(DataBrokerImplRuntimeRegistrator rootRegistrator) {
        runtimeBeanRegistration = rootRegistrator.register(this);
    }

}
