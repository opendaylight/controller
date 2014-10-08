package org.opendaylight.controller.frm.impl;

public class FRMConfig {
    private final boolean cleanAlienFlowsOnReconcil;

    private FRMConfig(FrmConfigBuilder builder) {
        cleanAlienFlowsOnReconcil = builder.isCleanAlienFlowsOnReconcil();
    }

    public boolean isCleanAlienFlowsOnReconcil() {
        return cleanAlienFlowsOnReconcil;
    }

    public static FrmConfigBuilder builder() {
        return new FrmConfigBuilder();
    }

    public static class FrmConfigBuilder {
        private boolean cleanAlienFlowsOnReconcil;

        public boolean isCleanAlienFlowsOnReconcil() {
            return cleanAlienFlowsOnReconcil;
        }

        public FrmConfigBuilder setCleanAlienFlowsOnReconcil(boolean cleanAlienFlowsOnReconcil) {
            this.cleanAlienFlowsOnReconcil = cleanAlienFlowsOnReconcil;
            return this;
        }

        public FRMConfig build() {
            return new FRMConfig(this);
        }
    }
}
