import React, { useEffect, useState } from "react";
import ClientService from "../services/client.service";
import LoanService from "../services/loan.service";
import { useKeycloak } from "@react-keycloak/web";
import { Link } from "react-router-dom";

function ClientList() {
  const [clients, setClients] = useState([]);
  const { keycloak } = useKeycloak();
  const isAuth = !!keycloak?.authenticated;
  const isAdmin = isAuth && keycloak.hasRealmRole("ADMIN");
  const isEmployee = isAuth && keycloak.hasRealmRole("USER");
  const [message, setMessage] = useState("");

  const loadClients = () => {
    setMessage("");
    ClientService.getAll()
      .then(response => setClients(response.data))
      .catch(e => {
          console.error("Error fetching clients:", e);
          setMessage("Error al cargar la lista de clientes.");
      });
  };

  useEffect(() => {
    if (isAdmin || isEmployee) { // Permitir a Empleado ver la lista si tiene acceso
      loadClients();
    }
  }, [isAdmin, isEmployee]); // Depender de ambos roles

  const handleUpdateStatus = (id, currentStatus) => {
    // ... (sin cambios)
     setMessage(""); // Limpiar mensaje
    const newStatus = currentStatus === "ACTIVE" ? "RESTRICTED" : "ACTIVE";

    if (window.confirm(`¿Seguro que quieres cambiar el estado del cliente ${id} a ${newStatus}?`)) {
      ClientService.updateStatus(id, newStatus)
        .then(loadClients) // Recargar la lista si tiene éxito
        .catch(e => {
            console.error(`Error updating client status to ${newStatus}:`, e);
            setMessage(`Error al actualizar estado: ${e.response?.data?.message || e.message}`);
        });
    }
  };

  // --- NUEVA FUNCIÓN PARA PAGAR DEUDAS (SIMPLIFICADA) ---
  // Esta función es una simplificación. Marca como pagado el *primer* préstamo
  // cerrado con deuda que encuentre para el cliente. Idealmente, se debería
  // mostrar la lista de deudas y permitir pagar una específica.
  const handlePayDues = async (clientId) => {
      setMessage(`Buscando deudas para cliente ${clientId}...`);
      try {
           // 1. Obtener TODOS los préstamos del cliente (necesitaríamos un endpoint o filtrar en frontend)
           // Alternativa: Llamar a un endpoint específico si lo creamos: GET /loans/client/{clientId}/unpaid
           // Solución simple (menos eficiente): Obtener todos y filtrar
          const allLoansResponse = await LoanService.getAll(); // Asume que esto trae todos o necesitamos otro método
          const clientLoans = allLoansResponse.data.filter(loan => loan.client?.id === clientId);

           // 2. Encontrar el primer préstamo cerrado con deuda
          const unpaidLoan = clientLoans.find(loan => loan.status === 'CLOSED' && loan.totalPenalty > 0);

          if (!unpaidLoan) {
              setMessage(`Cliente ${clientId} no tiene deudas pendientes registradas.`);
               // Si no hay deudas, intentar activar por si acaso quedó restringido erróneamente
              if (clients.find(c => c.id === clientId)?.status === 'RESTRICTED') {
                    if (window.confirm(`No se encontraron deudas para el cliente ${clientId}, pero está restringido. ¿Desea intentar activarlo?`)){
                        ClientService.updateStatus(clientId, "ACTIVE")
                            .then(loadClients)
                            .catch(e => setMessage(`Error al activar cliente: ${e.response?.data?.message || e.message}`));
                    }
              }
              return;
          }

           // 3. Confirmar y marcar como pagado
           if (window.confirm(`Marcar la deuda del préstamo #${unpaidLoan.id} ($${unpaidLoan.totalPenalty.toFixed(0)}) como pagada para el cliente ${clientId}?`)) {
               setMessage(`Procesando pago para préstamo ${unpaidLoan.id}...`);
               LoanService.markAsPaid(unpaidLoan.id)
                   .then((response) => {
                       // El backend devuelve el estado actualizado del cliente
                       setMessage(`Pago registrado para préstamo ${unpaidLoan.id}. Estado del cliente: ${response.data.status}`);
                       loadClients(); // Recargar la lista para ver el estado actualizado
                   })
                   .catch(e => {
                       console.error("Error marking loan as paid:", e);
                       setMessage(`Error al marcar pago: ${e.response?.data?.message || e.message}`);
                   });
           } else {
                setMessage(""); // Limpiar mensaje si cancela
           }

       } catch (error) {
           console.error("Error fetching or processing loans for payment:", error);
           setMessage(`Error al procesar pago: ${error.message}`);
       }
  };


  // --- Renderizado ---
   if (!isAdmin && !isEmployee) { // Ajustar condición si Empleado no debe ver
     return <h3 style={{ padding: 16 }}>Acceso denegado.</h3>;
   }


  return (
    <div style={{ padding: 16 }}>
      <h2>Gestión de Clientes</h2>
      {/* Solo Admin puede agregar */}
      {isAdmin && (
          <Link to="/clients/add" style={{ marginBottom: '15px', display: 'inline-block' }}>
            ➕ Registrar Nuevo Cliente
          </Link>
      )}
      {message && <p style={{ color: message.startsWith("Error") ? 'red' : 'green' }}>{message}</p>}

      <table border="1" style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th>ID</th>
            <th>RUT</th>
            <th>Nombre</th>
            <th>Email</th>
            <th>Teléfono</th>
            <th>Estado</th>
            <th>Acciones</th> {/* Ampliar columna */}
          </tr>
        </thead>
        <tbody>
          {clients.map(client => (
            <tr key={client.id}>
              <td>{client.id}</td>
              <td>{client.rut}</td>
              <td>{client.name}</td>
              <td>{client.email}</td>
              <td>{client.phone}</td>
              <td style={{ fontWeight: 'bold', color: client.status === 'RESTRICTED' ? 'red' : 'green' }}>{client.status}</td>
              <td>
                {/* Admin puede editar datos */}
                {isAdmin && (
                    <Link to={`/clients/edit/${client.id}`} style={{ marginRight: '10px' }}>
                      Editar
                    </Link>
                )}
                {/* Admin puede cambiar estado manualmente */}
                {isAdmin && (
                    <button
                      onClick={() => handleUpdateStatus(client.id, client.status)}
                      style={{ backgroundColor: client.status === 'ACTIVE' ? 'darkred' : 'darkgreen', color: 'white', marginRight: '10px' }}
                    >
                      {client.status === 'ACTIVE' ? 'Restringir' : 'Activar'}
                    </button>
                )}
                {/* Admin o Empleado pueden marcar deudas como pagadas SI está restringido */}
                {(isAdmin || isEmployee) && client.status === 'RESTRICTED' && (
                     <button
                        onClick={() => handlePayDues(client.id)}
                        style={{ backgroundColor: 'blue', color: 'white' }}
                      >
                        Pagar Deudas
                      </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default ClientList;