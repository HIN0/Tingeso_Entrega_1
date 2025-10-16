import React, { useEffect, useState } from "react";
import ReportService from "../services/report.service";
import { useKeycloak } from "@react-keycloak/web";

function ReportViewer() {
  const [reportType, setReportType] = useState("LATE_CLIENTS");
  const [reportData, setReportData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [dateRange, setDateRange] = useState({ from: "", to: "" });

  const { keycloak } = useKeycloak();
  const isAdminOrUser = keycloak?.authenticated && (keycloak.hasRealmRole("ADMIN") || keycloak.hasRealmRole("USER"));

  const loadReport = (type, from, to) => {
    if (!isAdminOrUser) return;
    setLoading(true);
    setReportData([]);

    let promise;
    switch (type) {
      case "ACTIVE_LOANS":
      case "LATE_LOANS":
        promise = ReportService.getLoansByStatus(type.replace('_LOANS', ''));
        break;
      case "LATE_CLIENTS":
        promise = ReportService.getClientsWithLateLoans();
        break;
      case "TOP_TOOLS":
        if (!from || !to) {
          setLoading(false);
          return;
        }
        promise = ReportService.getTopTools(from, to);
        break;
      default:
        setLoading(false);
        return;
    }

    promise
      .then(response => setReportData(response.data))
      .catch(e => console.error("Error loading report:", e))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    // Cargar reporte por defecto (clientes atrasados) al montar
    loadReport(reportType);
  }, [isAdminOrUser]);

  const handleRunReport = (type) => {
    setReportType(type);
    loadReport(type, dateRange.from, dateRange.to);
  };

  const renderData = () => {
    if (loading) return <p>Cargando datos...</p>;
    if (reportData.length === 0) return <p>No hay resultados para este reporte.</p>;

    switch (reportType) {
      case "ACTIVE_LOANS":
      case "LATE_LOANS":
        return (
          <table>
            <thead><tr><th>ID</th><th>Cliente</th><th>Herramienta</th><th>Inicio</th><th>Vencimiento</th><th>Estado</th></tr></thead>
            <tbody>
              {reportData.map(loan => (
                <tr key={loan.id}>
                  <td>{loan.id}</td>
                  <td>{loan.client?.name} ({loan.client?.rut})</td>
                  <td>{loan.tool?.name}</td>
                  <td>{loan.startDate}</td>
                  <td>{loan.dueDate}</td>
                  <td>{loan.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        );
      case "LATE_CLIENTS":
        return (
          <table>
            <thead><tr><th>ID</th><th>RUT</th><th>Nombre</th><th>Email</th><th>Estado</th></tr></thead>
            <tbody>
              {reportData.map(client => (
                <tr key={client.id}>
                  <td>{client.id}</td>
                  <td>{client.rut}</td>
                  <td>{client.name}</td>
                  <td>{client.email}</td>
                  <td>{client.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        );
      case "TOP_TOOLS":
        return (
          <table>
            <thead><tr><th>Ranking</th><th>Herramienta</th><th>Total Préstamos</th></tr></thead>
            <tbody>
              {/* El backend devuelve Object[] con ToolEntity y Count */}
              {reportData.map((item, index) => (
                <tr key={index}>
                  <td>{index + 1}</td>
                  <td>{item[0]?.name}</td>
                  <td>{item[1]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        );
      default:
        return null;
    }
  };

  return (
    <div style={{ padding: 16 }}>
      <h2>Reportes del Sistema (Épica 6)</h2>
      
      {/* Botones de Reporte */}
      <div style={{marginBottom: '20px', display: 'flex', gap: '10px', flexWrap: 'wrap'}}>
        <button onClick={() => handleRunReport("LATE_CLIENTS")} disabled={loading}>Clientes con Préstamos Atrasados (RF6.2)</button>
        <button onClick={() => handleRunReport("ACTIVE_LOANS")} disabled={loading}>Préstamos Activos (RF6.1)</button>
        <button onClick={() => handleRunReport("LATE_LOANS")} disabled={loading}>Préstamos Atrasados (RF6.1)</button>
        
        {/* Formulario para Top Tools */}
        <div style={{display: 'flex', gap: '10px', alignItems: 'center'}}>
            <label>Desde:</label>
            <input type="date" value={dateRange.from} onChange={e => setDateRange({...dateRange, from: e.target.value})} />
            <label>Hasta:</label>
            <input type="date" value={dateRange.to} onChange={e => setDateRange({...dateRange, to: e.target.value})} />
            <button onClick={() => handleRunReport("TOP_TOOLS")} disabled={loading || !dateRange.from || !dateRange.to}>Ranking Herramientas (RF6.3)</button>
        </div>
      </div>

      {/* Visor de Datos */}
      <div style={{marginTop: '20px', textAlign: 'left'}}>
        <h3>Resultados: {reportType}</h3>
        {renderData()}
      </div>
    </div>
  );
}

export default ReportViewer;