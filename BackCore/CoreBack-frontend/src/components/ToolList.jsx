import { useEffect, useState } from "react";
import { useKeycloak } from "@react-keycloak/web";
import ToolService from "../services/tool.service";
import { Link } from "react-router-dom";

function ToolList() {
  const [tools, setTools] = useState([]);
  const { keycloak } = useKeycloak();
  const isAuth = !!keycloak?.authenticated;
  const isAdmin = isAuth && keycloak.hasRealmRole("ADMIN");

  const loadTools = () => {
    ToolService.getAll()
      .then(response => setTools(response.data))
      .catch(e => console.error("Error fetching tools:", e));
  };

  useEffect(() => {
    loadTools();
  }, []);

  const handleDecommission = (id) => {
    if (window.confirm("Are you sure you want to decommission this tool?")) {
      ToolService.decommission(id)
        .then(loadTools)
        .catch(e => console.error("Error decommissioning tool:", e));
    }
  };

  return (
    <div>
      <h2>Tool Inventory</h2>
      {isAdmin && <Link to="/tools/add">➕ Add New Tool</Link>}
      <ul>
        {tools.map(tool => (
          <li key={tool.id}>
            {tool.name} | {tool.category} | {tool.status} | ${tool.replacementValue}
            {" "}
            {isAdmin && tool.status !== "DECOMMISSIONED" && (
              <button onClick={() => handleDecommission(tool.id)}>
                Decommission
              </button>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}

export default ToolList;
