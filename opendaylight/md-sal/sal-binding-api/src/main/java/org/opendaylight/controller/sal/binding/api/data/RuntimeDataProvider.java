/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.data;

import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider.ProviderFunctionality;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataRoot;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Utility interface which does type capture for BindingAware DataReader. 
 * 
 * @author 
 *
 */
public interface RuntimeDataProvider extends ProviderFunctionality,DataReader<InstanceIdentifier<? extends DataObject>, DataObject> {
    
    

}
