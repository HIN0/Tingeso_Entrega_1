import http from "../http-common";

class ReportService {
  // RF6.1: loans by status, optional date range
  getLoansByStatus(status, from, to) {
    let url = `/reports/loans?status=${status}`;
    // add the date range only when both dates are present
    if (from && to) {
      url += `&from=${from}&to=${to}`;
    }
    return http.get(url);
  }

  // RF6.2: clients with late loans, optional date range
  getClientsWithLateLoans(from, to) {
    let url = "/reports/clients/late";
    // add the date range only when both dates are present
    if (from && to) {
      url += `?from=${from}&to=${to}`;
    }
    return http.get(url);
  }

  // RF6.3: most loaned tools (date range required)
  getTopTools(from, to) {
    // the ranking always needs both dates
    if (!from || !to) {
        return Promise.reject(new Error("Both 'from' and 'to' dates are required for Top Tools report."));
    }
    return http.get(`/reports/tools/top?from=${from}&to=${to}`);
  }
}

export default new ReportService();