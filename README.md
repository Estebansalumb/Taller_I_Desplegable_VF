# Sistema de Gestión de Productos para Tienda de guitarras eléctricas

Aplicación de consola en Java con JDBC y base de datos emulada H2 (embebida en archivo).

## Requisitos

- JDK 17 o superior
- VS Code con "Extension Pack for Java"
- Driver H2: descargar `h2-2.2.224.jar` desde
  https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar
  y copiarlo en la carpeta `lib/`

## Estructura

```
src/
  modelo/        Producto.java               (entidad + validaciones)
  datos/         ConexionBD.java             (conexión y creación de tabla)
                 ProductoDAO.java            (interfaz CRUD)
                 ProductoDAOImpl.java        (JDBC con PreparedStatement)
                 AccesoDatosException.java   (excepción de la capa de datos)
  presentacion/  MenuConsola.java            (interfaz de consola)
                 Main.java                   (punto de entrada)
lib/             h2-2.2.224.jar
data/            tienda_guitarras.mv.db      (se crea al primer arranque)
```

## Ejecución

- VS Code: abrir la carpeta, Run and Debug > "Sistema de Gestión de Productos".
- Windows: `ejecutar.bat`
- Linux/macOS: `sh ejecutar.sh`

## Tabla

```sql
CREATE TABLE IF NOT EXISTS productos (
  codigo      VARCHAR(20)   PRIMARY KEY,
  nombre      VARCHAR(100)  NOT NULL,
  descripcion VARCHAR(255),
  precio      DECIMAL(12,2) NOT NULL CHECK (precio >= 0),
  cantidad    INT           NOT NULL CHECK (cantidad >= 0)
);
```

## Cumplimiento de requisitos

| Requisito | Implementación |
|---|---|
| RF1 Registrar | `MenuConsola.registrar()` → `ProductoDAOImpl.insertar()` |
| RF2 Consultar | `listarTodos()`, `buscarPorCodigo()`, `buscarPorNombre()` (LIKE, sin distinguir mayúsculas) |
| RF3 Actualizar | `actualizar()`: precio, descripción o cantidad; Enter conserva el valor |
| RF4 Eliminar | `eliminar()` con confirmación S/N |
| RF5 Persistencia | H2 en archivo `./data/tienda_guitarras.mv.db` |
| PreparedStatement | Todas las consultas, incluidas las de lectura |
| Validaciones | Precio ≥ 0 y cantidad entera ≥ 0 en consola, en `Producto` y en `CHECK` de la tabla |
| Errores | `SQLException` envuelta en `AccesoDatosException`; código duplicado detectado por SQLState 23505 |
| Cierre de recursos | try-with-resources en `Connection`, `PreparedStatement`, `ResultSet` y `Scanner` |

## Git

```
git init
git add .
git commit -m "Sistema de Gestión de Productos - CRUD con JDBC y H2"
git branch -M main
git remote add origin https://github.com/<usuario>/gestion-guitarras.git
git push -u origin main
```

## Ejecución en línea (GitHub Codespaces)

1. En la página del repositorio pulse **Code** > pestaña **Codespaces** > **Create codespace on main**.
2. Espere a que cargue VS Code en el navegador (la primera vez tarda 1-2 minutos).
3. En la terminal ejecute: `sh ejecutar.sh`

## Versión web y despliegue en Render

La clase `presentacion.ServidorWeb` ofrece las mismas funciones (RF1-RF5) desde el navegador,
usando el servidor HTTP del JDK y la misma capa de datos. La versión de consola (`presentacion.Main`) se conserva.

Ejecución local: en VS Code elija la configuración "Versión web" y abra http://localhost:8080

Despliegue en Render:

1. Suba el proyecto a GitHub, incluidos `Dockerfile` y `lib/h2-2.2.224.jar`.
2. En https://render.com cree una cuenta con GitHub.
3. New > Web Service > seleccione el repositorio.
4. Language: Docker. Instance Type: Free. Pulse Deploy Web Service.
5. Al terminar, Render muestra la URL pública (https://<nombre>.onrender.com).

Limitación del plan gratuito: el sistema de archivos es temporal, por lo que los productos
se pierden cuando el servicio se reinicia, se redespliega o se suspende tras 15 minutos sin uso.
La persistencia (RF5) se demuestra en la ejecución local o en Codespaces.
