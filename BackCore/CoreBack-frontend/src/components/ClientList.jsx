import React, { useEffect, useState } from "react";
import ClientService from "../services/client.service";
import { useKeycloak } from "@react-keycloak/web";
import { Link } from "react-router-dom";

function ClientList() {
  const [clients, setClients] = useState([]);
  const { keycloak } = useKeycloak();
  const isAuth = !!keycloak?.authenticated;
  const isAdmin = isAuth && keycloak.hasRealmRole("ADMIN");
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
    if (isAdmin) {
      loadClients();
    }
  }, [isAdmin]);

  const handleUpdateStatus = (id, currentStatus) => {
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

  if (!isAdmin) {
    return <h3 style={{ padding: 16 }}>Acceso denegado. Solo Administradores pueden gestionar clientes.</h3>;
  }

  return (
    <div style={{ padding: 16 }}>
      <h2>Gestión de Clientes</h2>
      {/* --- ENLACE PARA AÑADIR CLIENTE --- */}
      <Link to="/clients/add" style={{ marginBottom: '15px', display: 'inline-block' }}>
        ➕ Registrar Nuevo Cliente
      </Link>
      {message && <p style={{ color: message.startsWith("Error") ? 'red' : 'green' }}>{message}</p>}

      <table border="1" style={{ width: '100%', borderCollapse: 'collapse' }}> {/* Estilo tabla */}
        <thead>
          <tr>
            <th>ID</th>
            <th>RUT</th>
            <th>Nombre</th>
            <th>Email</th>
            <th>Teléfono</th> {/* Añadir columna teléfono */}
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
              <td>{client.phone}</td> {/* Mostrar teléfono */}
              <td style={{ fontWeight: 'bold', color: client.status === 'RESTRICTED' ? 'red' : 'green' }}>{client.status}</td>
              <td>
                {/* --- ENLACE PARA EDITAR --- */}
                <Link to={`/clients/edit/${client.id}`} style={{ marginRight: '10px' }}>
                  Editar
                </Link>
                <button
                  onClick={() => handleUpdateStatus(client.id, client.status)}
                  style={{ backgroundColor: client.status === 'ACTIVE' ? 'darkred' : 'darkgreen', color: 'white' }} /* Colores ajustados */
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