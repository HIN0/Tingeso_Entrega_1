import http from "../http-common";

class LoanService {
  getAll() {
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
}

export default new LoanService();
