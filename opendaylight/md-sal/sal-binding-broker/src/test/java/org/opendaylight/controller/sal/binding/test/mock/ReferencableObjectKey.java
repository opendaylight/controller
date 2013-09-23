package org.opendaylight.controller.sal.binding.test.mock;

import org.opendaylight.yangtools.yang.binding.Identifier;

public class ReferencableObjectKey implements Identifier<ReferencableObject> {

    final Integer value;
    
    public ReferencableObjectKey(Integer _value) {
        this.value = _value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        ReferencableObjectKey other = (ReferencableObjectKey) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ReferencableObjectKey [value=" + value + "]";
    }
    
    
}
