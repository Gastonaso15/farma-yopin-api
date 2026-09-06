# FarmaYopin API

Backend REST para la aplicación móvil **FarmaYopin**, desarrollado en **Spring Boot** con **Spring Security**, **JPA / Hibernate**, autenticación **JWT** y persistencia en **MySQL** corriendo en Docker.

Desarrollado bajo la metodología **Test-Driven Development (TDD)** con cobertura de pruebas unitarias y de integración directa contra la base de datos.

---

## Tecnologías Utilizadas

- **Java 21**
- **Spring Boot 4.x / 3.x**
- **Spring Data JPA & Hibernate**
- **Spring Security** (Autenticación Stateless con JWT)
- **MySQL 8.0** (Contenedor Docker)
- **Maven Wrapper (`./mvnw`)**
- **JUnit 5, Mockito & MockMvc** (Testing & TDD)
- **Lombok & Jackson**

---

## Arquitectura y Roles

La API está diseñada para dar soporte a dos tipos de usuarios:

- **Sin rol (Público)**: Registro de clientes y Login.
- **Cliente (`ROLE_CLIENTE`)**: Consulta de catálogo, gestión integral del carrito de compras, pago/checkout y visualización de su histórico de compras propio.
- **Administrador (`ROLE_ADMIN`)**: Gestión total del catálogo de productos (crear, editar, consultar detalle) y consulta del histórico de compras por producto.

---

## Puesta en Marcha

### 1. Prerrequisitos
- **Java 21** (o superior)
- **Docker** y **Docker Compose**

### 2. Levantar la Base de Datos MySQL
En la raíz del proyecto:
```bash
docker compose up -d
```
> La base de datos `farmayopin_db` quedará expuesta en el puerto `3306` con usuario `root` y contraseña `rootpassword`.

### 3. Ejecutar las Pruebas (TDD & Integración)
Para compilar y ejecutar las 47 pruebas unitarias y de integración contra MySQL:
```bash
./mvnw clean test
```

### 4. Iniciar la Aplicación
```bash
./mvnw spring-boot:run
```
La API estará disponible en: `http://localhost:8080`

---

## Documentación de Endpoints

### 1. Autenticación (`/api/auth`)

