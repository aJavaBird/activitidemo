package com.km.zhc.activiti.demo.holiday;

import java.io.Serializable;

public class HolidayInfo implements Serializable {
    private String fromDate;
    private String toDate;
    private Integer days;
    private String remark;

    public String getFromDate() {
        return fromDate;
    }

    public HolidayInfo setFromDate(String fromDate) {
        this.fromDate = fromDate;
        return this;
    }

    public String getToDate() {
        return toDate;
    }

    public HolidayInfo setToDate(String toDate) {
        this.toDate = toDate;
        return this;
    }

    public Integer getDays() {
        return days;
    }

    public HolidayInfo setDays(Integer days) {
        this.days = days;
        return this;
    }

    public String getRemark() {
        return remark;
    }

    public HolidayInfo setRemark(String remark) {
        this.remark = remark;
        return this;
    }
}
