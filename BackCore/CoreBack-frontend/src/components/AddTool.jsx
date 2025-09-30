import { useState } from "react";
import ToolService from "../services/tool.service";
import { useNavigate } from "react-router-dom";

function AddTool() {
  const [tool, setTool] = useState({
    name: "",
    category: "",
    status: "AVAILABLE",
    replacementValue: 0
  });
  const navigate = useNavigate();

  const handleChange = (e) => {
    setTool({ ...tool, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    ToolService.create(tool)
      .then(() => {
        navigate("/tools");
      })
      .catch((e) => {
        console.error("Error creating tool:", e);
      });
  };

  return (
    <div>
      <h2>Add Tool</h2>
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
          <label>Status: </label>
          <select name="status" value={tool.status} onChange={handleChange}>
            <option value="AVAILABLE">Available</option>
            <option value="LOANED">Loaned</option>
            <option value="IN_REPAIR">In Repair</option>
            <option value="DECOMMISSIONED">Decommissioned</option>
          </select>
        </div>

        <div>
          <label>Replacement Value: </label>
          <input
            type="number"
            name="replacementValue"
            value={tool.replacementValue}
            onChange={handleChange}
            required
          />
        </div>

        <button type="submit">Save</button>
      </form>
    </div>
  );
}

export default AddTool;
