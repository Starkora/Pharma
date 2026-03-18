-- Script de inicialización de base de datos PharmaSys

-- Ejecuta el script con: psql -v admin_password_hash='TU_HASH_BCRYPT'
\if :{?admin_password_hash}
\else
    \echo 'Falta la variable admin_password_hash. Ejemplo: psql -v admin_password_hash="TU_HASH_BCRYPT" -f database/init.sql'
    \quit 1
\endif

-- Crear Database
CREATE DATABASE IF NOT EXISTS pharmasys_db;

\c pharmasys_db;

-- Las tablas se crean automáticamente con JPA/Hibernate
-- Este script es para datos iniciales de prueba

-- Insertar categorías de ejemplo
WITH seed_constants AS (
    SELECT
        'Medicamentos' AS categoria_medicamentos,
    'Farmacéutica Central S.A.' AS proveedor_farmaceutica_central, -- NOSONAR
    'Distribuidora Médica' AS proveedor_distribuidora_medica, -- NOSONAR
    'admin' AS usuario_admin -- NOSONAR
)
INSERT INTO categorias (nombre, descripcion, activo, fecha_creacion)
SELECT seed_constants.categoria_medicamentos, 'Productos farmacéuticos y medicinas', true, NOW() FROM seed_constants
UNION ALL
SELECT 'Vitaminas', 'Suplementos vitamínicos y minerales', true, NOW()
UNION ALL
SELECT 'Cosméticos', 'Productos de belleza y cuidado personal', true, NOW()
UNION ALL
SELECT 'Higiene', 'Productos de higiene y cuidado personal', true, NOW()
UNION ALL
SELECT 'Equipos Médicos', 'Equipos y dispositivos médicos', true, NOW()
ON CONFLICT DO NOTHING;

-- Insertar proveedores de ejemplo
WITH seed_constants AS (
    SELECT
        'Farmacéutica Central S.A.' AS proveedor_farmaceutica_central,
        'Distribuidora Médica' AS proveedor_distribuidora_medica
)
INSERT INTO proveedores (nombre, ruc, direccion, telefono, email, persona_contacto, activo, fecha_creacion)
SELECT seed_constants.proveedor_farmaceutica_central, '20123456789', 'Av. Principal 123, Lima', '01-2345678', 'ventas@farmcentral.com', 'Juan Pérez', true, NOW() FROM seed_constants
UNION ALL
SELECT seed_constants.proveedor_distribuidora_medica, '20987654321', 'Jr. Comercio 456, Lima', '01-8765432', 'contacto@distmedica.com', 'María García', true, NOW() FROM seed_constants
UNION ALL
SELECT 'Laboratorios Unidos', '20456789123', 'Av. Industrial 789, Lima', '01-4567891', 'info@labunidos.com', 'Carlos Rodríguez', true, NOW()
ON CONFLICT DO NOTHING;

-- Insertar usuario administrador de ejemplo
-- Pasa el hash bcrypt por variable psql: -v admin_password_hash='...'
WITH seed_constants AS (
    SELECT 'admin' AS usuario_admin
)
INSERT INTO usuarios (username, password, nombre, apellido, email, telefono, activo, fecha_creacion)
SELECT seed_constants.usuario_admin, :'admin_password_hash', 'Administrador', 'Sistema', 'admin@pharmasys.com', '999888777', true, NOW()
FROM seed_constants
ON CONFLICT DO NOTHING;

-- Insertar roles para el usuario admin
WITH seed_constants AS (
    SELECT 'admin' AS usuario_admin
)
INSERT INTO usuario_roles (usuario_id, rol)
SELECT usuarios.id, roles.rol
FROM usuarios
JOIN seed_constants ON usuarios.username = seed_constants.usuario_admin
CROSS JOIN (VALUES ('ROLE_ADMIN'), ('ROLE_VENDEDOR')) AS roles(rol)
ON CONFLICT DO NOTHING;

-- Insertar clientes de ejemplo
INSERT INTO clientes (nombre, apellido, dni, direccion, telefono, email, activo, fecha_creacion) VALUES
('Juan', 'Pérez López', '12345678', 'Av. Los Olivos 123', '987654321', 'juan.perez@email.com', true, NOW()),
('María', 'García Sánchez', '87654321', 'Jr. Las Flores 456', '987654322', 'maria.garcia@email.com', true, NOW()),
('Carlos', 'Rodríguez Díaz', '45678912', 'Av. Central 789', '987654323', 'carlos.rodriguez@email.com', true, NOW())
ON CONFLICT DO NOTHING;

-- Insertar productos de ejemplo
WITH seed_constants AS (
    SELECT
        'Medicamentos' AS categoria_medicamentos,
        'Farmacéutica Central S.A.' AS proveedor_farmaceutica_central,
        'Distribuidora Médica' AS proveedor_distribuidora_medica
), referencias AS (
    SELECT
        (SELECT id FROM categorias WHERE nombre = seed_constants.categoria_medicamentos) AS categoria_medicamentos_id,
        (SELECT id FROM categorias WHERE nombre = 'Vitaminas') AS categoria_vitaminas_id,
        (SELECT id FROM categorias WHERE nombre = 'Higiene') AS categoria_higiene_id,
        (SELECT id FROM proveedores WHERE nombre = seed_constants.proveedor_farmaceutica_central) AS proveedor_farmaceutica_central_id,
        (SELECT id FROM proveedores WHERE nombre = seed_constants.proveedor_distribuidora_medica) AS proveedor_distribuidora_medica_id,
        (SELECT id FROM proveedores WHERE nombre = 'Laboratorios Unidos') AS proveedor_laboratorios_unidos_id
    FROM seed_constants
)
INSERT INTO productos (codigo, nombre, descripcion, categoria_id, proveedor_id, precio_compra, precio_venta, stock, stock_minimo, unidad_medida, requiere_receta, activo, fecha_creacion)
SELECT 'MED001', 'Paracetamol 500mg', 'Analgésico y antipirético', referencias.categoria_medicamentos_id, referencias.proveedor_farmaceutica_central_id, 5.00, 8.50, 100, 20, 'Caja x 20 tabletas', false, true, NOW() FROM referencias
UNION ALL
SELECT 'MED002', 'Ibuprofeno 400mg', 'Antiinflamatorio no esteroideo', referencias.categoria_medicamentos_id, referencias.proveedor_farmaceutica_central_id, 7.00, 12.00, 80, 15, 'Caja x 20 tabletas', false, true, NOW() FROM referencias
UNION ALL
SELECT 'VIT001', 'Vitamina C 1000mg', 'Suplemento vitamínico', referencias.categoria_vitaminas_id, referencias.proveedor_distribuidora_medica_id, 15.00, 25.00, 50, 10, 'Frasco x 60 tabletas', false, true, NOW() FROM referencias
UNION ALL
SELECT 'HIG001', 'Alcohol en Gel 500ml', 'Desinfectante de manos', referencias.categoria_higiene_id, referencias.proveedor_distribuidora_medica_id, 8.00, 14.00, 120, 30, 'Frasco', false, true, NOW() FROM referencias
UNION ALL
SELECT 'MED003', 'Amoxicilina 500mg', 'Antibiótico de amplio espectro', referencias.categoria_medicamentos_id, referencias.proveedor_laboratorios_unidos_id, 12.00, 20.00, 60, 15, 'Caja x 12 cápsulas', true, true, NOW() FROM referencias
ON CONFLICT DO NOTHING;

-- Nota: Las ventas y compras se registrarán a través de la aplicación
