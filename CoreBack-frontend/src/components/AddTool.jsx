import { useState } from "react";
import ToolService from "../services/tool.service";
import { useNavigate } from "react-router-dom";
import { getErrorMessage } from "../utils/apiError";

function AddTool() {
  // Units loaned / in repair and the status are decided by the backend, not sent from here
  const [tool, setTool] = useState({
    name: "",
    category: "",
    replacementValue: 0,
    stock: 0,
  });
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleChange = (e) => {
    const value = (e.target.name === 'stock' || e.target.name === 'replacementValue')
                  ? parseInt(e.target.value) || 0
                  : e.target.value;
    setTool({ ...tool, [e.target.name]: value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setError("");

    if (tool.stock < 0 || tool.replacementValue < 0) {
        setError("Stock and Replacement Value cannot be negative.");
        return;
    }
     if (tool.replacementValue < 1000) { // same rule as the backend (@Min(1000))
        setError("Replacement Value must be at least 1000.");
        return;
    }


    ToolService.create(tool)
      .then(() => {
        navigate("/tools");
      })
      .catch((err) => {
        console.error("Error creating tool:", err);
        setError(getErrorMessage(err, "Failed to create tool. Please check the data."));
      });
  };

  return (
    <div style={{ padding: 16 }}>
      <h2>Add Tool</h2>
      {/* Error message, keeping line breaks */}
      {error && (
        <div style={{ color: 'red', whiteSpace: 'pre-wrap', border: '1px solid red', padding: '10px', marginBottom: '15px' }}>
          <strong>Error:</strong><br/>{error}
        </div>
      )}
      <form onSubmit={handleSubmit}>
        {/* Form fields */}
         <div>
          <label>Name: </label>
          <input
            type="text"
            name="name"
            value={tool.name}
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label>Category: </label>
          <input
            type="text"
            name="category"
            value={tool.category}
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label>Initial Stock: </label>
          <input
            type="number"
            name="stock"
            value={tool.stock}
            onChange={handleChange}
            required
            min="0" // 0 allowed (a tool can be registered before receiving stock)
          />
        </div>

        <div>
          <label>Replacement Value: </label>
          <input
            type="number"
            name="replacementValue"
            value={tool.replacementValue}
            onChange={handleChange}
            required
            min="1000" // same rule as the backend
          />
        </div>

        <button type="submit" style={{ marginTop: '15px' }}>Save</button>
      </form>
    </div>
  );
}

export default AddTool;