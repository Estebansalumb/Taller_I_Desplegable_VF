package presentacion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import datos.AccesoDatosException;
import datos.ConexionBD;
import datos.ProductoDAO;
import datos.ProductoDAOImpl;
import modelo.Producto;

/**
 * Capa de presentación web. Usa el servidor HTTP incluido en el JDK
 * (sin frameworks) y reutiliza sin cambios el modelo y la capa de datos.
 * Rutas:
 *   GET  /                   lista y búsqueda (RF2)
 *   POST /registrar          RF1
 *   POST /actualizar         RF3
 *   POST /eliminar           RF4 (confirmación en el navegador)
 */
public class ServidorWeb {

    private final ProductoDAO dao = new ProductoDAOImpl();

    public static void main(String[] args) throws IOException {
        try {
            ConexionBD.inicializar();
        } catch (AccesoDatosException e) {
            System.err.println("[ERROR FATAL] " + e.getMessage());
            System.exit(1);
        }
        // Render asigna el puerto en la variable PORT; en local se usa 8080.
        int puerto = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer servidor = HttpServer.create(new InetSocketAddress("0.0.0.0", puerto), 0);
        ServidorWeb app = new ServidorWeb();
        servidor.createContext("/", app::atender);
        servidor.start();
        System.out.println("Servidor iniciado en http://localhost:" + puerto);
    }

