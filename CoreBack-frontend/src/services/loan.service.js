import http from "../http-common";

class LoanService {
  getAll() {
    // Ajustado para coincidir con el backend (devuelve activos + atrasados)
    return http.get("/loans");
  }

  get(id) {
    return http.get(`/loans/${id}`);
  }

  create(data) {
    return http.post("/loans", data);
  }

  returnLoan(id, data) {
    return http.put(`/loans/${id}/return`, data);
  }

  markAsPaid(loanId) {
    return http.patch(`/loans/${loanId}/pay`);
  }

  getUnpaidLoansByClient(clientId) {
    return http.get(`/loans/client/${clientId}/unpaid`);
  }
}

export default new LoanService();