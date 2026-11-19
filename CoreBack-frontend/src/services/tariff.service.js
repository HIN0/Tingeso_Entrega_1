import http from "../http-common";

class TariffService {
  getTariff() {
    return http.get("/tariffs");
  }

  // Updates all tariffs at once
  updateTariff(data) {
    // The backend keeps a single tariff record
    // data: { dailyRentFee, dailyLateFee, repairFee }
    return http.put("/tariffs", data);
  }
}

export default new TariffService();