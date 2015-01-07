package org.opendaylight.datasand.tests;


public class PojoObject {
    private int testIndex;
    private String testString;
    private boolean testBoolean;
    private long testLong;
    private short testShort;
    private SubPojoObject subPojo = null;

    public PojoObject(){
    }

    public String getTestString() {
        return testString;
    }

    public void setTestString(String testString) {
        this.testString = testString;
    }

    public boolean isTestBoolean() {
        return testBoolean;
    }

    public void setTestBoolean(boolean testBoolean) {
        this.testBoolean = testBoolean;
    }

    public long getTestLong() {
        return testLong;
    }

    public void setTestLong(long testLong) {
        this.testLong = testLong;
    }

    public short getTestShort() {
        return testShort;
    }

    public void setTestShort(short testShort) {
        this.testShort = testShort;
    }

    public int getTestIndex() {
        return testIndex;
    }

    public void setTestIndex(int testIndex) {
        this.testIndex = testIndex;
    }

    public SubPojoObject getSubPojo() {
        return subPojo;
    }

    public void setSubPojo(SubPojoObject subPojo) {
        this.subPojo = subPojo;
    }
}
