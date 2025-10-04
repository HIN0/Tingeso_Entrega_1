import http from "../http-common";

class ToolService {
  getAll() {
    return http.get("/tools");
  }

  get(id) {
    return http.get(`/tools/${id}`);
  }

  create(data) {
    return http.post("/tools", data);
  }

  decommission(id) {
    return http.put(`/tools/${id}/decommission`);
  }
}

export default new ToolService();
