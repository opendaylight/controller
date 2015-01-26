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

import com.google.common.base.Splitter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class AugmentationIdentifierGenerator {
    private static final Pattern PATTERN = Pattern.compile("AugmentationIdentifier\\Q{\\EchildNames=\\Q[\\E(.*)\\Q]}\\E");
    private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults();

    private final String id;
    private final Matcher matcher;
    private final boolean doesMatch;

    public AugmentationIdentifierGenerator(String id) {
        this.id = id;
        matcher = PATTERN.matcher(this.id);
        doesMatch = matcher.matches();
    }

    public boolean matches() {
        return doesMatch;
    }

    public YangInstanceIdentifier.AugmentationIdentifier getPathArgument() {
        final String childQNames = matcher.group(1);

        final Set<QName> childNames = new HashSet<>();
        for (String name : COMMA_SPLITTER.split(childQNames)) {
            childNames.add(QNameFactory.create(name));
        }

        return new YangInstanceIdentifier.AugmentationIdentifier(childNames);
    }

}
