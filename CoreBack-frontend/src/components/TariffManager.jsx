import React, { useEffect, useState } from "react";
import TariffService from "../services/tariff.service";
import { useKeycloak } from "@react-keycloak/web";
import { getErrorMessage } from "../utils/apiError";

function TariffManager() {
  const [tariffs, setTariffs] = useState({
    dailyRentFee: 0,
    dailyLateFee: 0,
    repairFee: 0,
  });
  const [message, setMessage] = useState("");

  const { keycloak } = useKeycloak();
  const isAdmin = keycloak?.authenticated && keycloak.hasRealmRole("ADMIN"); 

  const loadTariffs = () => {
    TariffService.getTariff()
      .then(response => {
        // numeric values for the form
        setTariffs({
          dailyRentFee: response.data.dailyRentFee || 0,
          dailyLateFee: response.data.dailyLateFee || 0,
          repairFee: response.data.repairFee || 0,
        });
        setMessage("");
      })
      .catch(e => {
        console.error("Error fetching tariffs:", e);
        setMessage("Error loading the tariffs.");
      });
  };

  useEffect(() => {
    if (isAdmin) {
      loadTariffs();
    }
  }, [isAdmin]);

  const handleChange = (e) => {
    setTariffs({ ...tariffs, [e.target.name]: parseInt(e.target.value) || 0 });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setMessage("Guardando...");

    TariffService.updateTariff(tariffs)
      .then(() => {
        setMessage("Tariffs updated successfully.");
        loadTariffs();
      })
      .catch((e) => {
        console.error("Error updating tariffs:", e);
        setMessage(`Error updating tariffs: ${getErrorMessage(e)}`);
      });
  };

  if (!isAdmin) {
    return <h3 style={{padding:16}}>Access denied. Only administrators can manage tariffs.</h3>;
  }

  return (
    <div style={{padding: 16}}>
      <h2>Tariffs and Fees</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Daily Rental Fee: </label>
          <input
            type="number"
            name="dailyRentFee"
            value={tariffs.dailyRentFee}
            onChange={handleChange}
            required
          />
        </div>
        <div>
          <label>Daily Late Fee: </label>
          <input
            type="number"
            name="dailyLateFee"
            value={tariffs.dailyLateFee}
            onChange={handleChange}
            required
          />
        </div>
        <div>
          <label>Light Damage Repair Fee: </label>
          <input
            type="number"
            name="repairFee"
            value={tariffs.repairFee}
            onChange={handleChange}
            required
          />
        </div>
        <button type="submit" style={{marginTop: '15px'}}>Update Tariffs</button>
      </form>
      {message && <p style={{ color: message.startsWith("Error") ? 'red' : 'green' }}>{message}</p>}
    </div>
  );
}

export default TariffManager;