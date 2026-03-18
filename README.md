# PharmaSys - Sistema ERP para Gestión de Farmacia

Sistema completo de gestión para farmacias desarrollado con Java Spring Boot (Backend), Angular (Frontend) y PostgreSQL (Base de datos).

## Características Principales

- **Gestión de Productos**: Control completo de inventario con alertas de stock mínimo
- **Ventas**: Procesamiento de ventas con múltiples métodos de pago
- **Compras**: Registro de compras a proveedores
- **Clientes**: Administración de información de clientes
- **Proveedores**: Gestión de proveedores
- **Categorías**: Organización de productos por categorías
- **Reportes**: Estadísticas y análisis del negocio
- **Control de Stock**: Actualización automática de inventario
- **Historial**: Registro completo de todas las transacciones

## Requisitos Previos

### Backend
- Java 17 o superior
- Maven 3.6+
- PostgreSQL 12+

### Frontend
- Node.js 18+
- npm 9+
- Angular CLI 17+

## Instalación y Configuración

### 1. Base de Datos

```bash
# Crear la base de datos PostgreSQL
createdb pharmasys_db

# Ejecutar el script de inicialización
psql -d pharmasys_db -f database/init.sql
```

### 2. Backend (Spring Boot)

```bash
# Navegar al directorio del backend
cd backend

# Configurar las credenciales de PostgreSQL en src/main/resources/application.properties
# Editar las siguientes líneas según tu configuración:
spring.datasource.url=jdbc:postgresql://localhost:5432/pharmasys_db
spring.datasource.username=postgres
spring.datasource.password=tu_contraseña

# Compilar el proyecto
mvn clean install

# Ejecutar la aplicación
mvn spring-boot:run
```

El backend estará disponible en: `http://localhost:8080`

### 3. Frontend (Angular)

```bash
# Navegar al directorio del frontend
cd frontend

# Instalar dependencias
npm install

# Ejecutar el servidor de desarrollo
npm start
```

El frontend estará disponible en: `http://localhost:4200`

## Estructura del Proyecto

```
PharmaSys/
├── backend/                    # Aplicación Spring Boot
│   ├── src/main/java/com/pharmasys/
│   │   ├── model/             # Entidades JPA
│   │   ├── repository/        # Repositorios de datos
│   │   ├── service/           # Lógica de negocio
│   │   ├── controller/        # Controladores REST
│   │   └── config/            # Configuraciones
│   └── pom.xml                # Dependencias Maven
│
├── frontend/                   # Aplicación Angular
│   ├── src/app/
│   │   ├── models/            # Interfaces TypeScript
│   │   ├── services/          # Servicios HTTP
│   │   └── components/        # Componentes UI
│   └── package.json           # Dependencias npm
│
└── database/                   # Scripts SQL
    └── init.sql               # Datos iniciales
```

## API Endpoints

### Productos
- `GET /api/productos` - Listar todos los productos
- `GET /api/productos/activos` - Listar productos activos
- `GET /api/productos/{id}` - Obtener producto por ID
- `GET /api/productos/buscar?nombre={nombre}` - Buscar productos
- `GET /api/productos/bajo-stock` - Productos con stock bajo
- `POST /api/productos` - Crear producto
- `PUT /api/productos/{id}` - Actualizar producto
- `DELETE /api/productos/{id}` - Eliminar producto

### Ventas
- `GET /api/ventas` - Listar todas las ventas
- `GET /api/ventas/{id}` - Obtener venta por ID
- `GET /api/ventas/cliente/{clienteId}` - Ventas por cliente
- `GET /api/ventas/periodo?fechaInicio=...&fechaFin=...` - Ventas por período
- `POST /api/ventas` - Crear venta
- `PUT /api/ventas/{id}/cancelar` - Cancelar venta

### Clientes
- `GET /api/clientes` - Listar todos los clientes
- `GET /api/clientes/activos` - Listar clientes activos
- `GET /api/clientes/{id}` - Obtener cliente por ID
- `GET /api/clientes/buscar?termino={termino}` - Buscar clientes
- `POST /api/clientes` - Crear cliente
- `PUT /api/clientes/{id}` - Actualizar cliente
- `DELETE /api/clientes/{id}` - Eliminar cliente

### Proveedores, Categorías y Compras
Similar estructura a los endpoints anteriores.

## Usuario de Prueba

```
Usuario: admin
Contraseña: admin123
```

## Seguridad

- Autenticación mediante Spring Security
- Passwords encriptados con BCrypt
- CORS configurado para desarrollo
- JWT para autenticación de APIs (próximamente)

## Funcionalidades del Sistema

### Gestión de Inventario
- Registro de productos con código único
- Control de stock en tiempo real
- Alertas de stock mínimo
- Gestión de lotes y fechas de vencimiento
- Categorización de productos
- Vinculación con proveedores

### Punto de Venta
- Interfaz rápida para procesar ventas
- Búsqueda rápida de productos
- Cálculo automático de totales
- Soporte para múltiples métodos de pago
- Emisión de comprobantes
- Ventas con o sin cliente registrado

### Reportes y Análisis
- Ventas diarias, mensuales y anuales
- Productos más vendidos
- Análisis de inventario
- Compras por proveedor
- Utilidades del negocio

## Próximas Mejoras

- [ ] Sistema completo de autenticación con JWT
- [ ] Módulo de compras completamente funcional
- [ ] Generación de reportes en PDF
- [ ] Notificaciones de productos por vencer
- [ ] Dashboard con gráficos estadísticos
- [ ] Aplicación móvil
- [ ] Sistema de roles y permisos avanzado
- [ ] Backup automático de base de datos
- [ ] Integración con sistemas de facturación electrónica

## Contribución

Este es un proyecto de demostración. Si deseas contribuir:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## Licencia

Este proyecto es de código abierto y está disponible bajo la Licencia MIT.

## Contacto

Para consultas y soporte, contacta a través del repositorio del proyecto.

---

**Desarrollado con para la gestión eficiente de farmacias**