    private void atender(HttpExchange ex) throws IOException {
        try {
            String ruta = ex.getRequestURI().getPath();
            String metodo = ex.getRequestMethod();
            try {
                if (metodo.equals("GET") && ruta.equals("/")) {
                    mostrarInicio(ex);
                } else if (metodo.equals("POST") && ruta.equals("/registrar")) {
                    registrar(ex);
                } else if (metodo.equals("POST") && ruta.equals("/actualizar")) {
                    actualizar(ex);
                } else if (metodo.equals("POST") && ruta.equals("/eliminar")) {
                    eliminar(ex);
                } else {
                    responder(ex, 404, "<p>Página no encontrada. <a href='/'>Volver</a></p>");
                }
            } catch (AccesoDatosException e) {
                redirigir(ex, "error", "Error de base de datos: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                redirigir(ex, "error", "Dato inválido: " + e.getMessage());
            }
        } finally {
            ex.close(); // libera la conexión HTTP (compatible con Java 17)
        }
    }

    // ---------- RF1 ----------
    private void registrar(HttpExchange ex) throws IOException {
        Map<String, String> f = leerFormulario(ex);
        String codigo = f.getOrDefault("codigo", "");
        if (!codigo.isBlank() && dao.buscarPorCodigo(codigo).isPresent()) {
            redirigir(ex, "error", "Ya existe un producto con el código " + codigo.trim().toUpperCase() + ".");
            return;
        }
        Producto p = new Producto(codigo, f.get("nombre"), f.get("descripcion"),
                parsearPrecio(f.get("precio")), parsearCantidad(f.get("cantidad")));
        dao.insertar(p);
        redirigir(ex, "ok", "Producto " + p.getCodigo() + " registrado.");
    }

    // ---------- RF3 ----------
    private void actualizar(HttpExchange ex) throws IOException {
        Map<String, String> f = leerFormulario(ex);
        Optional<Producto> encontrado = dao.buscarPorCodigo(f.getOrDefault("codigo", ""));
        if (encontrado.isEmpty()) {
            redirigir(ex, "error", "El producto no existe.");
            return;
        }
        Producto p = encontrado.get();
        p.setDescripcion(f.get("descripcion"));
        p.setPrecio(parsearPrecio(f.get("precio")));
        p.setCantidad(parsearCantidad(f.get("cantidad")));
        dao.actualizar(p);
        redirigir(ex, "ok", "Producto " + p.getCodigo() + " actualizado.");
    }

    // ---------- RF4 ----------
    private void eliminar(HttpExchange ex) throws IOException {
        String codigo = leerFormulario(ex).getOrDefault("codigo", "");
        if (dao.eliminar(codigo)) {
            redirigir(ex, "ok", "Producto " + codigo + " eliminado.");
        } else {
            redirigir(ex, "error", "No se encontró el código " + codigo + ".");
        }
    }

    // ---------- RF2 ----------
    private void mostrarInicio(HttpExchange ex) throws IOException {
        Map<String, String> q = parsear(ex.getRequestURI().getRawQuery());
        String buscar = q.getOrDefault("q", "").trim();
        String tipo = q.getOrDefault("tipo", "nombre");
        String editar = q.getOrDefault("editar", "");

        // La consulta se maneja aquí (no con redirección) para evitar un ciclo
        // de redirecciones si la base de datos no responde.
        List<Producto> lista;
        String errorConsulta = null;
        try {
            if (buscar.isEmpty()) {
                lista = dao.listarTodos();
            } else if (tipo.equals("codigo")) {
                lista = dao.buscarPorCodigo(buscar).map(List::of).orElse(List.of());
            } else {
                lista = dao.buscarPorNombre(buscar);
            }
        } catch (AccesoDatosException e) {
            lista = List.of();
            errorConsulta = "Error de base de datos: " + e.getMessage();
        }

        StringBuilder h = new StringBuilder();
        if (q.containsKey("ok")) {
            h.append("<p class='msg ok'>").append(esc(q.get("ok"))).append("</p>");
        }
        if (q.containsKey("error")) {
            h.append("<p class='msg error'>").append(esc(q.get("error"))).append("</p>");
        }
        if (errorConsulta != null) {
            h.append("<p class='msg error'>").append(esc(errorConsulta)).append("</p>");
        }

        h.append("<section><h2>Registrar producto</h2>")
         .append("<form method='post' action='/registrar' class='registro'>")
         .append("<label>Código<input name='codigo' maxlength='20' required placeholder='GTR-001'></label>")
         .append("<label>Nombre<input name='nombre' maxlength='100' required></label>")
         .append("<label class='ancho'>Descripción<input name='descripcion' maxlength='255'></label>")
         .append("<label>Precio (COP)<input name='precio' type='number' min='0' max='9999999999.99' step='0.01' required></label>")
         .append("<label>Cantidad<input name='cantidad' type='number' min='0' step='1' required></label>")
         .append("<button>Registrar</button></form></section>");

        h.append("<section><h2>Inventario</h2>")
         .append("<form method='get' action='/' class='busqueda'>")
         .append("<select name='tipo'>")
         .append("<option value='nombre'").append(tipo.equals("nombre") ? " selected" : "").append(">Por nombre</option>")
         .append("<option value='codigo'").append(tipo.equals("codigo") ? " selected" : "").append(">Por código</option>")
         .append("</select><input name='q' value='").append(esc(buscar)).append("' placeholder='Buscar...'>")
         .append("<button>Buscar</button>");
        if (!buscar.isEmpty()) {
            h.append("<a href='/'>Ver todos</a>");
        }
        h.append("</form>");

        if (lista.isEmpty()) {
            h.append("<p>No hay productos para mostrar.</p>");
        } else {
            h.append("<div class='tabla'><table><thead><tr><th>Código</th><th>Nombre</th><th>Descripción</th>")
             .append("<th class='num'>Precio</th><th class='num'>Cant.</th><th></th></tr></thead><tbody>");
            for (Producto p : lista) {
                h.append(p.getCodigo().equals(editar) ? filaEdicion(p) : fila(p));
            }
            h.append("</tbody></table></div><p class='total'>Total: ").append(lista.size()).append(" producto(s)</p>");
        }
        h.append("</section>");
        responder(ex, 200, h.toString());
    }

    private String fila(Producto p) {
        String c = esc(p.getCodigo());
        return "<tr><td>" + c + "</td><td>" + esc(p.getNombre()) + "</td><td>" + esc(p.getDescripcion())
             + "</td><td class='num'>" + p.getPrecio().toPlainString() + "</td><td class='num'>" + p.getCantidad()
             + "</td><td class='acciones'><a href='/?editar=" + url(p.getCodigo()) + "'>Editar</a>"
             + "<form method='post' action='/eliminar' onsubmit=\"return confirm('¿Confirma la eliminación de "
             + c + "?')\"><input type='hidden' name='codigo' value='" + c + "'><button class='peligro'>Eliminar</button></form></td></tr>";
    }

    private String filaEdicion(Producto p) {
        String c = esc(p.getCodigo());
        String form = "form='e-" + c + "'";
        return "<tr class='editando'><td>" + c + "</td><td>" + esc(p.getNombre()) + "</td>"
             + "<td><input " + form + " name='descripcion' maxlength='255' value='" + esc(p.getDescripcion()) + "'></td>"
             + "<td><input " + form + " name='precio' type='number' min='0' max='9999999999.99' step='0.01' required value='"
             + p.getPrecio().toPlainString() + "'></td>"
             + "<td><input " + form + " name='cantidad' type='number' min='0' step='1' required value='" + p.getCantidad() + "'></td>"
             + "<td class='acciones'><form id='e-" + c + "' method='post' action='/actualizar'>"
             + "<input type='hidden' name='codigo' value='" + c + "'><button>Guardar</button></form>"
             + "<a href='/'>Cancelar</a></td></tr>";
    }

    // ---------- Validación (misma regla que la consola) ----------
    private BigDecimal parsearPrecio(String s) {
        try {
            return new BigDecimal(s == null ? "" : s.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El precio debe ser un número.");
        }
    }

    private int parsearCantidad(String s) {
        try {
            return Integer.parseInt(s == null ? "" : s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La cantidad debe ser un número entero.");
        }
    }

    // ---------- Utilidades HTTP ----------
    private Map<String, String> leerFormulario(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return parsear(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static Map<String, String> parsear(String datos) {
        Map<String, String> m = new HashMap<>();
        if (datos == null || datos.isEmpty()) {
            return m;
        }
        for (String par : datos.split("&")) {
            int i = par.indexOf('=');
            if (i > 0) {
                m.put(URLDecoder.decode(par.substring(0, i), StandardCharsets.UTF_8),
                      URLDecoder.decode(par.substring(i + 1), StandardCharsets.UTF_8));
            }
        }
        return m;
    }

    private void redirigir(HttpExchange ex, String tipo, String mensaje) throws IOException {
        ex.getResponseHeaders().set("Location", "/?" + tipo + "=" + url(mensaje));
        ex.sendResponseHeaders(303, -1);
    }

    private void responder(HttpExchange ex, int estado, String cuerpo) throws IOException {
        byte[] bytes = plantilla(cuerpo).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(estado, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Escapa HTML para evitar inyección de código en la página. */
    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String plantilla(String cuerpo) {
        return "<!doctype html><html lang='es'><head><meta charset='utf-8'>"
             + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
             + "<title>Gestión de Productos - Tienda de Guitarras</title><style>"
             + ":root{--azul:#1d4ed8;--azul-osc:#1e3a8a;--rojo:#c8102e;--negro:#0b0b0b;--blanco:#ffffff}"
             + "*{box-sizing:border-box}body{margin:0;font-family:system-ui,sans-serif;background:#eef2f9;color:var(--negro)}"
             + "header{background:var(--negro);color:var(--blanco);padding:16px;border-bottom:5px solid var(--azul);"
             + "box-shadow:0 3px 0 var(--rojo)}header h1{margin:0;font-size:1.3rem}"
             + "header p{margin:4px 0 0;color:#cbd5e1;font-size:.9rem}main{max-width:1100px;margin:0 auto;padding:20px 16px}"
             + "section{background:var(--blanco);border:1px solid #d6dbe6;border-top:3px solid var(--azul);border-radius:6px;padding:16px;margin-bottom:16px}"
             + "h2{margin:0 0 12px;font-size:1.05rem;color:var(--negro)}label{display:flex;flex-direction:column;font-size:.85rem;gap:4px}"
             + "input,select{padding:7px;border:1px solid #b8c0d0;border-radius:4px;font:inherit;width:100%;background:var(--blanco)}"
             + "input:focus,select:focus{outline:2px solid var(--azul);border-color:var(--azul)}a{color:var(--azul)}"
             + "button{padding:7px 14px;border:0;border-radius:4px;background:var(--azul);color:var(--blanco);font:inherit;cursor:pointer}"
             + "button:hover{background:var(--azul-osc)}button.peligro{background:var(--rojo)}button.peligro:hover{background:#9b0d22}"
             + ".registro{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:10px;align-items:end}"
             + ".registro .ancho{grid-column:span 2}.busqueda{display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap;align-items:center}"
             + ".busqueda select{width:auto}.busqueda input{flex:1;min-width:160px}.tabla{overflow-x:auto}"
             + "table{width:100%;border-collapse:collapse;font-size:.9rem}th,td{padding:8px;border-bottom:1px solid #eee;text-align:left;vertical-align:middle}"
             + "th{background:var(--negro);color:var(--blanco)}.num{text-align:right;font-variant-numeric:tabular-nums}"
             + "tbody tr:hover{background:#f5f8ff}td:first-child{white-space:nowrap;font-weight:600}"
             + ".acciones{display:flex;gap:8px;align-items:center;white-space:nowrap}.acciones form{margin:0}"
             + "tr.editando{background:#e6eeff}.msg{padding:10px;border-radius:4px;margin:0 0 16px;border-left:5px solid}"
             + ".ok{background:#e6eeff;color:var(--azul-osc);border-color:var(--azul)}"
             + ".error{background:#fde8eb;color:#9b0d22;border-color:var(--rojo)}.total{color:#475569;font-size:.85rem}"
             + "@media(max-width:600px){.registro .ancho{grid-column:auto}}"
             + "</style></head><body><header><h1>Sistema de Gestión de Productos</h1>"
             + "<p>Tienda de guitarras eléctricas</p></header><main>" + cuerpo + "</main></body></html>";
    }
}
