package com.grad.sam.service.reports;

public interface ReportGenerator<T> {

    T generateReport(String trigger);
}
