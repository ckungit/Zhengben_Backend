package com.zhangben.backend.dto;

import java.util.List;

public class SettlementResponse {

    private List<SettlementItem> settlements;
    private List<SettlementItem> mySettlements;
    private int originalTransferCount;
    private int minimizedTransferCount;

    public List<SettlementItem> getSettlements() {
        return settlements;
    }

    public void setSettlements(List<SettlementItem> settlements) {
        this.settlements = settlements;
    }

    public List<SettlementItem> getMySettlements() {
        return mySettlements;
    }

    public void setMySettlements(List<SettlementItem> mySettlements) {
        this.mySettlements = mySettlements;
    }

    public int getOriginalTransferCount() {
        return originalTransferCount;
    }

    public void setOriginalTransferCount(int originalTransferCount) {
        this.originalTransferCount = originalTransferCount;
    }

    public int getMinimizedTransferCount() {
        return minimizedTransferCount;
    }

    public void setMinimizedTransferCount(int minimizedTransferCount) {
        this.minimizedTransferCount = minimizedTransferCount;
    }
}
