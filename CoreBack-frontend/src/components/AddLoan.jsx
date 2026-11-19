import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import LoanService from "../services/loan.service";
import ClientService from "../services/client.service";
import ToolService from "../services/tool.service";
import { getErrorMessage } from "../utils/apiError";

// yyyy-MM-dd in local time (toISOString() would use UTC and can shift the day)
const toInputDate = (date) => {
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().substring(0, 10);
};

const addDays = (isoDate, days) => {
  const date = new Date(`${isoDate}T00:00:00`);
  date.setDate(date.getDate() + days);
  return toInputDate(date);
};

function AddLoan() {
  const navigate = useNavigate();
  const today = toInputDate(new Date());

  const [clients, setClients] = useState([]);
  const [tools, setTools] = useState([]);
  const [loadingOptions, setLoadingOptions] = useState(true);
  const [loan, setLoan] = useState({
    clientId: "",
    toolId: "",
    startDate: today,
    dueDate: addDays(today, 7),
  });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // Load clients and tools once to fill the selectors
  useEffect(() => {
    Promise.all([ClientService.getAll(), ToolService.getAll()])
      .then(([clientsResponse, toolsResponse]) => {
        setClients([...clientsResponse.data].sort((a, b) => a.name.localeCompare(b.name)));
        setTools([...toolsResponse.data].sort((a, b) => a.name.localeCompare(b.name)));
      })
      .catch((e) => setError(`Could not load clients and tools: ${getErrorMessage(e)}`))
      .finally(() => setLoadingOptions(false));
  }, []);

  // Only tools that can be lent right now (business rule: AVAILABLE with stock >= 1)
  const lendableTools = useMemo(
    () => tools.filter((t) => t.status === "AVAILABLE" && t.stock > 0),
    [tools]
  );

  const selectedClient = clients.find((c) => String(c.id) === loan.clientId);
  const clientIsRestricted = selectedClient?.status === "RESTRICTED";

  const handleChange = (e) => {
    const { name, value } = e.target;
    setLoan((prev) => {
      const next = { ...prev, [name]: value };
      // Keep the due date valid when the start date moves past it
      if (name === "startDate" && next.dueDate < value) next.dueDate = value;
      return next;
    });
    setError("");
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);

    const payload = {
      clientId: Number(loan.clientId),
      toolId: Number(loan.toolId),
      startDate: loan.startDate,
      dueDate: loan.dueDate,
    };

    LoanService.create(payload)
      .then(() => navigate("/loans"))
      // The backend validates every business rule (late loans, debts, 5-loan limit...);
      // show its message instead of failing silently
      .catch((e) => setError(getErrorMessage(e, "The loan could not be registered.")))
      .finally(() => setSubmitting(false));
  };

  if (loadingOptions) return <p style={{ padding: 16 }}>Loading clients and tools...</p>;

  return (
    <div style={{ padding: 16, maxWidth: 520 }}>
      <h2>Register Loan</h2>
      <form onSubmit={handleSubmit} style={{ display: "grid", gap: 12 }}>
        <label>
          Client
          <select name="clientId" value={loan.clientId} onChange={handleChange} required style={{ display: "block", width: "100%" }}>
            <option value="">-- Select a client --</option>
            {clients.map((c) => (
              <option key={c.id} value={c.id} disabled={c.status === "RESTRICTED"}>
                {c.name} ({c.rut}){c.status === "RESTRICTED" ? " - RESTRICTED" : ""}
              </option>
            ))}
          </select>
        </label>
        {clientIsRestricted && (
          <p style={{ color: "darkorange", margin: 0 }}>
            This client is restricted and cannot request loans until they settle their debts.
          </p>
        )}

        <label>
          Tool
          <select name="toolId" value={loan.toolId} onChange={handleChange} required style={{ display: "block", width: "100%" }}>
            <option value="">-- Select a tool --</option>
            {lendableTools.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name} - {t.category} (stock: {t.stock})
              </option>
            ))}
          </select>
        </label>
        {lendableTools.length === 0 && (
          <p style={{ color: "darkorange", margin: 0 }}>No tools are available to lend right now.</p>
        )}

        <label>
          Start date
          <input type="date" name="startDate" value={loan.startDate} onChange={handleChange} required style={{ display: "block" }} />
        </label>

        <label>
          Due date
          <input
            type="date"
            name="dueDate"
            value={loan.dueDate}
            min={loan.startDate > today ? loan.startDate : today}
            onChange={handleChange}
            required
            style={{ display: "block" }}
          />
        </label>

        {error && <p style={{ color: "red", margin: 0 }}>{error}</p>}

        <div style={{ display: "flex", gap: 8 }}>
          <button type="submit" disabled={submitting || clientIsRestricted}>
            {submitting ? "Saving..." : "Save"}
          </button>
          <button type="button" onClick={() => navigate("/loans")}>Cancel</button>
        </div>
      </form>
    </div>
  );
}

export default AddLoan;
