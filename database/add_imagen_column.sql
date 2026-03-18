-- =====================================================
-- Script: Agregar soporte para imágenes en PRODUCTOS
-- =====================================================

-- Verificar e agregar columna imagen_url si no existe
ALTER TABLE productos 
ADD COLUMN IF NOT EXISTS imagen_url VARCHAR(500) DEFAULT NULL;

-- Comentario para documentación
COMMENT ON COLUMN productos.imagen_url IS 'URL o ruta de la imagen del producto guardada en servidor: /uploads/productos/timestamp_nombre.jpg';

-- Crear índice para búsquedas rápidas
CREATE INDEX IF NOT EXISTS idx_productos_imagen ON productos(imagen_url);

-- Verificar estructura actualizada
SELECT 
    column_name, 
    data_type, 
    is_nullable
FROM information_schema.columns
WHERE table_name = 'productos'
ORDER BY ordinal_position;
