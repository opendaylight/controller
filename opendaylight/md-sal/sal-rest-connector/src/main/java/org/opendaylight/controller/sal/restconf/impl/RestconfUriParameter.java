package org.opendaylight.controller.sal.restconf.impl;

public enum RestconfUriParameter {
    DEPTH {
        @Override
        public String toString() {
            return "depth";
        }
    },
    FORMAT {
        @Override
        public String toString() {
            return "format";
        }
    },
    INSERT {
        @Override
        public String toString() {
            return "insert";
        }
    },
    POINT {
        @Override
        public String toString() {
            return "point";
        }
    },
    SELECT {
        @Override
        public String toString() {
            return "select";
        }
    }
}
