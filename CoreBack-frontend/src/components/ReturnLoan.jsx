import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import LoanService from "../services/loan.service";
import { getErrorMessage } from "../utils/apiError";

function ReturnLoan() {
  const { id } = useParams();
  const navigate = useNavigate();
  
  // Loan details (tool ID and replacement value)
  const [loanDetails, setLoanDetails] = useState(null);
  const [returnForm, setReturnForm] = useState({
    returnDate: "",
    damaged: false,
    irreparable: false,
    toolId: null, 
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 1. Load the loan details
  useEffect(() => {
    LoanService.get(id)
      .then(response => {
        const loan = response.data;
        if (loan.status !== "ACTIVE" && loan.status !== "LATE") {
            setError("This loan is not open (ACTIVE or LATE).");
            setLoading(false);
            return;
        }
        setLoanDetails(loan);
        setReturnForm(prev => ({
            ...prev,
            toolId: loan.tool.id, // required by the backend DTO
            // Return date defaults to today
            returnDate: new Date().toISOString().substring(0, 10) 
        }));
        setLoading(false);
      })
      .catch(e => {
        console.error("Error fetching loan details:", e);
        setError("Error loading the loan details.");
        setLoading(false);
      });
  }, [id]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    let newForm = { ...returnForm, [name]: type === 'checkbox' ? checked : value };
    
    // Unchecking 'damaged' also unchecks 'irreparable'
    if (name === 'damaged' && !checked) {
        newForm.irreparable = false;
    }

    setReturnForm(newForm);
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!returnForm.toolId) {
        setError("Error: tool ID not found.");
        return;
    }
    
    if (returnForm.damaged && !window.confirm("This return generates extra charges. Continue?")) {
        return;
    }

    const payload = {
        toolId: returnForm.toolId,
        damaged: returnForm.damaged,
        irreparable: returnForm.irreparable,
        returnDate: returnForm.returnDate,
    };
    
    LoanService.returnLoan(id, payload)
      .then(() => {
        navigate("/loans");
      })
      .catch((e) => {
        console.error("Error returning loan:", e.response ? e.response.data : e);
        setError(`Error registering the return: ${getErrorMessage(e)}`);
      });
  };

  if (loading) return <h3 style={{padding:16}}>Loading loan details...</h3>;
  if (error) return <h3 style={{padding:16, color: 'red'}}>{error}</h3>;
  if (!loanDetails) return <h3 style={{padding:16}}>Loan not found.</h3>;


  return (
    <div style={{ padding: 16 }}>
      <h2>Return Loan #{id}</h2>
      <p>Tool: <strong>{loanDetails.tool?.name}</strong> | Client: <strong>{loanDetails.client?.name}</strong></p>
      <p>Due date: {loanDetails.dueDate}</p>

      <form onSubmit={handleSubmit}>
        <div>
          <label>Return date: </label>
          <input
            type="date"
            name="returnDate"
            value={returnForm.returnDate}
            onChange={handleChange}
            required
          />
        </div>

        <div style={{marginTop: '10px'}}>
          <label>
            <input
              type="checkbox"
              name="damaged"
              checked={returnForm.damaged}
              onChange={handleChange}
            />
            Damaged tool? (repair or replacement charge applies)
          </label>
        </div>

        {returnForm.damaged && (
            <div style={{marginLeft: '20px', marginTop: '10px'}}>
                <label>
                    <input
                        type="checkbox"
                        name="irreparable"
                        checked={returnForm.irreparable}
                        onChange={handleChange}
                    />
                    Irreparable damage? (replacement charge: ${loanDetails.tool?.replacementValue})
                </label>
            </div>
        )}

        <button type="submit" style={{marginTop: '20px'}}>Confirm Return</button>
      </form>
      {error && <p style={{ color: 'red' }}>{error}</p>}
    </div>
  );
}

export default ReturnLoan;