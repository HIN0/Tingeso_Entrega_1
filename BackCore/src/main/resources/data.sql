-- ============================
-- DATOS DE PRUEBA
-- ============================

-- Usuarios
INSERT INTO users (username, password, role) VALUES
('admin', 'admin123', 'ADMIN'),
('empleado', 'empleado123', 'EMPLOYEE');

-- Clientes
INSERT INTO clients (rut, name, phone, email, status) VALUES
('12.345.678-9', 'Juan Pérez', '987654321', 'juan@mail.com', 'ACTIVE'),
('98.765.432-1', 'María López', '912345678', 'maria@mail.com', 'ACTIVE');

-- Herramientas
INSERT INTO tools (name, category, status, replacement_value, stock) VALUES
('Taladro Eléctrico', 'Eléctrico', 'AVAILABLE', 45000, 10),
('Martillo', 'Manual', 'AVAILABLE', 8000, 25),
('Sierra Circular', 'Eléctrico', 'AVAILABLE', 60000, 5);

-- Tarifas iniciales
INSERT INTO tariffs (daily_rate, late_fee_rate, repair_fee) VALUES
(3000, 5000, 1500);
