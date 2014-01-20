/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker.tools;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.CreateNotificationStreamInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.CreateNotificationStreamOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.QName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.SalRemoteService;
import org.opendaylight.yangtools.restconf.client.api.RestconfClientContext;
import org.opendaylight.yangtools.restconf.client.api.event.EventStreamInfo;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteStreamTools {
    private static final Logger logger = LoggerFactory.getLogger(RemoteStreamTools.class.toString());

    public static String createNotificationStream(SalRemoteService salRemoteService,List<QName> notifications){
        CreateNotificationStreamInputBuilder notificationStreamInputBuilder = new CreateNotificationStreamInputBuilder();

        if (null == notifications){
            notificationStreamInputBuilder.setNotifications(notifications);
        }

        Future<RpcResult<CreateNotificationStreamOutput>> notificationStream = salRemoteService.createNotificationStream(notificationStreamInputBuilder.build());

        String nofiticationStreamIdentifier = "";
        try {
            if (notificationStream.get().isSuccessful()){
                nofiticationStreamIdentifier  = notificationStream.get().getResult().getNotificationStreamIdentifier();
            }
        } catch (InterruptedException e) {
            logger.trace("Interrupted while resolving notification stream identifier due to {}",e);
        } catch (ExecutionException e) {
            logger.trace("Execution exception while resolving notification stream identifier due to {}",e);
        }
        return nofiticationStreamIdentifier;
    }

    public static Map<String,EventStreamInfo> createEventStream(RestconfClientContext restconfClientContext, String desiredStreamName){
        ListenableFuture<Set<EventStreamInfo>> availableEventStreams = restconfClientContext.getAvailableEventStreams();
        final Map<String,EventStreamInfo> desiredEventStream = new HashMap<String,EventStreamInfo>();

        try {
            Iterator<EventStreamInfo> it = availableEventStreams.get().iterator();
            while (it.hasNext()){
                if (it.next().getIdentifier().equals(desiredStreamName)){
                    desiredEventStream.put(desiredStreamName,it.next());
                }
            }
        } catch (InterruptedException e) {
            logger.trace("Resolving of event stream interrupted due to  {}",e);
        } catch (ExecutionException e) {
            logger.trace("Resolving of event stream failed due to {}",e);
        }
        return desiredEventStream;
    }
}
