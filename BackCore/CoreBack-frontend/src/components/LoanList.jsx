import { useEffect, useState } from "react";
import LoanService from "../services/loan.service";
import { Link } from "react-router-dom";

function LoanList() {
  const [loans, setLoans] = useState([]);

  const loadLoans = () => {
    LoanService.getAll()
      .then(response => {
        setLoans(response.data);
      })
      .catch(e => {
        console.error("Error fetching loans:", e);
      });
  };

  useEffect(() => {
    loadLoans();
  }, []);

  return (
    <div>
      <h2>Loans</h2>
      <Link to="/loans/add">➕ Add Loan</Link>
      <ul>
        {loans.map(loan => (
          <li key={loan.id}>
            Client: {loan.client?.name} | Tool: {loan.tool?.name} | 
            Start: {loan.startDate} | Due: {loan.dueDate} | Status: {loan.status}
            {" "}
            {loan.status === "ACTIVE" && (
              <Link to={`/loans/return/${loan.id}`}>Return</Link>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}

export default LoanList;
