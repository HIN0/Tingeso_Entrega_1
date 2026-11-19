import { useState } from "react";
import ClientService from "../services/client.service";
import { useNavigate } from "react-router-dom";
import { getErrorMessage } from "../utils/apiError";

function AddClient() {
  const [client, setClient] = useState({
    name: "",
    rut: "",
    phone: "",
    email: ""
  });
  const [message, setMessage] = useState("");
  const navigate = useNavigate();

  const handleChange = (e) => {
    setClient({ ...client, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setMessage("Saving client...");

    ClientService.create(client)
      .then(() => {
        navigate("/clients");
      })
      .catch((error) => {
        console.error("Error creating client:", error);
        setMessage(`Error creating client: ${getErrorMessage(error)}`);
      });
  };

  return (
    <div style={{padding: 16}}>
      <h2>Register New Client</h2>
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
        <button type="submit" style={{marginTop: '15px'}}>Save Client</button>
      </form>
      {message && <p style={{ color: message.startsWith("Error") ? 'red' : 'green' }}>{message}</p>}
    </div>
  );
}

export default AddClient;