| Método | Endpoint | Rol Requerido | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/register` | Sin rol | Registra un nuevo usuario con rol `CLIENTE` y crea su carrito. |
| `POST` | `/api/auth/login` | Sin rol | Autentica credenciales y retorna el token JWT + rol. |
| `POST` | `/api/auth/logout` | Cliente / Admin | Invalida el token activo agregándolo a la lista negra. |

#### Ejemplo Registro (`POST /api/auth/register`):
```json
{
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "password": "password123"
}
```
**Respuesta (201 Created):**
```json
{
  "id": 1,
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "rol": "CLIENTE",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### Ejemplo Login (`POST /api/auth/login`):
```json
{
  "email": "juan@example.com",
  "password": "password123"
}
```
**Respuesta (200 OK):**
```json
{
  "id": 1,
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "rol": "CLIENTE",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### 2. Catálogo de Productos (`/api/productos`)

> Todas las peticiones requieren la cabecera: `Authorization: Bearer <token>`

| Método | Endpoint | Rol Requerido | Descripción |
|---|---|---|---|
| `GET` | `/api/productos` | Cliente / Admin | Lista todos los productos disponibles. |
| `GET` | `/api/productos/{id}` | Admin | Obtiene el detalle de un producto específico. |
| `POST` | `/api/productos` | Admin | Crea un nuevo producto en el catálogo. |
| `PUT` | `/api/productos/{id}` | Admin | Actualiza los datos y stock de un producto. |
| `GET` | `/api/productos/{id}/compras` | Admin | Consulta el histórico de compras de un producto (ventas realizadas). |

#### Ejemplo Crear Producto (`POST /api/productos`):
```json
{
  "nombre": "Ibuprofeno 600mg",
  "precio": 180.50,
  "detalle": "Caja x 20 comprimidos",
  "foto": "https://servidor/fotos/ibuprofeno.jpg",
  "stock": 50
}
```

#### Ejemplo Histórico de Compras de Producto (`GET /api/productos/{id}/compras`):
**Respuesta (200 OK):**
```json
[
  {
    "compraId": 10,
    "fecha": "2026-08-27T23:30:00",
    "cantidad": 3,
    "cliente": "Lucía Ramos",
    "precioUnitario": 180.50,
    "subtotal": 541.50
  }
]
```

---

### 3. Carrito de Compras (`/api/carrito`)

> Endpoints exclusivos para **Cliente** (`ROLE_CLIENTE`). Requieren `Authorization: Bearer <token>`.

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/carrito` | Obtiene los ítems del carrito activo del usuario y el total calculado. |
| `POST` | `/api/carrito/items` | Agrega un producto. Si ya existe en el carrito, acumula la cantidad. Valida stock. |
| `PUT` | `/api/carrito/items/{id}` | Edita la cantidad de un ítem. Si la cantidad es `0`, elimina el ítem. Valida stock. |
| `DELETE` | `/api/carrito/items/{id}` | Elimina el ítem indicado del carrito. |
| `POST` | `/api/carrito/pago` | **Checkout / Pago**: Valida stock, descuenta inventario, genera la compra y vacía el carrito. |

#### Ejemplo Agregar al Carrito (`POST /api/carrito/items`):
```json
{
  "productoId": 1,
  "cantidad": 2
}
```

#### Ejemplo Respuesta Carrito (`GET /api/carrito`):
```json
{
  "id": 1,
  "total": 361.00,
  "items": [
    {
      "id": 5,
      "productoId": 1,
      "nombreProducto": "Ibuprofeno 600mg",
      "foto": "https://servidor/fotos/ibuprofeno.jpg",
      "precioUnitario": 180.50,
      "cantidad": 2,
      "subtotal": 361.00
    }
  ]
}
```

---

### 4. Histórico de Compras (`/api/compras`)

> Endpoint exclusivo para **Cliente** (`ROLE_CLIENTE`). Requiere `Authorization: Bearer <token>`.

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/compras` | Obtiene el listado de compras realizadas por el cliente autenticado en orden descendente. |

#### Ejemplo Histórico de Compras (`GET /api/compras`):
```json
[
  {
    "id": 101,
    "fecha": "2026-08-27T23:35:00",
    "total": 361.00,
    "clienteNombre": "Lucía Ramos",
    "clienteEmail": "lucia@gmail.com",
    "items": [
      {
        "id": 1,
        "productoId": 1,
        "nombreProducto": "Ibuprofeno 600mg",
        "cantidad": 2,
        "precioUnitario": 180.50,
        "subtotal": 361.00
      }
    ]
  }
]
```

---

## Manejo de Errores

Todos los errores retornan una estructura uniforme en formato JSON:

```json
{
  "timestamp": "2026-08-27T23:30:00.000",
  "status": 400,
  "error": "Stock insuficiente",
  "mensaje": "Stock insuficiente para el producto 'Ibuprofeno 600mg'. Stock disponible: 5, cantidad solicitada: 10",
  "path": "/api/carrito/items",
  "detalles": null
}
```

---

## Estructura del Proyecto

```
farma-yopin-api/
├── docker-compose.yml              # Configuración del contenedor MySQL 8
├── pom.xml                         # Dependencias Maven y configuración del build
├── README.md
└── src/
    ├── main/
    │   ├── java/com/farmayopin/api/
    │   │   ├── config/             # Configuración de beans (Jackson, etc.)
    │   │   ├── controller/         # Controladores REST (/auth, /productos, /carrito, /compras)
    │   │   ├── dto/                # Data Transfer Objects para requests y responses
    │   │   ├── exception/          # Excepciones personalizadas y GlobalExceptionHandler
    │   │   ├── model/              # Entidades JPA (Usuario, Producto, Carrito, Compra, etc.)
    │   │   ├── repository/         # Repositorios Spring Data JPA
    │   │   ├── security/           # JWT Util, Filtro de autenticación y SecurityConfig
    │   │   ├── service/            # Capa de lógica de negocio
    │   │   └── FarmaYopinApiApplication.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/farmayopin/api/
            ├── controller/         # Pruebas unitarias de controladores con MockMvc
            ├── integration/        # Pruebas de integración con MySQL en Docker
            └── service/            # Pruebas unitarias de servicios con Mockito
```
