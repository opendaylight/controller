package org.opendaylight.controller.networkconfig.neutron.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class NeutronPortSecurityGroupDiff {

    private final Iterable<String> joining;
    private final Iterable<String> leaving;
    private final Iterable<String> remaining;

    public NeutronPortSecurityGroupDiff(List<String> current, List<String> update) {
        if (current == null)
            current = Collections.emptyList();
        if (update == null)
            update = Collections.emptyList();

        HashSet<String> joining = new HashSet<>(update);
        ArrayList<String> leaving = new ArrayList<>();
        ArrayList<String> remaining = new ArrayList<>();

        for (String uuid : current) {
            if (joining.remove(uuid))
                remaining.add(uuid);
            else
                leaving.add(uuid);
        }

        this.joining = joining;
        this.leaving = leaving;
        this.remaining = remaining;
    }

    public Iterable<String> getJoining() {
        return joining;
    }

    public Iterable<String> getLeaving() {
        return leaving;
    }

    public Iterable<String> getRemaining() {
        return remaining;
    }
}
