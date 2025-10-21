import { useEffect, useState } from "react";
import { useKeycloak } from "@react-keycloak/web";
import ToolService from "../services/tool.service";
import { Link } from "react-router-dom";

function ToolList() {
  const [tools, setTools] = useState([]);
  const { keycloak } = useKeycloak();
  const isAuth = !!keycloak?.authenticated;
  const isAdmin = isAuth && keycloak.hasRealmRole("ADMIN");
  const [error, setError] = useState('');

  const loadTools = () => {
    setError('');
    ToolService.getAll()
      .then(response => setTools(response.data))
      .catch(e => {
          console.error("Error fetching tools:", e);
          setError('Failed to load tools.');
      });
  };

  useEffect(() => {
    loadTools();
  }, []);

  const handleDecommission = (id) => {
    setError('');
    if (window.confirm("Are you sure you want to decommission this tool? This action cannot be undone.")) {
      ToolService.decommission(id)
        .then(loadTools)
        .catch(e => {
            console.error("Error decommissioning tool:", e);
            const errorMsg = e.response?.data?.message || e.response?.data || "Failed to decommission tool.";
            setError(`Error decommissioning tool ${id}: ${errorMsg}`);
        });
    }
  };

  // --- NUEVA FUNCIÓN GENÉRICA PARA AJUSTAR STOCK ---
  const handleStockChange = (id, isIncrease) => {
      const actionText = isIncrease ? "increase" : "decrease";
      const promptMessage = `Enter POSITIVE quantity to ${actionText} stock:`;
      const quantityStr = prompt(promptMessage);

      if (quantityStr === null) return; // Cancelado

      const quantity = parseInt(quantityStr);

      // Validar que sea un número positivo
      if (isNaN(quantity) || quantity <= 0) {
          alert("Invalid quantity. Please enter a positive number.");
          return;
      }

      // Determinar el cambio (+q o -q)
      const quantityChange = isIncrease ? quantity : -quantity;

      setError('');
      ToolService.adjustStock(id, { quantityChange }) // Llama al mismo método del servicio
        .then(loadTools) // Recargar lista
        .catch(e => {
            console.error(`Error ${actionText}ing stock:`, e);
            const errorMsg = e.response?.data?.message || e.response?.data || `Failed to ${actionText} stock.`;
            setError(`Error adjusting stock for tool ${id}: ${errorMsg}`);
        });
  };


  return (
    <div style={{ padding: 16 }}>
      <h2>Tool Inventory</h2>
      {isAdmin && <Link to="/tools/add" style={{ marginRight: '10px' }}>➕ Add New Tool</Link>}
      {error && <p style={{ color: 'red' }}>{error}</p>}

      <table border="1" style={{ width: '100%', marginTop: '15px', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Category</th>
            <th>Status</th>
            <th>Stock</th>
            <th>Repl. Value</th>
            {isAdmin && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {tools.map(tool => (
            <tr key={tool.id}>
              <td>{tool.id}</td>
              <td>{tool.name}</td>
              <td>{tool.category}</td>
              <td>{tool.status}</td>
              <td>{tool.stock}</td>
              <td>${tool.replacementValue}</td>
              {isAdmin && (
                <td>
                  <Link to={`/tools/edit/${tool.id}`} style={{ marginRight: '5px' }}>Edit</Link>

                  {/* --- BOTONES SEPARADOS PARA STOCK --- */}
                  <button
                    onClick={() => handleStockChange(tool.id, true)} // true para Aumentar
                    style={{ marginRight: '5px', backgroundColor: 'darkgreen', color: 'white' }}
                    disabled={tool.status === 'DECOMMISSIONED'} // No ajustar si está de baja
                  >
                    + Stock
                  </button>
                  <button
                    onClick={() => handleStockChange(tool.id, false)} // false para Disminuir
                    style={{ marginRight: '5px', backgroundColor: 'darkorange', color: 'white' }}
                    disabled={tool.status === 'DECOMMISSIONED'} // No ajustar si está de baja
                  >
                    - Stock
                  </button>

                  {tool.status !== "DECOMMISSIONED" && (
                    <button onClick={() => handleDecommission(tool.id)} disabled={tool.status === 'LOANED' || tool.status === 'REPAIRING'}>
                      Decommission
                    </button>
                  )}
                  {(tool.status === 'LOANED' || tool.status === 'REPAIRING') && (
                      <span style={{ fontSize: '0.8em', marginLeft: '5px' }}>(Cannot decommission while {tool.status})</span>
                  )}
                   {tool.status === 'DECOMMISSIONED' && (
                      <span style={{ fontSize: '0.8em', marginLeft: '5px' }}>(Decommissioned)</span>
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default ToolList;