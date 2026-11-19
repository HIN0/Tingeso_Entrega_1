import { useEffect, useState } from "react";
import { useKeycloak } from "@react-keycloak/web";
import ToolService from "../services/tool.service";
import { Link } from "react-router-dom";
import { getErrorMessage } from "../utils/apiError";

// --- Sort order of the inventory table ---
const sortTools = (a, b) => {
    // Order of the non-decommissioned statuses
    const statusOrder = {
        'AVAILABLE': 1,
        'REPAIRING': 2,
        'LOANED': 4,
    };
    
    const orderA = statusOrder[a.status] || 99;
    const orderB = statusOrder[b.status] || 99;

    // by status order
    if (orderA !== orderB) {
        return orderA - orderB;
    }

    // tie-break: ascending ID
    return a.id - b.id;
};
// ------------------------------------------

function ToolList() {
  const [activeTools, setActiveTools] = useState([]); // not decommissioned
  const [decommissionedTools, setDecommissionedTools] = useState([]); // decommissioned
  
  const { keycloak } = useKeycloak();
  const isAuth = !!keycloak?.authenticated;
  const isAdmin = isAuth && keycloak.hasRealmRole("ADMIN");
  const [error, setError] = useState('');

  // --- Inline stock adjustment state ---
  const [adjustment, setAdjustment] = useState({
      id: null,
      quantity: 1,
      type: null
  });


  const loadTools = () => {
    setError('');
    ToolService.getAll()
      .then(response => {
          const allTools = response.data;
          
          // 1. Split active and decommissioned tools
          const decommissioned = allTools.filter(t => t.status === 'DECOMMISSIONED');
          const active = allTools.filter(t => t.status !== 'DECOMMISSIONED');
          
          // 2. Sort the active ones
          const sortedActive = active.sort(sortTools);
          
          setActiveTools(sortedActive);
          // decommissioned tools sorted by ID
          setDecommissionedTools(decommissioned.sort((a, b) => a.id - b.id)); 
          
      })
      .catch(e => {
          console.error("Error fetching tools:", e);
          setError('Failed to load tools.');
      });
  };

  useEffect(() => {
    loadTools();
  }, []);

  // --- Decommission and stock adjustment ---
  const handleDecommission = (id) => {
    setError('');
    if (window.confirm("Are you sure you want to decommission this tool? This action cannot be undone.")) {
      ToolService.decommission(id)
        .then(loadTools)
        .catch(e => {
            console.error("Error decommissioning tool:", e);
            const errorMsg = getErrorMessage(e, "Failed to decommission tool.");
            setError(`Error decommissioning tool ${id}: ${errorMsg}`);
        });
    }
  };

  // Repair evaluation of one damaged unit: "complete" returns it to stock, "write-off" removes it
  const handleRepair = (tool, action) => {
    setError('');
    const question = action === 'complete'
      ? `Mark one unit of "${tool.name}" as repaired? It goes back to the available stock.`
      : `Write off one unit of "${tool.name}"? The damage is irreparable and the unit is removed permanently.`;
    if (!window.confirm(question)) return;

    const request = action === 'complete'
      ? ToolService.completeRepair(tool.id)
      : ToolService.writeOffRepair(tool.id);
    request
      .then(loadTools)
      .catch(e => {
          const errorMsg = getErrorMessage(e, "Failed to update the repair.");
          setError(`Error updating repair for tool ${tool.id}: ${errorMsg}`);
      });
  };

  const startStockAdjustment = (id, type) => {
      setError('');
      setAdjustment({ id, quantity: 1, type });
  };

  const handleAdjustmentChange = (e) => {
      const value = parseInt(e.target.value);
      setAdjustment(prev => ({ 
          ...prev, 
          quantity: (isNaN(value) || value < 1) ? 1 : value 
      }));
  };

  const applyStockAdjustment = () => {
      if (!adjustment.id || adjustment.quantity < 1) {
          setError("Invalid quantity or tool selected.");
          return;
      }
      
      const isIncrease = adjustment.type === 'INCREASE';
      const actionText = isIncrease ? "increase" : "decrease";
      const quantityChange = isIncrease ? adjustment.quantity : -adjustment.quantity;
      
      setError('');
      ToolService.adjustStock(adjustment.id, { quantityChange }) 
          .then(() => {
              setAdjustment({ id: null, quantity: 1, type: null });
              loadTools(); 
          })
          .catch(e => {
              console.error(`Error ${actionText}ing stock:`, e);
              const errorMsg = getErrorMessage(e, `Failed to ${actionText} stock.`);
              setError(`Error adjusting stock for tool ${adjustment.id}: ${errorMsg}`);
          });
  };


  return (
    <div style={{ padding: 16 }}>
      <h2>Tool Inventory</h2>
      {isAdmin && <Link to="/tools/add" style={{ marginRight: '10px' }}>➕ Add New Tool</Link>}
      {error && <p style={{ color: 'red' }}>{error}</p>}

      {/* ========================================================================================================
          TABLE 1: ACTIVE INVENTORY (every status except DECOMMISSIONED)
          ======================================================================================================== */}
      <table border="1" style={{ width: '95%', marginTop: '15px',marginLeft: 'auto', marginRight: 'auto', borderCollapse: 'collapse'}}>
        
        {/* --- 9 columns (table 1) --- */}
        <colgroup>
            <col style={{ width: '4%' }} />    {/* 1. ID */}
            <col style={{ width: '17%' }} />   {/* 2. Name */}
            <col style={{ width: '11%' }} />   {/* 3. Category */}
            <col style={{ width: '9%' }} />    {/* 4. Status */}
            <col style={{ width: '5%' }} />    {/* 5. Stock */}
            <col style={{ width: '6%' }} />    {/* 6. Loaned */}
            <col style={{ width: '6%' }} />    {/* 7. In Repair */}
            <col style={{ width: '9%' }} />    {/* 8. Repl. Value */}
            <col style={{ width: '33%' }} />   {/* 9. Actions */}
        </colgroup>
        
      {/* --- Green title row --- */}
        <thead>
          <tr>
            <th colSpan="9" style={{ backgroundColor: '#65e10cff', color: 'white', textAlign: 'center', padding: '8px' }}>
                ACTIVE ({activeTools.length - activeTools.filter(t => t.status === 'REPAIRING').length}) AND REPAIRING ({activeTools.filter(t => t.status === 'REPAIRING').length})
            </th>
          </tr>
        </thead>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Category</th>
            <th>Status</th>
            <th>Stock</th>
            <th>Loaned</th>
            <th>In Repair</th>
            <th>Repl. Value</th>
            {isAdmin && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {activeTools.map(tool => (
            <tr key={tool.id}>
              <th>{tool.id}</th>
              <td>{tool.name}</td>
              <td>{tool.category}</td>
              <td>{tool.status}</td>
              <td>{tool.stock}</td>
              <td>{tool.loaned ?? 0}</td>
              <td>{tool.inRepair}</td>
              <td>${tool.replacementValue}</td>
              {isAdmin && (
                <td>
                  <Link to={`/tools/edit/${tool.id}`} style={{ marginRight: '5px' }}>Edit</Link>

                  {/* Inline stock adjustment */}
                  {adjustment.id === tool.id ? (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                          <input
                            type="number"
                            value={adjustment.quantity}
                            onChange={handleAdjustmentChange}
                            min="1"
                            style={{ width: '50px', padding: '2px', textAlign: 'center' }}
                          />
                          <button
                            onClick={applyStockAdjustment}
                            style={{ backgroundColor: adjustment.type === 'INCREASE' ? 'darkgreen' : 'darkorange', color: 'white', padding: '2px 8px' }}
                          >
                            Apply {adjustment.type === 'INCREASE' ? '+' : '-'}
                          </button>
                          <button
                            onClick={() => setAdjustment({ id: null, quantity: 1, type: null })}
                            style={{ backgroundColor: 'grey', color: 'white', padding: '2px 8px' }}
                          >
                            X
                          </button>
                      </div>
                  ) : (
                      <>
                        <button
                          onClick={() => startStockAdjustment(tool.id, 'INCREASE')}
                          style={{ marginRight: '5px', backgroundColor: 'darkgreen', color: 'white' }}
                          disabled={tool.status === 'DECOMMISSIONED'} 
                        >
                          + Stock
                        </button>
                        <button
                          onClick={() => startStockAdjustment(tool.id, 'DECREASE')}
                          style={{ marginRight: '5px', backgroundColor: 'darkorange', color: 'white' }}
                          disabled={tool.status === 'DECOMMISSIONED'} 
                        >
                          - Stock
                        </button>
                      </>
                  )}

                  {/* Repair evaluation: only when there are damaged units waiting */}
                  {tool.inRepair > 0 && (
                    <>
                      <button
                        onClick={() => handleRepair(tool, 'complete')}
                        style={{ marginRight: '5px', backgroundColor: 'steelblue', color: 'white' }}
                      >
                        Repaired
                      </button>
                      <button
                        onClick={() => handleRepair(tool, 'write-off')}
                        style={{ marginRight: '5px', backgroundColor: 'firebrick', color: 'white' }}
                      >
                        Irreparable
                      </button>
                    </>
                  )}

                  {/* Decommission the whole tool: not allowed while units are loaned or in repair */}
                  {tool.status !== "DECOMMISSIONED" && (
                    <button onClick={() => handleDecommission(tool.id)} disabled={(tool.loaned ?? 0) > 0 || tool.inRepair > 0}>
                      Decommission
                    </button>
                  )}
                  {((tool.loaned ?? 0) > 0 || tool.inRepair > 0) && (
                      <span style={{ fontSize: '0.8em', marginLeft: '5px' }}>(Cannot decommission while units are loaned or in repair)</span>
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
      <h1></h1>

      {/* ========================================================================================================
        TABLE 2: DECOMMISSIONED TOOLS (no status, stock or actions)
        ======================================================================================================== */}
      <table border="1" style={{ width: '95%', marginTop: '15px',marginLeft: 'auto', marginRight: 'auto', borderCollapse: 'collapse' }}>
        
        {/* --- 4 columns (table 2) --- */}
        <colgroup>
            <col style={{ width: '5%' }} />    {/* 1. ID */}
            <col style={{ width: '20%' }} />   {/* 2. Name */}
            <col style={{ width: '30%' }} />   {/* 3. Category */}
            <col style={{ width: '30%' }} />   {/* 7. Repl. Value */}
        </colgroup>
        
        {/* --- Red title row --- */}
        <thead>
          <tr>
            <th colSpan="4" style={{ backgroundColor: '#cc0000', color: 'white', textAlign: 'center', padding: '8px' }}>
                TOOLS DECOMMISSIONED ({decommissionedTools.length})
            </th>
          </tr>
        </thead>
        
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Category</th>
            <th>Replacement Value</th>
          </tr>
        </thead>
        <tbody>
          {decommissionedTools.map(tool => (
            <tr key={tool.id}>
              <th>{tool.id}</th>
              <td>{tool.name}</td>
              <td>{tool.category}</td>
              <td>${tool.replacementValue}</td>
            </tr>
          ))}
        </tbody>
      </table>
      
    </div>
  );
}

export default ToolList;