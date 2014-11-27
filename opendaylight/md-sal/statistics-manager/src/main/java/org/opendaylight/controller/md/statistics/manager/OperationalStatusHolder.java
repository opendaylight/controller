package org.opendaylight.controller.md.statistics.manager;


import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.statistics.manager.rev140925.OperStatus;

/**
 * Created by Martin Bobak mbobak@cisco.com on 11/27/14.
 */
public class OperationalStatusHolder {

    private static OperStatus currentOperationalStatus = OperStatus.RUN;

    /**
     * Method provides current operational status of statistics manager.
     *
     * @return {@link org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.statistics.manager.operational.status.rev141127.OperStatus}
     */
    public static OperStatus getOperationalStatus() {
        return OperationalStatusHolder.currentOperationalStatus;
    }

    ;

    /**
     * Method sets operational status for statistics manager.
     *
     * @param operationalStatus
     */
    public static void setOperationalStatus(OperStatus operationalStatus) {
        currentOperationalStatus = operationalStatus;
    }

}
