import { BrowserRouter as Router, Routes, Route, Navigate, Link } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";
import Header from "./components/Header";
import ToolList from "./components/ToolList";
import AddTool from "./components/AddTool";
import LoanList from "./components/LoanList";
import AddLoan from "./components/AddLoan";
import ReturnLoan from "./components/ReturnLoan";
import ClientList from "./components/ClientList";
import TariffManager from "./components/TariffManager";
import ReportViewer from "./components/ReportViewer";

function RequireAuth({ children, roles }) {
  const { keycloak, initialized } = useKeycloak();

  if (!initialized) return null;
  if (!keycloak?.authenticated) return <Navigate to="/" replace />;

  if (roles?.length) {
    const hasRole = roles.some((r) => keycloak.hasRealmRole(r));
    if (!hasRole) return <h3 style={{padding:16}}>No autorizado</h3>;
  }
  return children;
}

// Una vez ya logeado con Keycloak, este componente muestra el menú según el rol
function Menu() {
  const { keycloak } = useKeycloak();
  const isAuth = !!keycloak?.authenticated;
  const isAdmin = isAuth && keycloak.hasRealmRole("ADMIN");
  const isUser = isAuth && keycloak.hasRealmRole("USER");

  return (
    <nav style={{display:"flex",gap:12,padding:"8px 16px",borderBottom:"1px solid #eee"}}>
      <Link to="/tools">Herramientas</Link>
      {(isAdmin) && <Link to="/tools/add">Agregar herramienta</Link>}
      <Link to="/loans">Préstamos</Link>
      {(isUser || isAdmin) && <Link to="/loans/add">Registrar préstamo</Link>}
      {(isAdmin) && <Link to="/clients">Clientes</Link>}
      {(isAdmin) && <Link to="/tariffs">Tarifas</Link>}
      {(isUser || isAdmin) && <Link to="/reports">Reportes</Link>} 
    </nav>
  );
}

export default function App() {
  return (
    <Router>
      <Header />
      <Menu />
      <Routes>  
        <Route path="/" element={
          <div style={{ padding: 16 }}>
            <h2>Bienvenido</h2>
            <p>Inicia sesión para ver tu menú según tu rol.</p>
          </div>
        } />

        {/* protegidas por login */}
        <Route path="/tools" element={<RequireAuth><ToolList /></RequireAuth>} />
        <Route path="/tools/add" element={<RequireAuth roles={["ADMIN"]}><AddTool /></RequireAuth>} />
        <Route path="/loans" element={<RequireAuth><LoanList /></RequireAuth>} />
        <Route path="/loans/add" element={<RequireAuth roles={["USER","ADMIN"]}><AddLoan /></RequireAuth>} />
        <Route path="/loans/return/:id" element={<RequireAuth roles={["ADMIN"]}><ReturnLoan /></RequireAuth>} />
        <Route path="/clients" element={<RequireAuth roles={["ADMIN"]}><ClientList /></RequireAuth>} />
        <Route path="/tariffs" element={<RequireAuth roles={["ADMIN"]}><TariffManager /></RequireAuth>} />
        <Route path="/reports" element={<RequireAuth roles={["USER","ADMIN"]}><ReportViewer /></RequireAuth>} /> 

        {/* fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}
