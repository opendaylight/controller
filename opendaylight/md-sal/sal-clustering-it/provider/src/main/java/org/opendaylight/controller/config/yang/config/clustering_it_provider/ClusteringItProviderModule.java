/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.clustering_it_provider;


import org.opendaylight.controller.clustering.it.listener.PeopleCarListener;
import org.opendaylight.controller.clustering.it.provider.PeopleProvider;
import org.opendaylight.controller.clustering.it.provider.PurchaseCarProvider;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarPurchaseService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.PeopleService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

public class ClusteringItProviderModule extends org.opendaylight.controller.config.yang.config.clustering_it_provider.AbstractClusteringItProviderModule {
    public ClusteringItProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ClusteringItProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.clustering_it_provider.ClusteringItProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
      DataBroker dataBrokerService = getDataBrokerDependency();
      NotificationProviderService notificationProvider = getNotificationServiceDependency();

      // Add routed RPC registration for car purchase
      final PurchaseCarProvider purchaseCar = new PurchaseCarProvider();
      purchaseCar.setNotificationProvider(notificationProvider);

      final BindingAwareBroker.RoutedRpcRegistration<CarPurchaseService> purchaseCarRpc = getRpcRegistryDependency()
          .addRoutedRpcImplementation(CarPurchaseService.class, purchaseCar);

      // Add people provider registration
      final PeopleProvider people = new PeopleProvider();
      people.setDataProvider(dataBrokerService);

      people.setRpcRegistration(purchaseCarRpc);

      final BindingAwareBroker.RpcRegistration<PeopleService> peopleRpcReg = getRpcRegistryDependency()
          .addRpcImplementation(PeopleService.class, people);



      final PeopleCarListener peopleCarListener = new PeopleCarListener();
      peopleCarListener.setDataProvider(dataBrokerService);

      final ListenerRegistration<NotificationListener> listenerReg =
          getNotificationServiceDependency().registerNotificationListener( peopleCarListener );

      // Wrap toaster as AutoCloseable and close registrations to md-sal at
      // close()
      final class AutoCloseableToaster implements AutoCloseable {

        @Override
        public void close() throws Exception {
          peopleRpcReg.close();
          purchaseCarRpc.close();
          people.close();
          purchaseCar.close();
          listenerReg.close();
        }
      }

      AutoCloseable ret = new AutoCloseableToaster();
      return ret;
    }

}
