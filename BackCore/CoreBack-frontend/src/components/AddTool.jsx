import { useState } from "react";
import ToolService from "../services/tool.service";
import { useNavigate } from "react-router-dom";

function AddTool() {
  const [tool, setTool] = useState({
    name: "",
    category: "",
    replacementValue: 0,
    stock: 0
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

    if (tool.stock <= 0 || tool.replacementValue <= 0) {
        setError("Stock and Replacement Value cannot be negative.");
        return;
    }

    ToolService.create(tool)
      .then(() => {
        navigate("/tools");
      })
      .catch((err) => {
        console.error("Error creating tool:", err);
        // Mostrar mensaje de error del backend si existe
        const errorMsg = err.response?.data?.message || err.response?.data || "Failed to create tool. Please check the data.";
        setError(errorMsg);
      });
  };

  return (
    <div style={{ padding: 16 }}>
      <h2>Add Tool</h2>
      {error && <p style={{ color: 'red' }}>Error: {error}</p>} {/* Mostrar error */}
      <form onSubmit={handleSubmit}>
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
            min="1" 
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
             min="1000"
          />
        </div>

        <button type="submit" style={{ marginTop: '15px' }}>Save</button> {/* Añadir margen */}
      </form>
    </div>
  );
}

export default AddTool;