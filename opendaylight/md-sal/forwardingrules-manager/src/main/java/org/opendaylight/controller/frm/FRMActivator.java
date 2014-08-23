/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm;

import org.opendaylight.controller.frm.impl.ForwardingRulesManagerImpl;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forwarding Rules Manager Activator
 *
 * Activator {@link ForwardingRulesManager}.
 * It registers all listeners (DataChangeEvent, ReconcilNotification)
 * in the Session Initialization phase.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 * *
 */
public class FRMActivator extends AbstractBindingAwareProvider {

    private final static Logger LOG = LoggerFactory.getLogger(FRMActivator.class);

    private ForwardingRulesManager  manager;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("FRMActivator initialization.");
        try {
            final DataBroker dataBroker = session.getSALService(DataBroker.class);
            this.manager = new ForwardingRulesManagerImpl(dataBroker, session);
            this.manager.start();
            LOG.info("FRMActivator initialization successfull.");
        }
        catch (Exception e) {
            LOG.error("Unexpected error by FRM initialization!", e);
            this.stopImpl(null);
        }
    }

    @Override
    protected void stopImpl(final BundleContext context) {
        if (manager != null) {
            try {
                manager.close();
            } catch (Exception e) {
                LOG.error("Unexpected error by stopping FRMActivator", e);
            }
            manager = null;
            LOG.info("FRMActivator stopped.");
        }
    }
  }