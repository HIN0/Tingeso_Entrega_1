import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import LoanService from "../services/loan.service";

function ReturnLoan() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [returnDate, setReturnDate] = useState("");

  const handleSubmit = (e) => {
    e.preventDefault();
    LoanService.returnLoan(id, { returnDate })
      .then(() => {
        navigate("/loans");
      })
      .catch((e) => {
        console.error("Error returning loan:", e);
      });
  };

  return (
    <div>
      <h2>Return Loan</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Return Date: </label>
          <input
            type="date"
            value={returnDate}
            onChange={(e) => setReturnDate(e.target.value)}
            required
          />
        </div>
        <button type="submit">Return</button>
      </form>
    </div>
  );
}

export default ReturnLoan;
