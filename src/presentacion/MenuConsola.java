package presentacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import datos.AccesoDatosException;
import datos.ProductoDAO;
import modelo.Producto;

/** Capa de presentación: interfaz por consola. No contiene SQL. */
public class MenuConsola {

    private final ProductoDAO dao;
    private final Scanner sc;

    public MenuConsola(ProductoDAO dao, Scanner sc) {
        this.dao = dao;
        this.sc = sc;
    }

    public void iniciar() {
        int opcion;
        do {
            mostrarMenu();
            opcion = leerEnteroEnRango("Opción: ", 0, 5);
            try {
                switch (opcion) {
                    case 1 -> registrar();
                    case 2 -> listarTodos();
                    case 3 -> buscar();
                    case 4 -> actualizar();
                    case 5 -> eliminar();
                    case 0 -> System.out.println("Saliendo del sistema. Datos guardados.");
                    default -> { }
                }
            } catch (AccesoDatosException e) {
                System.out.println("[ERROR BD] " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Dato inválido: " + e.getMessage() + " No se guardaron cambios.");
            }
        } while (opcion != 0);
    }

    private void mostrarMenu() {
        System.out.println();
        System.out.println("=== SISTEMA DE GESTIÓN DE PRODUCTOS - TIENDA DE GUITARRAS ===");
        System.out.println("1. Registrar producto");
        System.out.println("2. Listar todos los productos");
        System.out.println("3. Buscar producto (código o nombre)");
        System.out.println("4. Actualizar producto");
        System.out.println("5. Eliminar producto");
        System.out.println("0. Salir");
    }

    // ---------- RF1 ----------
    private void registrar() {
        System.out.println("\n-- Registrar producto --");
        String codigo = leerTextoObligatorio("Código (ej. GTR-001): ");
        if (dao.buscarPorCodigo(codigo).isPresent()) {
            System.out.println("Ya existe un producto con ese código.");
            return;
        }
        String nombre = leerTextoObligatorio("Nombre: ");
        String descripcion = leerTexto("Descripción: ");
        BigDecimal precio = leerPrecio("Precio (COP): ");
        int cantidad = leerCantidad("Cantidad disponible: ");

        try {
            dao.insertar(new Producto(codigo, nombre, descripcion, precio, cantidad));
            System.out.println("Producto registrado correctamente.");
        } catch (IllegalArgumentException e) {
            System.out.println("Dato inválido: " + e.getMessage());
        }
    }

    // ---------- RF2 ----------
    private void listarTodos() {
        System.out.println("\n-- Inventario --");
        imprimirTabla(dao.listarTodos());
    }

    private void buscar() {
        System.out.println("\n-- Buscar --");
        System.out.println("1. Por código");
        System.out.println("2. Por nombre");
        int tipo = leerEnteroEnRango("Tipo de búsqueda: ", 1, 2);
        if (tipo == 1) {
            String codigo = leerTextoObligatorio("Código: ");
            dao.buscarPorCodigo(codigo).ifPresentOrElse(
                    p -> imprimirTabla(List.of(p)),
                    () -> System.out.println("No se encontró el código " + codigo + "."));
        } else {
            String nombre = leerTextoObligatorio("Nombre o parte del nombre: ");
            imprimirTabla(dao.buscarPorNombre(nombre));
        }
    }

    // ---------- RF3 ----------
    private void actualizar() {
        System.out.println("\n-- Actualizar producto --");
        String codigo = leerTextoObligatorio("Código del producto: ");
        Optional<Producto> encontrado = dao.buscarPorCodigo(codigo);
        if (encontrado.isEmpty()) {
            System.out.println("No se encontró el código " + codigo + ".");
            return;
        }
        Producto p = encontrado.get();
        imprimirTabla(List.of(p));
        System.out.println("Deje el campo vacío y presione Enter para conservar el valor actual.");

        String desc = leerTexto("Nueva descripción [" + p.getDescripcion() + "]: ");
        if (!desc.isBlank()) {
            p.setDescripcion(desc);
        }
        BigDecimal precio = leerPrecioOpcional("Nuevo precio [" + p.getPrecio().toPlainString() + "]: ");
        if (precio != null) {
            p.setPrecio(precio);
        }
        Integer cantidad = leerCantidadOpcional("Nueva cantidad [" + p.getCantidad() + "]: ");
        if (cantidad != null) {
            p.setCantidad(cantidad);
        }

        if (dao.actualizar(p)) {
            System.out.println("Producto actualizado.");
        } else {
            System.out.println("El producto ya no existe (pudo ser eliminado).");
        }
    }

    // ---------- RF4 ----------
    private void eliminar() {
        System.out.println("\n-- Eliminar producto --");
        String codigo = leerTextoObligatorio("Código del producto: ");
        Optional<Producto> encontrado = dao.buscarPorCodigo(codigo);
        if (encontrado.isEmpty()) {
            System.out.println("No se encontró el código " + codigo + ".");
            return;
        }
        imprimirTabla(List.of(encontrado.get()));
        if (confirmar("¿Confirma la eliminación? (S/N): ")) {
            System.out.println(dao.eliminar(codigo) ? "Producto eliminado." : "No se pudo eliminar.");
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    // ---------- Utilidades de salida ----------
    private void imprimirTabla(List<Producto> lista) {
        if (lista.isEmpty()) {
            System.out.println("No hay productos para mostrar.");
            return;
        }
        System.out.printf("%-10s | %-28s | %14s | %5s | %s%n",
                "CÓDIGO", "NOMBRE", "PRECIO", "CANT", "DESCRIPCIÓN");
        System.out.println("-".repeat(90));
        lista.forEach(System.out::println);
        System.out.println("Total: " + lista.size() + " producto(s).");
    }

    // ---------- Utilidades de entrada y validación ----------
    private String leerTexto(String msg) {
        System.out.print(msg);
        return sc.hasNextLine() ? sc.nextLine().trim() : "";
    }

    private String leerTextoObligatorio(String msg) {
        while (true) {
            String s = leerTexto(msg);
            if (!s.isEmpty()) {
                return s;
            }
            System.out.println("Este campo es obligatorio.");
        }
    }

    private int leerEnteroEnRango(String msg, int min, int max) {
        while (true) {
            String s = leerTexto(msg);
            try {
                int n = Integer.parseInt(s);
                if (n >= min && n <= max) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
                // se informa abajo
            }
            System.out.println("Ingrese un número entre " + min + " y " + max + ".");
        }
    }

    private BigDecimal leerPrecio(String msg) {
        while (true) {
            BigDecimal p = parsearPrecio(leerTexto(msg));
            if (p != null) {
                return p;
            }
        }
    }

    private BigDecimal leerPrecioOpcional(String msg) {
        while (true) {
            String s = leerTexto(msg);
            if (s.isEmpty()) {
                return null;
            }
            BigDecimal p = parsearPrecio(s);
            if (p != null) {
                return p;
            }
        }
    }

    /** Retorna null y muestra el error si el precio es inválido o negativo. */
    private BigDecimal parsearPrecio(String s) {
        try {
            BigDecimal p = new BigDecimal(s.replace(",", "."));
            if (p.signum() < 0) {
                System.out.println("El precio no puede ser negativo.");
                return null;
            }
            if (p.compareTo(Producto.PRECIO_MAXIMO) > 0) {
                System.out.println("El precio supera el máximo permitido (9999999999.99).");
                return null;
            }
            return p;
        } catch (NumberFormatException e) {
            System.out.println("Precio inválido. Use solo números (ej. 2500000 o 2500000.50).");
            return null;
        }
    }

    private int leerCantidad(String msg) {
        while (true) {
            Integer c = parsearCantidad(leerTexto(msg));
            if (c != null) {
                return c;
            }
        }
    }

    private Integer leerCantidadOpcional(String msg) {
        while (true) {
            String s = leerTexto(msg);
            if (s.isEmpty()) {
                return null;
            }
            Integer c = parsearCantidad(s);
            if (c != null) {
                return c;
            }
        }
    }

    /** Acepta solo enteros >= 0 (rechaza decimales como 2.5). */
    private Integer parsearCantidad(String s) {
        try {
            int c = Integer.parseInt(s);
            if (c < 0) {
                System.out.println("La cantidad no puede ser negativa.");
                return null;
            }
            return c;
        } catch (NumberFormatException e) {
            System.out.println("La cantidad debe ser un número entero (sin decimales).");
            return null;
        }
    }

    private boolean confirmar(String msg) {
        while (true) {
            String s = leerTexto(msg).toUpperCase();
            if (s.equals("S") || s.equals("SI") || s.equals("SÍ")) {
                return true;
            }
            if (s.equals("N") || s.equals("NO")) {
                return false;
            }
            System.out.println("Responda S o N.");
        }
    }
}
