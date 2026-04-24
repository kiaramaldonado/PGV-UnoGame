package net.salesianos.client.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GameButton extends JButton {

    // Paleta de colores (Rojo vibrante estilo UNO)
    private final Color normalColor = new Color(228, 30, 38);   // Rojo base
    private final Color hoverColor = new Color(255, 70, 70);    // Rojo claro al pasar el ratón
    private final Color pressedColor = new Color(180, 20, 20);  // Rojo oscuro al hacer clic

    public GameButton(String text, String iconPath) {
        super(text);

        // --- CORRECCIÓN DEL FONDO BLANCO ---
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false); // Quitamos el borde por defecto que suele ser blanco/gris
        setOpaque(false);        // Fundamental para que Swing no pinte un cuadrado blanco detrás

        // --- ESTILO DEL TEXTO ---
        setForeground(Color.WHITE);
        setFont(new Font("Arial", Font.BOLD, 16));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // --- TAMAÑO Y MÁRGENES ---
        // Da espacio interior al botón para que no quede pegado al texto
        setPreferredSize(new Dimension(160, 120));

        // --- MANEJO DEL ICONO ---
        if (iconPath != null) {
            try {
                ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));
                setIcon(icon);
                setVerticalTextPosition(SwingConstants.BOTTOM);
                setHorizontalTextPosition(SwingConstants.CENTER);
                setIconTextGap(15); // Espacio entre el icono y el texto
            } catch (Exception e) {
                System.out.println("No se pudo cargar el icono: " + iconPath);
            }
        }

        // --- EFECTOS INTERACTIVOS ---
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(normalColor);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setBackground(hoverColor);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Usamos Graphics2D para poder suavizar los bordes (Antialiasing)
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Determinar el color actual según la interacción del ratón
        if (getModel().isPressed()) {
            g2.setColor(pressedColor);
        } else if (getModel().isRollover()) {
            g2.setColor(hoverColor);
        } else {
            g2.setColor(normalColor);
        }

        // Dibujar nuestro propio fondo con bordes redondeados
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        g2.dispose();

        // Finalmente, decirle a Java que pinte el texto y el icono por encima
        super.paintComponent(g);
    }
}