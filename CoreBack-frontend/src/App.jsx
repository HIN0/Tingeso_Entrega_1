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
import EditTool from "./components/EditTool";
import AddClient from "./components/AddClient"; 
import EditClient from "./components/EditClient";
import KardexViewer from "./components/KardexViewer";

function RequireAuth({ children, roles }) {
  const { keycloak, initialized } = useKeycloak();

  if (!initialized) return null;
  if (!keycloak?.authenticated) return <Navigate to="/" replace />;

  if (roles?.length) {
    const hasRole = roles.some((r) => keycloak.hasRealmRole(r));
    if (!hasRole) return <h3 style={{padding:16}}>Not authorized</h3>;
  }
  return children;
}

function Menu() {
  const { keycloak } = useKeycloak();
  const isAuth = !!keycloak?.authenticated;
  const isAdmin = isAuth && keycloak.hasRealmRole("ADMIN");
  const isUser = isAuth && keycloak.hasRealmRole("EMPLOYEE");

  return (
    <nav style={{display:"flex",gap:12,padding:"8px 16px",borderBottom:"1px solid #eee"}}>
      <Link to="/tools">Tools</Link>
      {(isAdmin) && <Link to="/tools/add">Add tool</Link>}
      <Link to="/loans">Loans</Link>
      {(isAdmin) && <Link to="/clients">Clients</Link>}
      {(isAdmin) && <Link to="/tariffs">Tariffs</Link>}
      {(isUser || isAdmin) && <Link to="/loans/add">Register loan</Link>}
      {(isUser || isAdmin) && <Link to="/reports">Reports</Link>}
      {(isAdmin) && <Link to="/kardex">Kardex</Link>}
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
            <h2>Welcome to ToolRent</h2>
          </div>
        } />
        {/* --- Tool routes --- */}
        <Route path="/tools" element={<RequireAuth><ToolList /></RequireAuth>} />
        <Route path="/tools/add" element={<RequireAuth roles={["ADMIN"]}><AddTool /></RequireAuth>} />
        <Route path="/tools/edit/:id" element={<RequireAuth roles={["ADMIN"]}><EditTool /></RequireAuth>} />

        {/* --- Loan routes --- */}
        <Route path="/loans" element={<RequireAuth><LoanList /></RequireAuth>} />
        <Route path="/loans/add" element={<RequireAuth roles={["ADMIN","EMPLOYEE"]}><AddLoan /></RequireAuth>} />
        <Route path="/loans/return/:id" element={<RequireAuth roles={["ADMIN", "EMPLOYEE"]}><ReturnLoan /></RequireAuth>} />

        {/* --- Client routes --- */}
        <Route path="/clients" element={<RequireAuth roles={["ADMIN"]}><ClientList /></RequireAuth>} />
        <Route path="/clients/add" element={<RequireAuth roles={["ADMIN"]}><AddClient /></RequireAuth>} />
        <Route path="/clients/edit/:id" element={<RequireAuth roles={["ADMIN"]}><EditClient /></RequireAuth>} />
        
        {/* --- Tariff, report and Kardex routes --- */}
        <Route path="/tariffs" element={<RequireAuth roles={["ADMIN"]}><TariffManager /></RequireAuth>} />
        <Route path="/reports" element={<RequireAuth roles={["EMPLOYEE","ADMIN"]}><ReportViewer /></RequireAuth>} />
        <Route path="/kardex" element={<RequireAuth roles={["ADMIN"]}><KardexViewer /></RequireAuth>} />

        {/* fallback */}
        <Route path="*" element={
            <div style={{ padding: 16 }}>
                <h2>Page not found</h2>
                <Link to="/">Back to home</Link>
            </div>
        } />
      </Routes>
    </Router>
  );
}
