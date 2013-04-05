/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

/**
 * Trigger for refreshing of the data exposed by the {@link Provider}
 * 
 * 
 * 
 */
public interface DataRefresher extends
        BindingAwareProvider.ProviderFunctionality {

    /**
     * Fired when some component explicitly requested the data refresh.
     * 
     * The provider which exposed the {@link DataRefresher} should republish its
     * provided data by editing the data in all affected data stores.
     */
    void refreshData();
}