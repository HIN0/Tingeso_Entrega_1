import http from "../http-common";

class KardexService {
  // RF5.2: movement history of a tool
  getByToolId(toolId) {
    // toolId must be a number
    if (isNaN(parseInt(toolId))) {
        return Promise.reject(new Error("Invalid Tool ID. Please enter a number."));
    }
    return http.get(`/kardex/tool/${toolId}`);
  }

  // RF5.3: movements in a date range
  getByDateRange(startDate, endDate) {
    // both dates are required
    if (!startDate || !endDate) {
        return Promise.reject(new Error("Both start date and end date are required."));
    }
    // The backend expects LocalDateTime (YYYY-MM-DDTHH:mm:ss):
    // from 00:00:00 of the first day to 23:59:59 of the last day
    const startDateTime = `${startDate}T00:00:00`;
    const endDateTime = `${endDate}T23:59:59`;
    return http.get(`/kardex/date?start=${startDateTime}&end=${endDateTime}`);
  }
}

export default new KardexService();