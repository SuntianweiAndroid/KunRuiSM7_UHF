package com.speedata.uhf.dialog;

public class ResultBeen {
    private int result = 0;
    private String logInfo;
    private String epc;

    public ResultBeen(int result, String logInfo, String epc) {
        this.result = result;
        this.logInfo = logInfo;
        this.epc = epc;
    }

    public String getEpc() {
        return epc;
    }

    public void setEpc(String epc) {
        this.epc = epc;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getLogInfo() {
        return logInfo;
    }

    public void setLogInfo(String logInfo) {
        this.logInfo = logInfo;
    }
}
