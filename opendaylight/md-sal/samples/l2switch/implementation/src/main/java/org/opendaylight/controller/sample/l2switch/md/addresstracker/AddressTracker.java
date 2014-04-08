/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.l2switch.md.addresstracker;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.address.tracker.rev140402.L2Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.address.tracker.rev140402.l2.addresses.L2Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.address.tracker.rev140402.l2.addresses.L2AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.address.tracker.rev140402.l2.addresses.L2AddressKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

/**
 * AddressTracker manages the MD-SAL data tree for L2Address (mac, node connector pairings) information.
 */
public class AddressTracker {

  private final static Logger _logger = LoggerFactory.getLogger(AddressTracker.class);
  private DataBrokerService dataService;

  /**
   * Construct an AddressTracker with the specified inputs
   * @param dataService  The DataBrokerService for the AddressTracker
   */
  public AddressTracker(DataBrokerService dataService) {
    this.dataService = dataService;
  }

  /**
   * Get all the L2 Addresses in the MD-SAL data tree
   * @return    All the L2 Addresses in the MD-SAL data tree
   */
  public L2Addresses getAddresses() {
    return (L2Addresses)dataService.readOperationalData(InstanceIdentifier.<L2Addresses>builder(L2Addresses.class).toInstance());
  }

  /**
   * Get a specific L2 Address in the MD-SAL data tree
   * @param macAddress  A MacAddress associated with an L2 Address object
   * @return    The L2 Address corresponding to the specified macAddress
   */
  public L2Address getAddress(MacAddress macAddress) {
    return (L2Address) dataService.readOperationalData(createPath(macAddress));
  }

  /**
   * Add L2 Address into the MD-SAL data tree
   * @param macAddress  The MacAddress of the new L2Address object
   * @param nodeConnectorRef  The NodeConnectorRef of the new L2Address object
   * @return  Future containing the result of the add operation
   */
  public Future<RpcResult<TransactionStatus>> addAddress(MacAddress macAddress, NodeConnectorRef nodeConnectorRef) {
    if(macAddress == null || nodeConnectorRef == null) {
      return null;
    }

    // Create L2Address
    final L2AddressBuilder builder = new L2AddressBuilder();
    builder.setKey(new L2AddressKey(macAddress))
            .setMac(macAddress)
            .setNodeConnectorRef(nodeConnectorRef);

    // Add L2Address to MD-SAL data tree
    final DataModificationTransaction it = dataService.beginTransaction();
    it.putOperationalData(createPath(macAddress), builder.build());
    return it.commit();
  }

  /**
   * Remove L2Address from the MD-SAL data tree
   * @param macAddress  The MacAddress of an L2Address object
   * @return  Future containing the result of the remove operation
   */
  public Future<RpcResult<TransactionStatus>> removeHost(MacAddress macAddress) {
    final DataModificationTransaction it = dataService.beginTransaction();
    it.removeOperationalData(createPath(macAddress));
    return it.commit();
  }

  /**
   * Create InstanceIdentifier path for an L2Address in the MD-SAL data tree
   * @param macAddress  The MacAddress of an L2Address object
   * @return  InstanceIdentifier of the L2Address corresponding to the specified macAddress
   */
  private InstanceIdentifier<L2Address> createPath(MacAddress macAddress) {
    return InstanceIdentifier.<L2Addresses>builder(L2Addresses.class)
            .<L2Address, L2AddressKey>child(L2Address.class, new L2AddressKey(macAddress)).toInstance();
  }
}