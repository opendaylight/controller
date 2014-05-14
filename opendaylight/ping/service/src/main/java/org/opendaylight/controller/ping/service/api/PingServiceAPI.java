package org.opendaylight.controller.ping.service.api;


public interface PingServiceAPI {

    public enum PingResult {
        InProgress(0),
        GotResponse(1),
        NoResponse(2),
        Error(3);

        int value;
        static java.util.Map<java.lang.Integer, PingResult> valueMap;

        static {
            valueMap = new java.util.HashMap<>();
            for (PingResult enumItem : PingResult.values())
            {
                valueMap.put(enumItem.value, enumItem);
            }
        }

        private PingResult(int value) {
            this.value = value;
        }

        /**
         * @return integer value
         */
        public int getIntValue() {
            return value;
        }

        /**
         * @param valueArg
         * @return corresponding EchoResult item
         */
        public static PingResult forValue(int valueArg) {
            return valueMap.get(valueArg);
        }
    }

    /**
     * pingDestinationSync
     *
     * Will block caller until ping operation is finished.
     *
     * @param address An IPv4 address to be pinged
     * @return PingResult enum. Will never return InProgress.
     */
    PingResult pingDestinationSync(String address);

    /**
     * pingDestinationAsync
     *
     * Will return last known state for given address.
     *
     * @param address An IPv4 address to be pinged
     * @return PingResult enum.
     */
    PingResult pingDestinationAsync(String address);

    /**
     * pingAsyncClear
     *
     * Will remove async ping for given address.
     *
     * @param address An IPv4 address to be pinged
     */
    void pingAsyncClear(String address);
}

