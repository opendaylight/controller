/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.data;


/**
 * DataProviderService is common access point for {@link org.opendaylight.controller.sal.binding.api.BindingAwareProvider} providers
 * to access data trees described by the YANG model.
 *
 * @deprecated Replaced by {@link org.opendaylight.controller.md.sal.common.api.data.AsyncConfigurationCommitCoordinator} service.
 */
@Deprecated
public interface DataProviderService extends DataBrokerService {

}
