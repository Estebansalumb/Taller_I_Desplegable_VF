package datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import modelo.Producto;

/**
 * Implementación JDBC del DAO. Todas las consultas usan PreparedStatement
 * y try-with-resources, que cierra ResultSet, PreparedStatement y Connection
 * automáticamente incluso si ocurre una excepción.
 */
public class ProductoDAOImpl implements ProductoDAO {

    private static final String SQL_INSERT =
            "INSERT INTO productos (codigo, nombre, descripcion, precio, cantidad) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_TODOS =
            "SELECT codigo, nombre, descripcion, precio, cantidad FROM productos ORDER BY codigo";
    private static final String SQL_SELECT_CODIGO =
            "SELECT codigo, nombre, descripcion, precio, cantidad FROM productos WHERE codigo = ?";
    private static final String SQL_SELECT_NOMBRE =
            "SELECT codigo, nombre, descripcion, precio, cantidad FROM productos "
          + "WHERE LOWER(nombre) LIKE ? ORDER BY nombre";
    private static final String SQL_UPDATE =
            "UPDATE productos SET descripcion = ?, precio = ?, cantidad = ? WHERE codigo = ?";
    private static final String SQL_DELETE =
            "DELETE FROM productos WHERE codigo = ?";

    // Código SQLState estándar para violación de clave primaria / única.
    private static final String SQLSTATE_DUPLICADO = "23505";

    @Override
    public void insertar(Producto p) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getDescripcion());
            ps.setBigDecimal(4, p.getPrecio());
            ps.setInt(5, p.getCantidad());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (SQLSTATE_DUPLICADO.equals(e.getSQLState())) {
                throw new AccesoDatosException("Ya existe un producto con el código " + p.getCodigo() + ".", e);
            }
            throw new AccesoDatosException("Error al registrar el producto: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Producto> listarTodos() {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT_TODOS);
             ResultSet rs = ps.executeQuery()) {
            return mapearLista(rs);
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al consultar los productos: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Producto> buscarPorCodigo(String codigo) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT_CODIGO)) {
            ps.setString(1, codigo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar por código: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Producto> buscarPorNombre(String fragmento) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT_NOMBRE)) {
            ps.setString(1, "%" + fragmento.trim().toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return mapearLista(rs);
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar por nombre: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(Producto p) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, p.getDescripcion());
            ps.setBigDecimal(2, p.getPrecio());
            ps.setInt(3, p.getCantidad());
            ps.setString(4, p.getCodigo());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al actualizar el producto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(String codigo) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
            ps.setString(1, codigo.trim().toUpperCase());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al eliminar el producto: " + e.getMessage(), e);
        }
    }

    private List<Producto> mapearLista(ResultSet rs) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapear(rs));
        }
        return lista;
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getBigDecimal("precio"),
                rs.getInt("cantidad"));
    }
}
