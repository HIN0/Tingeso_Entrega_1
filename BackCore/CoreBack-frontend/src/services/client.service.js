import http from "../http-common";

class ClientService {
  getAll() {
    return http.get("/clients");
  }

  create(data) {
    return http.post("/clients", data);
  }

  updateStatus(id, status) {
    // PATCH /clients/{id}/status con body { "status": "RESTRICTED" }
    return http.patch(`/clients/${id}/status`, { status: status });
  }
}

export default new ClientService();