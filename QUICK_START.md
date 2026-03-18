# Guía de Inicio Rápido - PharmaSys

## Requisitos Previos

Antes de comenzar, asegúrate de tener instalado:

1. **Java 17 o superior**
   - Descargar: https://adoptium.net/
   - Verificar: `java -version`

2. **Maven 3.6+**
   - Descargar: https://maven.apache.org/download.cgi
   - Verificar: `mvn -version`

3. **Node.js 18+ y npm**
   - Descargar: https://nodejs.org/
   - Verificar: `node -version` y `npm -version`

4. **PostgreSQL 12+**
   - Descargar: https://www.postgresql.org/download/
   - Verificar: `psql --version`

5. **Angular CLI 17**
   - Instalar: `npm install -g @angular/cli`
   - Verificar: `ng version`

## Instalación Paso a Paso

### 1. Configurar PostgreSQL

```powershell
# Crear la base de datos
psql -U postgres
CREATE DATABASE pharmasys_db;
\q

# Ejecutar script de inicialización
psql -U postgres -d pharmasys_db -f database/init.sql
```

### 2. Configurar Backend

```powershell
# Editar backend/src/main/resources/application.properties
# Ajustar credenciales de PostgreSQL:
#   spring.datasource.username=tu_usuario
#   spring.datasource.password=tu_contraseña
```

### 3. Iniciar la Aplicación

#### Opción A: Usando scripts (Windows)

```powershell
# Terminal 1 - Backend
.\start-backend.bat

# Terminal 2 - Frontend
.\start-frontend.bat
```

#### Opción B: Manual

```powershell
# Terminal 1 - Backend
cd backend
mvn clean install
mvn spring-boot:run

# Terminal 2 - Frontend  
cd frontend
npm install
npm start
```

### 4. Acceder a la Aplicación

- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080/api

## Credenciales por Defecto

```
Usuario: admin
Contraseña: admin123
```

## Problemas Comunes

### Error de conexión a PostgreSQL
- Verificar que PostgreSQL esté corriendo
- Comprobar credenciales en `application.properties`
- Verificar que el puerto 5432 esté disponible

### Error de compilación Maven
- Verificar versión de Java: debe ser 17+
- Limpiar caché: `mvn clean`
- Verificar conexión a internet para descargar dependencias

### Error en Angular
- Limpiar caché: `npm cache clean --force`
- Reinstalar: `rm -rf node_modules && npm install`
- Verificar versión de Node.js: debe ser 18+

### Puerto ya en uso
- Backend (8080): Cambiar en `application.properties`
- Frontend (4200): Cambiar en `angular.json` o usar `ng serve --port 4300`

## Verificar Instalación

### Backend
```powershell
# Debe retornar un JSON vacío [] o con datos
curl http://localhost:8080/api/productos
```

### Frontend
- Abrir navegador en http://localhost:4200
- Debe mostrar la página de inicio de PharmaSys

## Siguientes Pasos

1. Explorar los módulos del sistema
2. Crear categorías de productos
3. Registrar proveedores
4. Agregar productos al inventario
5. Registrar clientes
6. Procesar tu primera venta

## Soporte

Si encuentras problemas:
1. Revisar los logs de consola
2. Verificar que todos los servicios estén corriendo
3. Consultar la documentación completa en README.md

¡Disfruta usando PharmaSys!
