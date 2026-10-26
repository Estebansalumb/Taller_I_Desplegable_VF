package modelo;

import java.math.BigDecimal;

/**
 * Entidad de dominio: representa una guitarra o accesorio del inventario.
 * Las reglas de validación (RNF) viven aquí para que ninguna capa
 * pueda construir un producto en estado inválido.
 */
public final class Producto {

    /** Límite de la columna DECIMAL(12,2): 10 enteros y 2 decimales. */
    public static final BigDecimal PRECIO_MAXIMO = new BigDecimal("9999999999.99");

    private String codigo;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private int cantidad;

    public Producto(String codigo, String nombre, String descripcion,
                    BigDecimal precio, int cantidad) {
        setCodigo(codigo);
        setNombre(nombre);
        setDescripcion(descripcion);
        setPrecio(precio);
        setCantidad(cantidad);
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public int getCantidad() { return cantidad; }

    public void setCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código es obligatorio.");
        }
        if (codigo.trim().length() > 20) {
            throw new IllegalArgumentException("El código admite máximo 20 caracteres.");
        }
        this.codigo = codigo.trim().toUpperCase();
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (nombre.trim().length() > 100) {
            throw new IllegalArgumentException("El nombre admite máximo 100 caracteres.");
        }
        this.nombre = nombre.trim();
    }

    public void setDescripcion(String descripcion) {
        String d = (descripcion == null) ? "" : descripcion.trim();
        if (d.length() > 255) {
            throw new IllegalArgumentException("La descripción admite máximo 255 caracteres.");
        }
        this.descripcion = d;
    }

    public void setPrecio(BigDecimal precio) {
        if (precio == null) {
            throw new IllegalArgumentException("El precio es obligatorio.");
        }
        if (precio.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        if (precio.compareTo(PRECIO_MAXIMO) > 0) {
            throw new IllegalArgumentException("El precio supera el máximo permitido (9999999999.99).");
        }
        this.precio = precio.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public void setCantidad(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        }
        this.cantidad = cantidad;
    }

    @Override
    public String toString() {
        return String.format("%-10s | %-28s | %14s | %5d | %s",
                codigo, recortar(nombre, 28), precio.toPlainString(), cantidad, descripcion);
    }

    private static String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
