package datos;

/**
 * Excepción propia de la capa de datos. Envuelve SQLException para que
 * la capa de presentación no dependa de java.sql.
 */
public class AccesoDatosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public AccesoDatosException(String mensaje) {
        super(mensaje);
    }
}
