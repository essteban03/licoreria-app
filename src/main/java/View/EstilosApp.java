package View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class EstilosApp {
    // Colores corporativos
    public static final Color COLOR_VINO = new Color(60, 0, 0);
    public static final Color COLOR_DORADO = new Color(212, 175, 55);
    public static final Color COLOR_FONDO = new Color(245, 245, 245);
    
    // Fuentes
    public static final Font FUENTE_TITULO = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FUENTE_NORMAL = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FUENTE_BOLD = new Font("Segoe UI", Font.BOLD, 14);

    /**
     * Crea una barra superior estandarizada con botón de volver.
     */
    public static JPanel crearHeader(String titulo, String usuario, ActionListener accionVolver) {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(COLOR_VINO);
        barra.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Botón Volver
        JButton btnVolver = new JButton("⟵ Volver");
        btnVolver.setBackground(COLOR_DORADO);
        btnVolver.setForeground(Color.BLACK);
        btnVolver.setFont(FUENTE_BOLD);
        btnVolver.setFocusPainted(false);
        if (accionVolver != null) {
            btnVolver.addActionListener(accionVolver);
        }

        // Título Central
        JLabel lblTitulo = new JLabel(titulo, JLabel.CENTER);
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(FUENTE_TITULO);

        // Info Usuario (Derecha)
        JLabel lblUser = new JLabel(usuario != null ? "Usuario: " + usuario : "");
        lblUser.setForeground(Color.WHITE);
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        // Panel derecho para equilibrar el botón de la izquierda
        JPanel panelDer = new JPanel(new BorderLayout());
        panelDer.setOpaque(false);
        panelDer.add(lblUser, BorderLayout.CENTER);
        // Un espacio vacío para que el título quede centrado
        panelDer.setPreferredSize(new Dimension(100, 30)); 

        barra.add(btnVolver, BorderLayout.WEST);
        barra.add(lblTitulo, BorderLayout.CENTER);
        barra.add(panelDer, BorderLayout.EAST);

        return barra;
    }
}