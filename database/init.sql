-- Script de inicialización de base de datos PharmaSys

-- Crear Database
CREATE DATABASE IF NOT EXISTS pharmasys_db;

\c pharmasys_db;

-- Las tablas se crean automáticamente con JPA/Hibernate
-- Este script es para datos iniciales de prueba

-- Insertar categorías de ejemplo
INSERT INTO categorias (nombre, descripcion, activo, fecha_creacion) VALUES
('Medicamentos', 'Productos farmacéuticos y medicinas', true, NOW()),
('Vitaminas', 'Suplementos vitamínicos y minerales', true, NOW()),
('Cosméticos', 'Productos de belleza y cuidado personal', true, NOW()),
('Higiene', 'Productos de higiene y cuidado personal', true, NOW()),
('Equipos Médicos', 'Equipos y dispositivos médicos', true, NOW())
ON CONFLICT DO NOTHING;

-- Insertar proveedores de ejemplo
INSERT INTO proveedores (nombre, ruc, direccion, telefono, email, persona_contacto, activo, fecha_creacion) VALUES
('Farmacéutica Central S.A.', '20123456789', 'Av. Principal 123, Lima', '01-2345678', 'ventas@farmcentral.com', 'Juan Pérez', true, NOW()),
('Distribuidora Médica', '20987654321', 'Jr. Comercio 456, Lima', '01-8765432', 'contacto@distmedica.com', 'María García', true, NOW()),
('Laboratorios Unidos', '20456789123', 'Av. Industrial 789, Lima', '01-4567891', 'info@labunidos.com', 'Carlos Rodríguez', true, NOW())
ON CONFLICT DO NOTHING;

-- Insertar usuario administrador de ejemplo
-- Contraseña: admin123 (debe ser hasheada en producción con BCrypt)
INSERT INTO usuarios (username, password, nombre, apellido, email, telefono, activo, fecha_creacion) VALUES
('admin', '$2a$10$8jXDBXCBj3w9jvqN6hQgxuN4.qB8yZOZPxu9DZ6o6xvZGXq8QZ5Ym', 'Administrador', 'Sistema', 'admin@pharmasys.com', '999888777', true, NOW())
ON CONFLICT DO NOTHING;

-- Insertar roles para el usuario admin
INSERT INTO usuario_roles (usuario_id, rol) 
SELECT id, 'ROLE_ADMIN' FROM usuarios WHERE username = 'admin'
ON CONFLICT DO NOTHING;

INSERT INTO usuario_roles (usuario_id, rol) 
SELECT id, 'ROLE_VENDEDOR' FROM usuarios WHERE username = 'admin'
ON CONFLICT DO NOTHING;

-- Insertar clientes de ejemplo
INSERT INTO clientes (nombre, apellido, dni, direccion, telefono, email, activo, fecha_creacion) VALUES
('Juan', 'Pérez López', '12345678', 'Av. Los Olivos 123', '987654321', 'juan.perez@email.com', true, NOW()),
('María', 'García Sánchez', '87654321', 'Jr. Las Flores 456', '987654322', 'maria.garcia@email.com', true, NOW()),
('Carlos', 'Rodríguez Díaz', '45678912', 'Av. Central 789', '987654323', 'carlos.rodriguez@email.com', true, NOW())
ON CONFLICT DO NOTHING;

-- Insertar productos de ejemplo
INSERT INTO productos (codigo, nombre, descripcion, categoria_id, proveedor_id, precio_compra, precio_venta, stock, stock_minimo, unidad_medida, requiere_receta, activo, fecha_creacion)
SELECT 
    'MED001', 
    'Paracetamol 500mg', 
    'Analgésico y antipirético', 
    cat.id, 
    prov.id, 
    5.00, 
    8.50, 
    100, 
    20, 
    'Caja x 20 tabletas', 
    false, 
    true, 
    NOW()
FROM categorias cat, proveedores prov
WHERE cat.nombre = 'Medicamentos' AND prov.nombre = 'Farmacéutica Central S.A.'
ON CONFLICT DO NOTHING;

INSERT INTO productos (codigo, nombre, descripcion, categoria_id, proveedor_id, precio_compra, precio_venta, stock, stock_minimo, unidad_medida, requiere_receta, activo, fecha_creacion)
SELECT 
    'MED002', 
    'Ibuprofeno 400mg', 
    'Antiinflamatorio no esteroideo', 
    cat.id, 
    prov.id, 
    7.00, 
    12.00, 
    80, 
    15, 
    'Caja x 20 tabletas', 
    false, 
    true, 
    NOW()
FROM categorias cat, proveedores prov
WHERE cat.nombre = 'Medicamentos' AND prov.nombre = 'Farmacéutica Central S.A.'
ON CONFLICT DO NOTHING;

INSERT INTO productos (codigo, nombre, descripcion, categoria_id, proveedor_id, precio_compra, precio_venta, stock, stock_minimo, unidad_medida, requiere_receta, activo, fecha_creacion)
SELECT 
    'VIT001', 
    'Vitamina C 1000mg', 
    'Suplemento vitamínico', 
    cat.id, 
    prov.id, 
    15.00, 
    25.00, 
    50, 
    10, 
    'Frasco x 60 tabletas', 
    false, 
    true, 
    NOW()
FROM categorias cat, proveedores prov
WHERE cat.nombre = 'Vitaminas' AND prov.nombre = 'Distribuidora Médica'
ON CONFLICT DO NOTHING;

INSERT INTO productos (codigo, nombre, descripcion, categoria_id, proveedor_id, precio_compra, precio_venta, stock, stock_minimo, unidad_medida, requiere_receta, activo, fecha_creacion)
SELECT 
    'HIG001', 
    'Alcohol en Gel 500ml', 
    'Desinfectante de manos', 
    cat.id, 
    prov.id, 
    8.00, 
    14.00, 
    120, 
    30, 
    'Frasco', 
    false, 
    true, 
    NOW()
FROM categorias cat, proveedores prov
WHERE cat.nombre = 'Higiene' AND prov.nombre = 'Distribuidora Médica'
ON CONFLICT DO NOTHING;

INSERT INTO productos (codigo, nombre, descripcion, categoria_id, proveedor_id, precio_compra, precio_venta, stock, stock_minimo, unidad_medida, requiere_receta, activo, fecha_creacion)
SELECT 
    'MED003', 
    'Amoxicilina 500mg', 
    'Antibiótico de amplio espectro', 
    cat.id, 
    prov.id, 
    12.00, 
    20.00, 
    60, 
    15, 
    'Caja x 12 cápsulas', 
    true, 
    true, 
    NOW()
FROM categorias cat, proveedores prov
WHERE cat.nombre = 'Medicamentos' AND prov.nombre = 'Laboratorios Unidos'
ON CONFLICT DO NOTHING;

-- Nota: Las ventas y compras se registrarán a través de la aplicación
