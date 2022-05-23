package com.byby.trobot.controller.dto;

public class ExchangeDto {
    private String name;
    private boolean isOpen;
    private boolean isTradingDay;
    private int hoursBeforeOpen;
    private int minutesBeforeOpen;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public boolean isTradingDay() {
        return isTradingDay;
    }

    public void setTradingDay(boolean tradingDay) {
        isTradingDay = tradingDay;
    }

    public int getHoursBeforeOpen() {
        return hoursBeforeOpen;
    }

    public void setHoursBeforeOpen(int hoursBeforeOpen) {
        this.hoursBeforeOpen = hoursBeforeOpen;
    }

    public int getMinutesBeforeOpen() {
        return minutesBeforeOpen;
    }

    public void setMinutesBeforeOpen(int minutesBeforeOpen) {
        this.minutesBeforeOpen = minutesBeforeOpen;
    }
}
