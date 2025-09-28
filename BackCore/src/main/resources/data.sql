-- ======================
-- USERS (Épica 7)
-- ======================
INSERT INTO users (username, password, role) VALUES 
('admin', 'admin123', 'ADMIN'),
('employee', 'emp123', 'EMPLOYEE');

-- ======================
-- TARIFF (Épica 4)
-- ======================
INSERT INTO tariffs (daily_rent_fee, daily_late_fee, repair_fee) VALUES 
(5000, 2000, 1500);

-- ======================
-- CLIENTS (Épica 3)
-- ======================
INSERT INTO clients (name, rut, phone, email, status) VALUES 
('Juan Perez', '11.111.111-1', '912345678', 'juan.perez@example.com', 'ACTIVE'),
('Maria Gonzalez', '22.222.222-2', '987654321', 'maria.gonzalez@example.com', 'RESTRICTED');

-- ======================
-- TOOLS (Épica 1)
-- ======================
INSERT INTO tools (name, category, status, stock, replacement_value) VALUES
('Taladro Bosch', 'Electric Tools', 'AVAILABLE', 5, 45000),
('Martillo Stanley', 'Hand Tools', 'AVAILABLE', 10, 8000),
('Sierra Circular Makita', 'Electric Tools', 'REPAIRING', 0, 60000),
('Llave Francesa', 'Hand Tools', 'DECOMMISSIONED', 0, 5000);

-- ======================
-- LOANS (Épica 2)
-- ======================
-- Un préstamo activo (vigente)
INSERT INTO loans (client_id, tool_id, start_date, due_date, status, total_penalty)
VALUES (1, 1, CURRENT_DATE - INTERVAL '2 days', CURRENT_DATE + INTERVAL '3 days', 'ACTIVE', 0);

-- Un préstamo atrasado
INSERT INTO loans (client_id, tool_id, start_date, due_date, status, total_penalty)
VALUES (1, 2, CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE - INTERVAL '5 days', 'LATE', 0);

-- Un préstamo ya cerrado
INSERT INTO loans (client_id, tool_id, start_date, due_date, return_date, status, total_penalty)
VALUES (2, 1, CURRENT_DATE - INTERVAL '20 days', CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE - INTERVAL '9 days', 'CLOSED', 5000);

-- ======================
-- KARDEX (Épica 5)
-- Registro de ingresos y préstamo
-- ======================
INSERT INTO kardex (tool_id, type, movement_date, quantity, user_id) VALUES
(1, 'INCOME', NOW() - INTERVAL '5 days', 5, 1),
(2, 'INCOME', NOW() - INTERVAL '5 days', 10, 1),
(1, 'LOAN',   NOW() - INTERVAL '2 days', 1, 2);
