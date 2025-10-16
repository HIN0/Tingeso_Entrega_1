import React, { useEffect, useState } from "react";
import ClientService from "../services/client.service";
import { useKeycloak } from "@react-keycloak/web";

function ClientList() {
  const [clients, setClients] = useState([]);
  const { keycloak } = useKeycloak();
  const isAuth = !!keycloak?.authenticated;
  const isAdmin = isAuth && keycloak.hasRealmRole("ADMIN"); // Solo ADMIN puede ver/gestionar clientes

  const loadClients = () => {
    ClientService.getAll()
      .then(response => setClients(response.data))
      .catch(e => console.error("Error fetching clients:", e));
  };

  useEffect(() => {
    // Solo carga si está autenticado (la ruta ya está protegida)
    if (isAdmin) {
      loadClients();
    }
  }, [isAdmin]);

  const handleUpdateStatus = (id, currentStatus) => {
    // Si está ACTIVO, lo pasamos a RESTRICTED; si está RESTRICTED, a ACTIVE.
    const newStatus = currentStatus === "ACTIVE" ? "RESTRICTED" : "ACTIVE";
    
    if (window.confirm(`¿Seguro que quieres cambiar el estado del cliente ${id} a ${newStatus}?`)) {
      ClientService.updateStatus(id, newStatus)
        .then(loadClients)
        .catch(e => console.error(`Error updating client status to ${newStatus}:`, e));
    }
  };

  if (!isAdmin) {
    return <h3 style={{padding:16}}>Acceso denegado. Solo Administradores pueden gestionar clientes.</h3>;
  }

  return (
    <div style={{padding: 16}}>
      <h2>Gestión de Clientes</h2>
      {/* Podríamos añadir un enlace para crear clientes si hubiera un componente AddClient */}
      
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>RUT</th>
            <th>Nombre</th>
            <th>Email</th>
            <th>Estado</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {clients.map(client => (
            <tr key={client.id}>
              <td>{client.id}</td>
              <td>{client.rut}</td>
              <td>{client.name}</td>
              <td>{client.email}</td>
              <td style={{fontWeight: 'bold', color: client.status === 'RESTRICTED' ? 'red' : 'green'}}>{client.status}</td>
              <td>
                <button 
                  onClick={() => handleUpdateStatus(client.id, client.status)}
                  style={{backgroundColor: client.status === 'ACTIVE' ? 'darkred' : 'green', color: 'white'}}
                >
                  {client.status === 'ACTIVE' ? 'Restringir' : 'Activar'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default ClientList;