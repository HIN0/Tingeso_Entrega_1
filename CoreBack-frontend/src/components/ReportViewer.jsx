import React, { useEffect, useState } from "react";
import ReportService from "../services/report.service";
import { useKeycloak } from "@react-keycloak/web";
import { getErrorMessage } from "../utils/apiError";

function ReportViewer() {
  const [reportType, setReportType] = useState("LATE_CLIENTS"); // selected report
  const [reportData, setReportData] = useState([]); // report rows
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(""); 

  // Date range used by the filters
  const [dateRange, setDateRange] = useState({ from: "", to: "" });
  // Whether the date filter is enabled
  const [useDateFilter, setUseDateFilter] = useState(false);

  const { keycloak } = useKeycloak();
  const isAdminOrUser = keycloak?.authenticated && (keycloak.hasRealmRole("ADMIN") || keycloak.hasRealmRole("EMPLOYEE"));

  // Loads a report, applying the date range when the filter is enabled
  const loadReport = (type, filterDates) => {
    if (!isAdminOrUser) return;
    setLoading(true);
    setReportData([]);
    setMessage(""); // clear messages

    // Dates sent to the service (null = no filter)
    const fromDate = filterDates && dateRange.from ? dateRange.from : null;
    const toDate = filterDates && dateRange.to ? dateRange.to : null;

    // With the filter enabled both dates are required (Top Tools always requires them)
    if (filterDates && type !== "TOP_TOOLS" && (!fromDate || !toDate)) {
        setMessage("Error: Both start and end dates are required when date filter is active.");
        setLoading(false);
        return;
    }
    if (type === "TOP_TOOLS" && (!fromDate || !toDate)) {
        // also validated by the backend; show a clear message here
        setMessage("Error: Date range is mandatory for Top Tools report.");
        setLoading(false);
        return;
    }


    let promise;
    switch (type) {
      case "ACTIVE_LOANS":
      case "LATE_LOANS":
        // dates may be null (no filter)
        promise = ReportService.getLoansByStatus(type.replace('_LOANS', ''), fromDate, toDate);
        break;
      case "LATE_CLIENTS":
         // dates may be null (no filter)
        promise = ReportService.getClientsWithLateLoans(fromDate, toDate);
        break;
      case "TOP_TOOLS":
         // dates are mandatory here and were validated above
        promise = ReportService.getTopTools(fromDate, toDate);
        break;
      default:
        setMessage("Error: Invalid report type selected.");
        setLoading(false);
        return; // unknown report type
    }

    // Handle the response
    promise
      .then(response => {
        setReportData(response.data);
        if (response.data.length === 0) {
          setMessage("No results found for the selected criteria.");
        }
      })
      .catch(e => {
          console.error("Error loading report:", e);
          setMessage(`Error loading report: ${getErrorMessage(e)}`);
      })
      .finally(() => setLoading(false));
  };

  // Initial report (no date filter)
  useEffect(() => {
    loadReport(reportType, false); // initial load, no date filter
    // Intentional: run only on first render / role change, not on every reportType change
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdminOrUser]); // only when the role changes

  // Select a report and run it
  const handleRunReport = (type) => {
    setReportType(type);
    // uses the current date filter state
    loadReport(type, useDateFilter);
  };

  // Report table
  const renderData = () => {
    // Renders the table of the selected report
    if (loading) return <p>Loading data...</p>;
    if (message && reportData.length === 0) return <p>{message}</p>; // no data: show the message
    if (reportData.length === 0) return <p>Click a report button to view data.</p>; // initial state


    switch (reportType) {
      case "ACTIVE_LOANS":
      case "LATE_LOANS":
        return (
          <table>
            <thead><tr><th>ID</th><th>Client</th><th>Tool</th><th>Start</th><th>Due</th><th>Status</th></tr></thead>
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
            <thead><tr><th>ID</th><th>RUT</th><th>Name</th><th>Email</th><th>Status</th></tr></thead>
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
            <thead><tr><th>Rank</th><th>Tool</th><th>Total Loans</th></tr></thead>
            <tbody>
              {/* The backend returns [tool, count] pairs */}
              {reportData.map((item, index) => (
                <tr key={item[0]?.id || index}> {/* tool ID as key */}
                  <td>{index + 1}</td>
                  <td>{item[0]?.name}</td>
                  <td>{item[1]}</td> {/* count */}
                </tr>
              ))}
            </tbody>
          </table>
        );
      default:
        return <p>Select a report type.</p>;
    }
  };


  return (
    <div style={{ padding: 16 }}>
      <h2>Reports</h2>

      {/* --- Date filter --- */}
      <div style={{ marginBottom: '20px', border: '1px solid #ccc', padding: '10px' }}>
        <h4>Date Filter (Optional for Loans/Clients, Required for Top Tools)</h4>
        <label>
          <input
            type="checkbox"
            checked={useDateFilter}
            onChange={(e) => setUseDateFilter(e.target.checked)}
          />
          Apply Date Filter
        </label>
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center', marginTop: '10px', opacity: useDateFilter ? 1 : 0.5 }}>
          <label>From:</label>
          <input
            type="date"
            value={dateRange.from}
            onChange={e => setDateRange({ ...dateRange, from: e.target.value })}
            disabled={!useDateFilter} // enabled only with the filter
          />
          <label>To:</label>
          <input
            type="date"
            value={dateRange.to}
            onChange={e => setDateRange({ ...dateRange, to: e.target.value })}
            disabled={!useDateFilter} // enabled only with the filter
          />
        </div>
      </div>


      {/* --- Report buttons --- */}
      <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
        <button onClick={() => handleRunReport("LATE_CLIENTS")} disabled={loading}>
          Clients with Late Loans (RF6.2) {useDateFilter ? '(Filtered)' : ''}
        </button>
        <button onClick={() => handleRunReport("ACTIVE_LOANS")} disabled={loading}>
          Active Loans (RF6.1) {useDateFilter ? '(Filtered)' : ''}
        </button>
        <button onClick={() => handleRunReport("LATE_LOANS")} disabled={loading}>
          Late Loans (RF6.1) {useDateFilter ? '(Filtered)' : ''}
        </button>
        {/* The ranking always needs a date range */}
        <button
           onClick={() => handleRunReport("TOP_TOOLS")}
           // disabled while loading or when dates are missing
           disabled={loading || (useDateFilter && (!dateRange.from || !dateRange.to))}
           title={!useDateFilter ? "Enable date filter to run Top Tools report" : ""} // Tooltip
        >
          Most Loaned Tools (RF6.3) {useDateFilter ? '(Filtered)' : '(Date Range Required)'}
        </button>
         {!useDateFilter && <span style={{fontSize: '0.9em', color: 'orange'}}>(Enable Date Filter for Top Tools)</span>}
         {useDateFilter && (!dateRange.from || !dateRange.to) && <span style={{fontSize: '0.9em', color: 'red'}}>(Select From/To Dates)</span>}

      </div>

      {/* --- Results --- */}
      <div style={{ marginTop: '20px', textAlign: 'left' }}>
        <h3>
          Results: {reportType.replace('_', ' ')}
          {useDateFilter && dateRange.from && dateRange.to && ` (From ${dateRange.from} To ${dateRange.to})`}
        </h3>
        {renderData()}
      </div>
    </div>
  );
}

export default ReportViewer;