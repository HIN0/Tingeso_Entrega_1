import http from "../http-common";

class ReportService {
  // RF6.1: préstamos por estado
  getLoansByStatus(status) {
    return http.get(`/reports/loans?status=${status}`);
  }

  // RF6.2: clientes con préstamos atrasados
  getClientsWithLateLoans() {
    return http.get("/reports/clients/late");
  }

  // RF6.3: ranking de herramientas más prestadas en un rango
  getTopTools(from, to) {
    return http.get(`/reports/tools/top?from=${from}&to=${to}`);
  }
}

export default new ReportService();