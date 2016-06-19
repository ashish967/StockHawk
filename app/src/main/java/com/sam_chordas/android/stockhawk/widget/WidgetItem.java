package com.sam_chordas.android.stockhawk.widget;

public class WidgetItem {

    String symbol,percentage_change,change,bidprice;
    int isup;
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getIsup() {
        return isup;
    }

    public void setIsup(int isup) {
        this.isup = isup;
    }

    public String getPercentage_change() {
        return percentage_change;
    }

    public void setPercentage_change(String percentage_change) {
        this.percentage_change = percentage_change;
    }

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }

    public String getBidprice() {
        return bidprice;
    }

    public void setBidprice(String bidprice) {
        this.bidprice = bidprice;
    }
}