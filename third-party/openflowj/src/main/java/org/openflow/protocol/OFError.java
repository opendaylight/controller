package org.openflow.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.openflow.protocol.factory.OFMessageFactory;
import org.openflow.protocol.factory.OFMessageFactoryAware;
import org.openflow.util.U16;

/**
 * Represents an ofp_error_msg
 * 
 * @author David Erickson (daviderickson@cs.stanford.edu)
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 */
public class OFError extends OFMessage implements OFMessageFactoryAware {
    public static int MINIMUM_LENGTH = 12;

    public enum OFErrorType {
        OFPET_HELLO_FAILED, OFPET_BAD_REQUEST, OFPET_BAD_ACTION, OFPET_FLOW_MOD_FAILED, OFPET_PORT_MOD_FAILED, OFPET_QUEUE_OP_FAILED
    }

    public enum OFHelloFailedCode {
        OFPHFC_INCOMPATIBLE, OFPHFC_EPERM
    }

    public enum OFBadRequestCode {
        OFPBRC_BAD_VERSION, OFPBRC_BAD_TYPE, OFPBRC_BAD_STAT, OFPBRC_BAD_VENDOR, OFPBRC_BAD_SUBTYPE, OFPBRC_EPERM, OFPBRC_BAD_LEN, OFPBRC_BUFFER_EMPTY, OFPBRC_BUFFER_UNKNOWN
    }

    public enum OFBadActionCode {
        OFPBAC_BAD_TYPE, OFPBAC_BAD_LEN, OFPBAC_BAD_VENDOR, OFPBAC_BAD_VENDOR_TYPE, OFPBAC_BAD_OUT_PORT, OFPBAC_BAD_ARGUMENT, OFPBAC_EPERM, OFPBAC_TOO_MANY, OFPBAC_BAD_QUEUE
    }

    public enum OFFlowModFailedCode {
        OFPFMFC_ALL_TABLES_FULL, OFPFMFC_OVERLAP, OFPFMFC_EPERM, OFPFMFC_BAD_EMERG_TIMEOUT, OFPFMFC_BAD_COMMAND, OFPFMFC_UNSUPPORTED
    }

    public enum OFPortModFailedCode {
        OFPPMFC_BAD_PORT, OFPPMFC_BAD_HW_ADDR
    }

    public enum OFQueueOpFailedCode {
        OFPQOFC_BAD_PORT, OFPQOFC_BAD_QUEUE, OFPQOFC_EPERM
    }

    protected short errorType;
    protected short errorCode;
    protected OFMessageFactory factory;
    protected byte[] error;
    protected boolean errorIsAscii;

    public OFError() {
        super();
        this.type = OFType.ERROR;
        this.length = U16.t(MINIMUM_LENGTH);
    }

    /**
     * @return the errorType
     */
    public short getErrorType() {
        return errorType;
    }

    /**
     * @param errorType
     *            the errorType to set
     */
    public void setErrorType(short errorType) {
        this.errorType = errorType;
    }

    public void setErrorType(OFErrorType type) {
        this.errorType = (short) type.ordinal();
    }

    /**
     * @return the errorCode
     */
    public short getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode
     *            the errorCode to set
     */
    public void setErrorCode(OFHelloFailedCode code) {
        this.errorCode = (short) code.ordinal();
    }

    public void setErrorCode(short errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorCode(OFBadRequestCode code) {
        this.errorCode = (short) code.ordinal();
    }

    public void setErrorCode(OFBadActionCode code) {
        this.errorCode = (short) code.ordinal();
    }

    public void setErrorCode(OFFlowModFailedCode code) {
        this.errorCode = (short) code.ordinal();
    }

    public void setErrorCode(OFPortModFailedCode code) {
        this.errorCode = (short) code.ordinal();
    }

    public void setErrorCode(OFQueueOpFailedCode code) {
        this.errorCode = (short) code.ordinal();
    }

    public OFMessage getOffendingMsg() {
        // should only have one message embedded; if more than one, just
        // grab first
        if (this.error == null)
            return null;
        ByteBuffer errorMsg = ByteBuffer.wrap(this.error);
        if (factory == null)
            throw new RuntimeException("MessageFactory not set");
        List<OFMessage> messages = this.factory.parseMessages(errorMsg,
                error.length);
        // OVS apparently sends partial messages in errors
        // need to be careful of that AND can't use data.limit() as
        // a packet boundary because there could be more data queued
        if (messages.size() > 0)
            return messages.get(0);
        else
            return null;
    }

    /**
     * Write this offending message into the payload of the Error message
     * 
     * @param offendingMsg
     */

    public void setOffendingMsg(OFMessage offendingMsg) {
        if (offendingMsg == null) {
            super.setLengthU(MINIMUM_LENGTH);
        } else {
            this.error = new byte[offendingMsg.getLengthU()];
            ByteBuffer data = ByteBuffer.wrap(this.error);
            offendingMsg.writeTo(data);
            super.setLengthU(MINIMUM_LENGTH + offendingMsg.getLengthU());
        }
    }

    public OFMessageFactory getFactory() {
        return factory;
    }

    @Override
    public void setMessageFactory(OFMessageFactory factory) {
        this.factory = factory;
    }

    /**
     * @return the error
     */
    public byte[] getError() {
        return error;
    }

    /**
     * @param error
     *            the error to set
     */
    public void setError(byte[] error) {
        this.error = error;
    }

    /**
     * @return the errorIsAscii
     */
    public boolean isErrorIsAscii() {
        return errorIsAscii;
    }

    /**
     * @param errorIsAscii
     *            the errorIsAscii to set
     */
    public void setErrorIsAscii(boolean errorIsAscii) {
        this.errorIsAscii = errorIsAscii;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.errorType = data.getShort();
        this.errorCode = data.getShort();
        int dataLength = this.getLengthU() - MINIMUM_LENGTH;
        if (dataLength > 0) {
            this.error = new byte[dataLength];
            data.get(this.error);
            if (this.errorType == OFErrorType.OFPET_HELLO_FAILED.ordinal())
                this.errorIsAscii = true;
        }
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putShort(errorType);
        data.putShort(errorCode);
        if (error != null)
            data.put(error);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(error);
        result = prime * result + errorCode;
        result = prime * result + (errorIsAscii ? 1231 : 1237);
        result = prime * result + errorType;
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        OFError other = (OFError) obj;
        if (!Arrays.equals(error, other.error))
            return false;
        if (errorCode != other.errorCode)
            return false;
        if (errorIsAscii != other.errorIsAscii)
            return false;
        if (errorType != other.errorType)
            return false;
        return true;
    }

}
