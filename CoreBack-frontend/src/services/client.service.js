import http from "../http-common";

class ClientService {
  getAll() {
    return http.get("/clients");
  }

  // --- Get client by ID ---
  get(id) {
    return http.get(`/clients/${id}`);
  }

  create(data) {
    // data: { name, rut, phone, email }
    return http.post("/clients", data);
  }

  // --- Update client data ---
  update(id, data) {
    // data: { name, phone, email }
    return http.put(`/clients/${id}`, data);
  }

  updateStatus(id, status) {
    return http.patch(`/clients/${id}/status`, { status: status });
  }

  // --- Reactivate (validated by the backend) ---
  attemptReactivation(id) {
    // returns the client with its final status
    return http.patch(`/clients/${id}/activate`);
  }
}

export default new ClientService();