import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import ToolList from "./components/ToolList";
import AddTool from "./components/AddTool";

import LoanList from "./components/LoanList";
import AddLoan from "./components/AddLoan";
import ReturnLoan from "./components/ReturnLoan";

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/tools" element={<ToolList />} />
        <Route path="/tools/add" element={<AddTool />} />
        <Route path="/loans" element={<LoanList />} />
        <Route path="/loans/add" element={<AddLoan />} />
        <Route path="/loans/return/:id" element={<ReturnLoan />} />

      </Routes>
    </Router>
  );
}

export default App;
