package org.opendaylight.controller.cluster.example.messages;

import java.util.List;

/**
 * Created by kramesha on 11/18/14.
 */
public class SetNotifiers {
    private List<String> notifierList;

    public SetNotifiers(List<String> notifierList) {
        this.notifierList = notifierList;
    }

    public List<String> getNotifierList() {
        return notifierList;
    }
}
