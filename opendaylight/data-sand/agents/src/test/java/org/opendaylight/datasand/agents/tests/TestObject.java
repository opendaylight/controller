package org.opendaylight.datasand.agents.tests;

public class TestObject {
    private String name = null;
    private String address = null;
    private int zipcode = -1;
    private long social = -1;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getZipcode() {
        return zipcode;
    }

    public void setZipcode(int zipcode) {
        this.zipcode = zipcode;
    }

    public long getSocial() {
        return social;
    }

    public void setSocial(long social) {
        this.social = social;
    }

    @Override
    public boolean equals(Object obj) {
        TestObject other = (TestObject) obj;
        if (name.equals(other.name) && address.equals(other.address)
                && zipcode == other.zipcode && social == other.social)
            return true;
        return false;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }
}
