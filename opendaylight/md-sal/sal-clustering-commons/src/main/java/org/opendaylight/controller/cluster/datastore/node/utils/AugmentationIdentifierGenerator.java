/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AugmentationIdentifierGenerator {
    private final String id;
    private static final Pattern pattern = Pattern.compile("AugmentationIdentifier\\Q{\\EchildNames=\\Q[\\E(.*)\\Q]}\\E");
    private final Matcher matcher;
    private final boolean doesMatch;

    public AugmentationIdentifierGenerator(String id){
        this.id = id;
        matcher = pattern.matcher(this.id);
        doesMatch = matcher.matches();
    }

    public boolean matches(){
        return doesMatch;
    }

    public YangInstanceIdentifier.AugmentationIdentifier getPathArgument(){
        Set<QName> childNames = new HashSet<QName>();
        final String childQNames = matcher.group(1);

        final String[] splitChildQNames = childQNames.split(",");

        for(String name : splitChildQNames){
            childNames.add(
                org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory
                    .create(name.trim()));
        }

        return new YangInstanceIdentifier.AugmentationIdentifier(null, childNames);
    }

}
