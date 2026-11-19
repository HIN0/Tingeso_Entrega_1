import { useState, useEffect } from "react";
import ClientService from "../services/client.service";
import { useNavigate, useParams } from "react-router-dom";
import { getErrorMessage } from "../utils/apiError";

function EditClient() {
  const { id } = useParams(); // client ID from the URL
  const navigate = useNavigate();
  const [client, setClient] = useState({
    name: "",
    rut: "", // shown but not editable
    phone: "",
    email: ""
  });
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  // Load the client on mount
  useEffect(() => {
    ClientService.get(id)
      .then(response => {
        setClient(response.data); // includes the (read-only) RUT
        setLoading(false);
      })
      .catch(error => {
        console.error("Error fetching client:", error);
        setMessage("Error loading the client.");
        setLoading(false);
      });
  }, [id]);

  const handleChange = (e) => {
    // The RUT cannot be changed
    if (e.target.name !== "rut") {
        setClient({ ...client, [e.target.name]: e.target.value });
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setMessage("Updating client...");

    // Send only the editable fields
    const updateData = {
        name: client.name,
        phone: client.phone,
        email: client.email
    };

    ClientService.update(id, updateData)
      .then(() => {
        navigate("/clients"); // back to the list
      })
      .catch((error) => {
        console.error("Error updating client:", error);
        setMessage(`Error updating client: ${getErrorMessage(error)}`);
      });
  };

  if (loading) {
    return <p style={{ padding: 16 }}>Loading...</p>;
  }

  return (
    <div style={{ padding: 16 }}>
      <h2>Edit Client (ID: {id})</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Name: </label>
          <input
            type="text"
            name="name"
            value={client.name}
            onChange={handleChange}
            required
          />
        </div>
        <div>
          <label>RUT: </label>
          <input
            type="text"
            name="rut"
            value={client.rut}
            onChange={handleChange}
            required
            readOnly // read-only
            style={{ backgroundColor: '#eee' }} // greyed out: not editable
          />
        </div>
        <div>
          <label>Phone: </label>
          <input
            type="text"
            name="phone"
            value={client.phone}
            onChange={handleChange}
            required
          />
        </div>
        <div>
          <label>Email: </label>
          <input
            type="email"
            name="email"
            value={client.email}
            onChange={handleChange}
            required
          />
        </div>
        <button type="submit" style={{ marginTop: '15px' }}>Save Changes</button>
      </form>
      {message && <p style={{ color: message.startsWith("Error") ? 'red' : 'green' }}>{message}</p>}
    </div>
  );
}

export default EditClient;