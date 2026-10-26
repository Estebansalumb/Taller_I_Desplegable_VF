package datos;

import java.util.List;
import java.util.Optional;

import modelo.Producto;

/** Contrato de acceso a datos para la entidad Producto (CRUD). */
public interface ProductoDAO {

    /** Create. Lanza AccesoDatosException si el código ya existe. */
    void insertar(Producto p);

    /** Read: todos los productos ordenados por código. */
    List<Producto> listarTodos();

    /** Read: búsqueda exacta por código. */
    Optional<Producto> buscarPorCodigo(String codigo);

    /** Read: búsqueda parcial, sin distinguir mayúsculas, por nombre. */
    List<Producto> buscarPorNombre(String fragmento);

    /** Update: precio, descripción y cantidad. Retorna true si existía. */
    boolean actualizar(Producto p);

    /** Delete por código. Retorna true si se eliminó. */
    boolean eliminar(String codigo);
}
