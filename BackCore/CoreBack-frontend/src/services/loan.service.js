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

  // --- NUEVA FUNCIÓN ---
  markAsPaid(loanId) {
    // Llama al nuevo endpoint PATCH
    return http.patch(`/loans/${loanId}/pay`);
  }

  // --- (Opcional) Funciones para obtener listas específicas ---
  getActiveLoans() {
    return http.get("/loans/active");
  }

  getLateLoans() {
      return http.get("/loans/late");
  }

  /* // Si implementas el endpoint para cerrados no pagados
  getUnpaidClosedLoans() {
      return http.get("/loans/closed/unpaid");
  }
  */

}

export default new LoanService();