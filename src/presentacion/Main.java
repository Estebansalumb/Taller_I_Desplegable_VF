package presentacion;

import java.util.Scanner;

import datos.AccesoDatosException;
import datos.ConexionBD;
import datos.ProductoDAOImpl;

/** Punto de entrada del Sistema de Gestión de Productos. */
public class Main {

    public static void main(String[] args) {
        try {
            ConexionBD.inicializar();
        } catch (AccesoDatosException e) {
            System.err.println("[ERROR FATAL] " + e.getMessage());
            System.exit(1);
        }

        // try-with-resources cierra el Scanner (System.in) al salir.
        try (Scanner sc = new Scanner(System.in)) {
            new MenuConsola(new ProductoDAOImpl(), sc).iniciar();
        }
    }
}
