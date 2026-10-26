package datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestiona la conexión a la base de datos emulada H2 (modo embebido en archivo).
 * Los datos se guardan en ./data/tienda_guitarras.mv.db, por lo que
 * persisten al cerrar y reiniciar la aplicación (RF5).
 */
public final class ConexionBD {

    // AUTO_SERVER no es necesario; MODE=MySQL acerca la sintaxis a MySQL.
    private static final String URL =
            "jdbc:h2:./data/tienda_guitarras;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
    private static final String USUARIO = "sa";
    private static final String CLAVE = "";

    private static final String DDL_PRODUCTOS =
            "CREATE TABLE IF NOT EXISTS productos ("
          + "  codigo      VARCHAR(20)   PRIMARY KEY,"
          + "  nombre      VARCHAR(100)  NOT NULL,"
          + "  descripcion VARCHAR(255),"
          + "  precio      DECIMAL(12,2) NOT NULL CHECK (precio >= 0),"
          + "  cantidad    INT           NOT NULL CHECK (cantidad >= 0)"
          + ")";

    private ConexionBD() { }

    /** Abre una conexión nueva. Quien la recibe es responsable de cerrarla. */
    public static Connection obtenerConexion() {
        try {
            return DriverManager.getConnection(URL, USUARIO, CLAVE);
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "No fue posible conectar con la base de datos. "
                  + "Verifique que lib/h2-*.jar esté en el classpath. Detalle: "
                  + e.getMessage(), e);
        }
    }

    /** Crea la tabla si no existe. Se invoca una vez al iniciar. */
    public static void inicializar() {
        try (Connection con = obtenerConexion();
             Statement st = con.createStatement()) {
            st.execute(DDL_PRODUCTOS);
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al crear la tabla productos: " + e.getMessage(), e);
        }
    }
}
