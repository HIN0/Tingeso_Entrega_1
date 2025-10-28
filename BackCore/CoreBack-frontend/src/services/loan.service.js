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
      // Necesitarías crear este endpoint en el backend, por ejemplo:
      // GET /loans/client/{clientId}/unpaid
      // Que filtre por status=RECEIVED y totalPenalty > 0
      return http.get(`/loans/client/${clientId}/unpaid`); // Endpoint hipotético
  }

  // --- NUEVO MÉTODO: Obtener deudas pendientes (RECEIVED) por ID de cliente ---
  getUnpaidLoansByClient(clientId) {
    return http.get(`/loans/client/${clientId}/unpaid`);
    // Devuelve una promesa que resuelve a un array de LoanEntity
  }

  getActiveLoans() {
    return http.get("/loans/active");
  }

  getLateLoans() {
      return http.get("/loans/late");
  }

  getUnpaidClosedLoans() {
      return http.get("/loans/closed/unpaid");
  }
}

export default new LoanService();