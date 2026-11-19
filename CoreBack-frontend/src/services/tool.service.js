import http from "../http-common";

class ToolService {
  // --- Get all ---
  getAll() {
    return http.get("/tools");
  }

  // --- Get one ---
  get(id) {
    return http.get(`/tools/${id}`);
  }

  // --- Create ---
  create(data) {
    return http.post("/tools", data);
  }

  // --- Update ---
  update(id, data) {
    // data: { name, category, replacementValue }
    return http.put(`/tools/${id}`, data);
  }

  // --- Adjust stock ---
  adjustStock(id, data) {
      // data: { quantityChange: number }
      return http.patch(`/tools/${id}/stock`, data);
  }

  // --- Decommission ---
  decommission(id) {
    return http.put(`/tools/${id}/decommission`);
  }

  // --- REPAIR EVALUATION (one damaged unit) ---
  completeRepair(id) {
    return http.patch(`/tools/${id}/repair/complete`);
  }

  writeOffRepair(id) {
    return http.patch(`/tools/${id}/repair/write-off`);
  }
}

export default new ToolService();