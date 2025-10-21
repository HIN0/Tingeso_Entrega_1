import http from "../http-common";

class ClientService {
  getAll() {
    return http.get("/clients");
  }

  // --- OBTENER CLIENTE POR ID ---
  get(id) {
    return http.get(`/clients/${id}`);
  }

  create(data) {
    // data debe incluir name, rut, phone, email
    return http.post("/clients", data);
  }

  // --- NUEVO: ACTUALIZAR DATOS CLIENTE ---
  update(id, data) {
    // data debe incluir name, phone, email
    return http.put(`/clients/${id}`, data);
  }

  updateStatus(id, status) {
    return http.patch(`/clients/${id}/status`, { status: status });
  }
}

export default new ClientService();