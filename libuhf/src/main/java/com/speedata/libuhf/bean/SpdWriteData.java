package com.speedata.libuhf.bean;

/**
 * Created by 张明_ on 2017/11/15.
 */

public class SpdWriteData {
    private byte[] EPCData;
    private int EPCLen;
    private int RSS;
    private int status;

    public byte[] getEPCData() {
        return EPCData;
    }

    public void setEPCData(byte[] EPCData) {
        this.EPCData = EPCData;
    }

    public int getEPCLen() {
        return EPCLen;
    }

    public void setEPCLen(int EPCLen) {
        this.EPCLen = EPCLen;
    }

    public int getRSS() {
        return RSS;
    }

    public void setRSS(int RSS) {
        this.RSS = RSS;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
