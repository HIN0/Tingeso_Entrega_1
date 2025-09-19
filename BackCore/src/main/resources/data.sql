-- ======================
-- USERS (Épica 7)
-- ======================
INSERT INTO users (id, username, password, role) VALUES 
(1, 'admin', 'admin123', 'ADMIN'),
(2, 'employee', 'emp123', 'EMPLOYEE');

-- ======================
-- TARIFF (Épica 4)
-- ======================
INSERT INTO tariffs (id, daily_rent_fee, daily_late_fee, repair_fee) VALUES 
(1, 5000, 2000, 1500);

-- ======================
-- CLIENTS (Épica 3)
-- ======================
INSERT INTO clients (id, name, rut, phone, email, status) VALUES 
(1, 'Juan Perez', '11.111.111-1', '912345678', 'juan.perez@example.com', 'ACTIVE'),
(2, 'Maria Gonzalez', '22.222.222-2', '987654321', 'maria.gonzalez@example.com', 'RESTRICTED');

-- ======================
-- TOOLS (Épica 1)
-- ======================
INSERT INTO tools (id, name, category, status, stock, replacement_value) VALUES
(1, 'Taladro Bosch', 'Electric Tools', 'AVAILABLE', 5, 45000),
(2, 'Martillo Stanley', 'Hand Tools', 'AVAILABLE', 10, 8000),
(3, 'Sierra Circular Makita', 'Electric Tools', 'REPAIRING', 0, 60000),
(4, 'Llave Francesa', 'Hand Tools', 'DECOMMISSIONED', 0, 5000);

-- ======================
-- LOANS (Épica 2)
-- Un préstamo activo (vigente)
-- Un préstamo atrasado
-- ======================
INSERT INTO loans (id, client_id, tool_id, start_date, due_date, return_date, status, total_penalty) VALUES 
(1, 1, 1, CURRENT_DATE - 2, CURRENT_DATE + 3, NULL, 'ACTIVE', 0),
(2, 1, 2, CURRENT_DATE - 10, CURRENT_DATE - 5, NULL, 'LATE', 0);

-- ======================
-- KARDEX (Épica 5)
-- Registro de ingresos y préstamo
-- ======================
INSERT INTO kardex (id, tool_id, type, movement_date, quantity, user_id) VALUES
(1, 1, 'INCOME', NOW() - INTERVAL '5 days', 5, 1),
(2, 2, 'INCOME', NOW() - INTERVAL '5 days', 10, 1),
(3, 1, 'LOAN',   NOW() - INTERVAL '2 days', 1, 2);
