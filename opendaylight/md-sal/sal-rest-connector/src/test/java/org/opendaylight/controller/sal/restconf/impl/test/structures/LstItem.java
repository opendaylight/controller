package org.opendaylight.controller.sal.restconf.impl.test.structures;

import java.util.*;

public class LstItem {
    String lstName;
    Map<String, Lf> lfs;
    Map<String, LfLst> lfLsts;
    Map<String, Lst> lsts;
    Map<String, Cont> conts;
    private int numOfEqualItems = 0;

    public LstItem() {
        lfs = new HashMap<>();
        conts = new HashMap<>();
        lfLsts = new HashMap<>();
        lsts = new HashMap<>();
    }

    public Map<String, Lst> getLsts() {
        return lsts;
    }

    public Map<String, Cont> getConts() {
        return conts;
    }

    public Map<String, LfLst> getLfLsts() {
        return lfLsts;
    }

    public Map<String, Lf> getLfs() {
        return lfs;
    }

    public String getLstName() {
        return lstName;
    }

    public LstItem addLf(Lf lf) {
        lfs.put(lf.getName(), lf);
        return this;
    }

    public LstItem addLf(String name, String value) {
        lfs.put(name, new Lf(name, value));
        return this;
    }

    public void addLfLst(LfLst lfLst) {
        lfLsts.put(lfLst.getName(), lfLst);
    }

    public void addLst(Lst lst) {
        lsts.put(lst.getName(), lst);
    }

    public void addCont(Cont cont) {
        conts.put(cont.getName(), cont);
    }

    public void incNumOfEqualItems() {
        this.numOfEqualItems++;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }
        LstItem lstItem = (LstItem) obj;
        if (this.conts == null) {
            if (lstItem.conts != null) {
                return false;
            }
        } else if (!this.conts.equals(lstItem.conts)) {
            return false;
        }
        if (this.lfs == null) {
            if (lstItem.lfs != null) {
                return false;
            }
        } else if (!this.lfs.equals(lstItem.lfs)) {
            return false;
        }
        if (this.lfLsts == null) {
            if (lstItem.lfLsts != null) {
                return false;
            }
        } else if (!this.lfLsts.equals(lstItem.lfLsts)) {
            return false;
        }
        if (this.lsts == null) {
            if (lstItem.lsts != null) {
                return false;
            }
        } else if (!this.lsts.equals(lstItem.lsts)) {
            return false;
        }
        if (this.numOfEqualItems != lstItem.numOfEqualItems) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((lfs == null) ? 0 : lfs.hashCode());
        result = prime * result + ((lfLsts == null) ? 0 : lfLsts.hashCode());
        result = prime * result + ((lsts == null) ? 0 : lsts.hashCode());
        result = prime * result + ((conts == null) ? 0 : conts.hashCode());
        result = prime * result + numOfEqualItems;
        return result;
    }

    @Override
    public String toString() {
        return "lst item of " + lstName;
    }

    public void setLstName(String name) {
        this.lstName = name;
    }

}
