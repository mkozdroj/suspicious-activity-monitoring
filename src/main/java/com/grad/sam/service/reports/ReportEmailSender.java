package com.grad.sam.service.reports;

import java.util.List;

public interface ReportEmailSender<T> {

    List<String> sendReportEmail(T reportResult);
}
