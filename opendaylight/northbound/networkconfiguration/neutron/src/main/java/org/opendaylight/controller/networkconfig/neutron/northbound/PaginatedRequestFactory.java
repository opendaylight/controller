/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.opendaylight.controller.networkconfig.neutron.INeutronObject;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PaginatedRequestFactory {

    public static class PaginationResults<T extends INeutronObject> {
        List<T> collection;
        List<NeutronPageLink> links;

        public PaginationResults(List<T> collection, List<NeutronPageLink> links) {
            this.collection = collection;
            this.links = links;
        }
    }

    public static <T extends INeutronObject> INeutronRequest createRequest(Integer limit, String marker,
                                                                           Boolean pageReverse,
                                                                           UriInfo uriInfo,
                                                                           List<T> collection,
                                                                           Class<T> clazz) {
        PaginationResults results = _paginate(limit, marker, pageReverse, uriInfo, collection);

        if (clazz.equals(NeutronNetwork.class)){
            return new NeutronNetworkRequest(results.collection, results.links);
        }
        if (clazz.equals(NeutronSubnet.class)){
            return new NeutronSubnetRequest(results.collection, results.links);
        }
        if (clazz.equals(NeutronPort.class)){
            return new NeutronPortRequest(results.collection, results.links);
        }
        return null;
    }

    private static <T extends INeutronObject> PaginationResults _paginate(Integer limit, String marker, Boolean pageReverse, UriInfo uriInfo, List<T> collection) {
        List<NeutronPageLink> links = new ArrayList<>();
        Integer startPos = null;
        String startMarker;
        String endMarker;
        Boolean firstPage = false;
        Boolean lastPage = false;

        Comparator<INeutronObject> neutronObjectComparator = new Comparator<INeutronObject>() {
            @Override
            public int compare(INeutronObject o1, INeutronObject o2) {
                return o1.getID().compareTo(o2.getID());
            }
        };

        Collections.sort(collection, neutronObjectComparator);

        if (marker == null) {
            startPos = 0;
        }

        else {

            class MarkerObject implements INeutronObject {
                private String id;

                public String getID() {
                    return id;
                }

                public void setID(String id) {
                    this.id = id;
                }
            }

            INeutronObject markerObject = new MarkerObject();

            markerObject.setID(marker);

            startPos = Collections.binarySearch(collection, markerObject, neutronObjectComparator);

            if (!pageReverse){
                startPos = startPos + 1;
            }
            else {
                startPos = startPos - limit;
            }

        }

        if (startPos == null) {
            throw new ResourceNotFoundException("UUID for marker:" + marker + " could not be found");
        }

        if (startPos == 0){
            firstPage = true;
        }

        if (startPos + limit >= collection.size()) {
            collection = collection.subList(startPos, collection.size());
            startMarker = collection.get(0).getID();
            endMarker = collection.get(collection.size() - 1).getID();
            lastPage = true;
        }
        else if (startPos < 0) {
            if (startPos + limit > 0) {
                collection = collection.subList(0, startPos + limit);
                startMarker = collection.get(0).getID();
                endMarker = collection.get(collection.size() - 1).getID();
                firstPage = true;
            }
            else {
                throw new BadRequestException("Requested page is out of bounds. Please check the supplied limit and marker");
            }
        }
        else {
            collection = collection.subList(startPos, startPos + limit);
            startMarker = collection.get(0).getID();
            endMarker = collection.get(limit-1).getID();
        }

        if (!lastPage) {
            NeutronPageLink next = new NeutronPageLink();
            next.setRef("next");
            next.setHref(uriInfo.getAbsolutePath().toString() + "?limit=" + limit.toString() + "&marker=" + endMarker);
            links.add(next);
        }

        if (!firstPage) {
            NeutronPageLink previous = new NeutronPageLink();
            previous.setRef("previous");
            previous.setHref(uriInfo.getAbsolutePath().toString() + "?limit=" + limit.toString() + "&marker=" + startMarker + "&page_reverse=True");
            links.add(previous);
        }

        return new PaginationResults(collection, links);
    }
}
