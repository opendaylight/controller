/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.pingdiscovery.impl;

import java.util.Collections;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sample.pingdiscovery.IcmpProfileManager;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoverInput;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoveryState;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileFinished;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileFinishedBuilder;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileGrp;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileId;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileStarted;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileStartedBuilder;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.Profiles;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfilesBuilder;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.profiles.Profile;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.profiles.ProfileBuilder;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.profiles.ProfileKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages discovery profiles, including their creation, updates and removals.
 * <br><br>DEMONSTRATES: How to read / write data to an operational data store.
 * @author Devin Avery
 * @author Greg Hall
 *
 */
public class IcmpProfileManagerImpl implements IcmpProfileManager {

    private final Logger logger = LoggerFactory.getLogger( IcmpProfileManagerImpl.class );

    private DataProviderService dataBrokerService;
    private NotificationProviderService notificationProviderService;

    public void setDataBrokerService(DataProviderService dataBrokerService) {
        this.dataBrokerService = dataBrokerService;
    }
    public void setNotificationProvider(
            NotificationProviderService notificationServiceDependency) {
        notificationProviderService = notificationServiceDependency;
    }



    /**
     * This method assumes that a discovery is being started and sets status to
     * running. To update status use the updateStatus method.
     *
     * @see org.opendaylight.controller.sample.pingdiscovery.IcmpProfileManager#
     * createOrUpdateProfile
     * (org.opendaylight.yang.gen.v1.http.opendaylight.org.samples
     * .icmp.rev140515.DiscoverInput)
     */
    @Override
    public final ProfileGrp startDiscoveryProfile( final DiscoverInput profileInput ) {

        InstanceIdentifier<Profile> profileInstanceId = createPath(profileInput.getId()) ;

        //  if the profile exists, overlay the input over the existing, replacing each value.
        ProfileGrp existingProfGrp = (ProfileGrp) dataBrokerService.readConfigurationData(profileInstanceId) ;

        // existing ip list must be removed if input
        if ( existingProfGrp != null && ( existingProfGrp.getIpList().size() > 0 ) && ( profileInput.getIpList().size() > 0 ) ) {
            removeExistingProfile(profileInput.getId(), existingProfGrp);
        }

        DataModificationTransaction putTxn = dataBrokerService.beginTransaction();

        Profiles profiles = overlayInputOnExisting(profileInput, existingProfGrp );

        if ( commitUpdatedProfile(putTxn, profiles) != true ) {
            logger.debug("The commit failed on a discovery profile which may have caused the pre-existing profile to be deleted.");
            return null ;
        } else {

            // read the profilegroup to return the current merged state.
            ProfileGrp mergedProfileGrp = (ProfileGrp) dataBrokerService.readConfigurationData(profileInstanceId) ;

            ProfileStarted notify = new ProfileStartedBuilder(mergedProfileGrp)
            .build();

            notificationProviderService.publish(notify);

            return profiles.getProfile().get(0) ;
        }
    }

    /**
     * This method simply updates the status of the profile to not discovering.
     *
     * @see org.opendaylight.controller.sample.pingdiscovery.IcmpProfileManager#
     * finishDiscoveryProfile
     * (org.opendaylight.yang.gen.v1.http.opendaylight.org.
     * samples.icmp.rev140515.ProfileId)
     */
    @Override
    public boolean finishDiscoveryProfile(ProfileGrp profGrp) {
        boolean success = false ;
        DataModificationTransaction txn = dataBrokerService.beginTransaction();

        ProfileBuilder profBldr = new ProfileBuilder(profGrp)
        .setStatus(new DiscoveryState((short) 0));

        Profile profile = profBldr.build();
        Profiles profiles = new ProfilesBuilder().setProfile(
                Collections.singletonList(profile)).build();

        if ( commitUpdatedProfile(txn, profiles) == true ) {

            ProfileFinished notify = new ProfileFinishedBuilder(profGrp).build();
            notificationProviderService.publish(notify);
            success = true ;

        } else {
            logger.error("The commit failed updating the state of a discovery"
                    + " profile, which may leave it in the running state when"
                    + " operations have in fact ceased.");
        }
        return success ;

    }

    /**
     * @param putTxn
     * @param profiles
     * @return TODO
     */
    private boolean commitUpdatedProfile(DataModificationTransaction putTxn, Profiles profiles) {
        boolean success = false ;
        InstanceIdentifier<Profiles> path = InstanceIdentifier.builder(Profiles.class).build();
        putTxn.putConfigurationData(path, profiles);

        try {
            Future<RpcResult<TransactionStatus>> commit = putTxn.commit();
            RpcResult<TransactionStatus> rpcResult = commit.get();
            TransactionStatus result = rpcResult.getResult();
            success = true ;
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Caught exception while trying to commit "
                        + "update to IP Device discovery.", e);
            }
        }
        return success ;
    }

    /**
     * @param profileId
     * @param existingProfGrp
     */
    private void removeExistingProfile(ProfileId profileId, ProfileGrp existingProfGrp) {
        DataModificationTransaction removeTxn = dataBrokerService.beginTransaction();
        removeTxn.removeConfigurationData(createPath(profileId));

        try {
            Future<RpcResult<TransactionStatus>> commit = removeTxn.commit();
            RpcResult<TransactionStatus> rpcResult = commit.get();
        } catch (Exception e) {
            if (logger.isErrorEnabled()) { logger.error("Caught exception while trying to commit " + "remove existing profile.", existingProfGrp, e);
            }
        }
    }

    /**
     * Given an optionally pre-existing profile and partial input from the RPC, overlay
     * the values from the input on the pre-existing and return the result as a new
     * Profiles object.
     * 
     * When the existing is null, this method has the effect of simply creating a Profiles
     * from the RPC input.
     * 
     * @param profileInput
     * @param existingProfGrp
     * @param profBldr
     */
    private Profiles overlayInputOnExisting(DiscoverInput profileInput, ProfileGrp existingProfGrp) {

        if ( profileInput == null ) {
            return null ;
        }

        ProfileBuilder profBldr = new ProfileBuilder().setId(profileInput.getId()).setKey(new ProfileKey(profileInput.getId()) ) ;

        if ( profileInput.getIpList().size() == 0 ) {
            if ( existingProfGrp != null ) {
                // copy the iplist from current profile state
                profBldr.setIpList( existingProfGrp.getIpList() ) ;
            } else {
                System.out.println("The discover rpc didn't specify an ip list");
            }
        } else {
            profBldr.setIpList( profileInput.getIpList()) ;
        }
        if ( profileInput.getTimeoutSeconds() == null ) {
            if ( existingProfGrp != null ) {
                // copy the iplist from current profile state
                profBldr.setTimeoutSeconds( existingProfGrp.getTimeoutSeconds() ) ;
            } else {
                System.out.println("The discover rpc didn't specify an Timout, defaulting to 1");
                profBldr.setTimeoutSeconds(1) ;
            }
        }else {
            profBldr.setTimeoutSeconds( profileInput.getTimeoutSeconds() ) ;
        }

        profBldr.setStatus(new DiscoveryState((short) 1));

        Profile profile = profBldr.build();
        Profiles profiles = new ProfilesBuilder().setProfile(Collections.singletonList(profile)).build();
        return profiles;
    }

    public InstanceIdentifier<Profile> createPath(ProfileId profId) {
        return InstanceIdentifier.<Profiles>builder(Profiles.class)
                .<Profile, ProfileKey>child(Profile.class, new ProfileKey(profId)).toInstance();
    }
}
