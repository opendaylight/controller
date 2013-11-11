package org.opendaylight.controller.sal.restconf.impl.test.structures;

import java.util.*;

public class LstItem {
    String lstName;
    Map<String, Lf> lfs;
    Map<String, LfLst> lfLsts;
    Map<String, Lst> lsts;
    Map<String, Cont> conts;

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

    public void addLfLst(LfLst lfLst) {
        lfLsts.put(lfLst.getName(), lfLst);
    }

    public void addLst(Lst lst) {
        lsts.put(lst.getName(), lst);
    }

    public void addCont(Cont cont) {
        conts.put(cont.getName(), cont);
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
        if (!this.conts.equals(lstItem.conts)) {
            return false;
        }
        if (!this.lfs.equals(lstItem.lfs)) {
            return false;
        }
        if (!this.lfLsts.equals(lstItem.lfLsts)) {
            return false;
        }
        if (!this.lsts.equals(lstItem.lsts)) {
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
