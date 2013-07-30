
package org.opendaylight.controller.connectionmanager.internal;

public class ConnectionMgmtEvent {
    ConnectionMgmtEventType event;
    Object data;
    public ConnectionMgmtEvent(ConnectionMgmtEventType event, Object data) {
        this.event = event;
        this.data = data;
    }
    public ConnectionMgmtEventType getEvent() {
        return event;
    }
    public Object getData() {
        return data;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((event == null) ? 0 : event.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConnectionMgmtEvent other = (ConnectionMgmtEvent) obj;
        if (data == null) {
            if (other.data != null)
                return false;
        } else if (!data.equals(other.data))
            return false;
        if (event != other.event)
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "ConnectionMgmtEvent [event=" + event + ", data=" + data + "]";
    }
}
