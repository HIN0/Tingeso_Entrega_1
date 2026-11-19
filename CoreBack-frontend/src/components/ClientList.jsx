import React, { useEffect, useState } from "react";
import ClientService from "../services/client.service";
import LoanService from "../services/loan.service";
import { useKeycloak } from "@react-keycloak/web";
import { Link } from "react-router-dom";
import { getErrorMessage } from "../utils/apiError";

// Shows the unpaid debts of a client below its row
function DebtDetails({ clientId, messageSetter }) {
  const [debts, setDebts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    setLoading(true);
    setErrorMessage("");
    LoanService.getUnpaidLoansByClient(clientId)
      .then(response => {
        setDebts(response.data);
        if (response.data.length === 0) {
          setErrorMessage("This client has no pending debts (RECEIVED loans).");
        }
      })
      .catch(e => {
        console.error("Error fetching unpaid loans:", e);
        setErrorMessage(`Error loading debts: ${getErrorMessage(e)}`);
      })
      .finally(() => setLoading(false));
  }, [clientId]); // reload if the client changes

  const handlePay = (loanId) => {
    setErrorMessage(""); // clear previous errors
    if (!window.confirm(`Mark the debt of loan #${loanId} as paid?`)) {
        return;
    }
    messageSetter(`Processing payment for loan ${loanId}...`); // message shown by the parent
    LoanService.markAsPaid(loanId)
      .then(() => {
        messageSetter(`Payment registered for loan ${loanId}. Refreshing...`);
        // Reload the debts of this row
        setLoading(true);
        LoanService.getUnpaidLoansByClient(clientId)
          .then(response => setDebts(response.data))
          .catch(e => setErrorMessage(`Error reloading debts: ${getErrorMessage(e)}`))
          .finally(() => setLoading(false));
      })
      .catch(e => {
        console.error("Error marking loan as paid:", e);
        setErrorMessage(`Error registering payment: ${getErrorMessage(e)}`);
        messageSetter(""); // clear the progress message
      });
  };

  if (loading) {
    return (
      <td colSpan="7" style={{ textAlign: 'center', padding: '10px', fontStyle: 'italic' }}>
        Loading debts...
      </td>
    );
  }

  return (
    <td colSpan="7" style={{ padding: '10px', backgroundColor: '#f0f0f0', borderTop: '2px solid grey' }}>
      <h4 style={{ marginTop: 0 }}>Pending Debts (RECEIVED loans)</h4>
      {errorMessage && <p style={{ color: 'red' }}>{errorMessage}</p>}
      {debts.length > 0 ? (
        <table border="1" style={{ width: '95%', margin: 'auto', borderCollapse: 'collapse', backgroundColor: 'white', color: '#333' }}>
          <thead>
            <tr>
              <th>Loan ID</th>
              <th>Tool</th>
              <th>Amount ($)</th>
              <th>Start</th>
              <th>Due</th>
              <th>Returned</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {debts.map(loan => (
              <tr key={loan.id}>
                <td>{loan.id}</td>
                <td>{loan.tool?.name}</td>
                <td>{loan.totalPenalty.toFixed(0)}</td>
                <td>{loan.startDate}</td>
                <td>{loan.dueDate}</td>
                <td>{loan.returnDate}</td>
                <td>
                  <button onClick={() => handlePay(loan.id)} style={{ backgroundColor: 'green', color: 'white' }}>
                    Pay
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        !errorMessage && <p>No pending debts found (RECEIVED loans).</p>
      )}
    </td>
  );
}


function ClientList() {
  const [clients, setClients] = useState([]);
  const { keycloak } = useKeycloak();
  const isAuth = !!keycloak?.authenticated;
  const isAdmin = isAuth && keycloak.hasRealmRole("ADMIN");
  const isEmployee = isAuth && keycloak.hasRealmRole("EMPLOYEE");
  const [message, setMessage] = useState("");

  // Client whose debt details are currently shown
  const [visibleDebtsClientId, setVisibleDebtsClientId] = useState(null);

  const loadClients = () => {
    setMessage("");
    ClientService.getAll()
      .then(response => setClients(response.data))
      .catch(e => {
        console.error("Error fetching clients:", e);
        setMessage("Error loading the client list.");
      });
  };

  useEffect(() => {
    if (isAdmin || isEmployee) {
      loadClients();
    }
  }, [isAdmin, isEmployee]);

  // Restrict directly; activation goes through 'attemptReactivation' (validated by the backend)
  const handleUpdateStatus = (id, currentStatus, clientName) => {
    setMessage("");
    if (currentStatus === "ACTIVE") {
      // Restrict
      if (window.confirm(`Change the status of client ${clientName} (ID: ${id}) to RESTRICTED?`)) {
        ClientService.updateStatus(id, "RESTRICTED")
          .then(loadClients)
          .catch(e => {
            console.error(`Error updating client status to RESTRICTED:`, e);
            setMessage(`Error restricting client: ${getErrorMessage(e)}`);
          });
      }
    } else { // currentStatus === "RESTRICTED"
      // Activate (only if the client has no late loans or debts)
      if (window.confirm(`Try to reactivate client ${clientName} (ID: ${id})? It must have no debts or late loans.`)) {
        ClientService.attemptReactivation(id)
          .then((response) => {
            setMessage(`Reactivation of ${clientName}: final status ${response.data.status}.`);
            loadClients(); // reload to show the new status
          })
          .catch(e => {
            console.error(`Error attempting reactivation for client ${id}:`, e);
            setMessage(`Error reactivating ${clientName}: ${getErrorMessage(e)}`);
          });
      }
    }
  };

  // Show / hide the debt details of a client
  const handleShowOrHideDebts = (clientId) => {
    setMessage(""); // clear the general message
    if (visibleDebtsClientId === clientId) {
      setVisibleDebtsClientId(null); // already visible: hide
    } else {
      setVisibleDebtsClientId(clientId); // otherwise: show
    }
  };

  return (
    <div style={{ padding: 16 }}>
      <h2>Client Management</h2>
      {isAdmin && (
        <Link to="/clients/add" style={{ marginBottom: '15px', display: 'inline-block' }}>
          ➕ Register New Client
        </Link>
      )}
      {message && <p style={{ color: message.startsWith("Error") ? 'red' : 'green' }}>{message}</p>}

      <table border="1" style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th>ID</th>
            <th>RUT</th>
            <th>Name</th>
            <th>Email</th>
            <th>Phone</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {clients.map(client => (
            // React.Fragment: a client may render a second row with its debts
            <React.Fragment key={client.id}>
              <tr>
                <td>{client.id}</td>
                <td>{client.rut}</td>
                <td>{client.name}</td>
                <td>{client.email}</td>
                <td>{client.phone}</td>
                <td style={{ fontWeight: 'bold', color: client.status === 'RESTRICTED' ? 'red' : 'green' }}>{client.status}</td>
                <td>
                  {/* Edit (admin only) */}
                  {isAdmin && (
                    <Link to={`/clients/edit/${client.id}`} style={{ marginRight: '10px' }}>
                      Edit
                    </Link>
                  )}
                  {/* Activate / restrict (admin only) */}
                  {isAdmin && (
                    <button
                      onClick={() => handleUpdateStatus(client.id, client.status, client.name)}
                      style={{ backgroundColor: client.status === 'ACTIVE' ? 'darkred' : 'darkgreen', color: 'white', marginRight: '10px' }}
                    >
                      {client.status === 'ACTIVE' ? 'Restrict' : 'Activate (check)'}
                    </button>
                  )}
                  {/* Show / hide debts (only for restricted clients) */}
                  {(isAdmin || isEmployee) && client.status === 'RESTRICTED' && (
                       <button
                          onClick={() => handleShowOrHideDebts(client.id)}
                          style={{ backgroundColor: 'purple', color: 'white' }}
                        >
                          {visibleDebtsClientId === client.id ? 'Hide Debts' : 'Show Debts'}
                        </button>
                  )}
                </td>
              </tr>
              {/* Optional row with the debt details */}
              {visibleDebtsClientId === client.id && (
                <tr className="debt-details-row">
                  {/* setMessage lets the child component show messages */}
                  <DebtDetails clientId={client.id} messageSetter={setMessage} />
                </tr>
              )}
            </React.Fragment>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default ClientList;