
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.util.ArrayList;
import java.util.List;

public final class TierHelper {
    private static TierHelper tierHelper;
    public static final int unknownTierNumber = 0;
    private List<String> tierNames;

    static {
        tierHelper = new TierHelper();
    }

    private TierHelper() {
        tierNames = new ArrayList<String>();
        tierNames.add("Unknown");
        tierNames.add("Access");
        tierNames.add("Distribution");
        tierNames.add("Core");
    }

    public static void setTierName(int tier, String name) {
        if (tier > tierHelper.tierNames.size() - 1) {
            for (int i = tierHelper.tierNames.size() - 1; i < tier; i++) {
                tierHelper.tierNames.add("Unknown");
            }
        }
        tierHelper.tierNames.set(tier, name);
    }

    public static String getTierName(int tier) {
        if ((tier < 0) || (tier > tierHelper.tierNames.size() - 1)) {
            return "Unknown";
        }
        return tierHelper.tierNames.get(tier);
    }

    public static List<String> getTiers() {
        return tierHelper.tierNames;
    }
}
