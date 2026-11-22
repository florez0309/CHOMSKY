package Chomsky;

import java.awt.*;
import javax.swing.*;

/** Parte común de los 4 niveles: controles, ejemplos, botones y animación. */
public abstract class PanelBase extends JPanel {
    // Símbolos especiales escritos como códigos Unicode, para que no dependan de la codificación de Eclipse
    public static final String EPS = "\u03b5", DELTA = "\u03b4", IZQ = "\u22a2", DER = "\u22a3",
            OK = "\u2713", NO = "\u2717", PLAY = "\u25b6", PAUSA = "\u275a\u275a", FLECHA = "\u2192";

    protected final JComboBox<String> combo = new JComboBox<>();
    protected final JTextField campo = new JTextField(16);
    protected final JLabel estado = new JLabel("Listo");
    protected final JTextArea log = new JTextArea(6, 50);
    protected final JSlider vel = new JSlider(1, 10, 5);
    protected boolean terminado, aceptada;

    private final JPanel panelEj = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
    private final JButton bRun = new JButton(PLAY + " Ejecutar");
    private Timer timer;
    private JComponent vista;

    protected abstract String[] nombres();                 
    protected abstract String[][] ejemplos(int preset);    
    protected abstract void iniciar(int preset, String entrada);
    protected abstract void avanzar();                     

    protected void construir(JComponent centro) {
        vista = centro;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (String n : nombres()) combo.addItem(n);

        JButton bRe = new JButton("Reiniciar"), bPaso = new JButton("Paso \u203a");
        JPanel f1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        f1.add(new JLabel("Lenguaje:")); f1.add(combo);
        JPanel f2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        f2.add(new JLabel("Cadena:")); f2.add(campo);
        f2.add(bRe); f2.add(bPaso); f2.add(bRun);
        f2.add(new JLabel("Velocidad:")); f2.add(vel);
        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        norte.add(f1); norte.add(f2); norte.add(panelEj);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        estado.setFont(estado.getFont().deriveFont(Font.BOLD, 15f));
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(estado, BorderLayout.NORTH);
        sur.add(new JScrollPane(log), BorderLayout.CENTER);

        add(norte, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);

        bRe.addActionListener(e -> reiniciar());
        campo.addActionListener(e -> reiniciar());
        bPaso.addActionListener(e -> { detener(); paso(); });
        bRun.addActionListener(e -> ejecutar());
        combo.addActionListener(e -> cambiarLenguaje());
        cambiarLenguaje();
    }

    private void cambiarLenguaje() {
        String[][] ex = ejemplos(combo.getSelectedIndex());
        panelEj.removeAll();
        panelEj.add(new JLabel("Ejemplos:"));
        for (String[] e : ex) {
            JButton b = new JButton((e[0].isEmpty() ? EPS : e[0]) + (e[1].equals("1") ? " " + OK : " " + NO));
            b.setMargin(new Insets(1, 6, 1, 6));
            b.setToolTipText(e[1].equals("1") ? "Debería aceptarse" : "Debería rechazarse");
            b.addActionListener(a -> { campo.setText(e[0]); reiniciar(); });
            panelEj.add(b);
        }
        panelEj.revalidate(); panelEj.repaint();
        campo.setText(ex[0][0]);
        reiniciar();
    }

    protected void reiniciar() {
        detener();
        terminado = false; aceptada = false;
        log.setText("");
        iniciar(combo.getSelectedIndex(), campo.getText().trim());
        actualizar();
    }

    private void paso() {
        if (terminado) return;
        avanzar();
        actualizar();
    }

    private void ejecutar() {
        if (timer != null && timer.isRunning()) { detener(); return; }
        if (terminado) reiniciar();
        bRun.setText(PAUSA + " Pausar");
        timer = new Timer(1100 - vel.getValue() * 100, e -> {
            timer.setDelay(1100 - vel.getValue() * 100);
            paso();
            if (terminado) detener();
        });
        timer.start();
    }

    private void detener() {
        if (timer != null) timer.stop();
        bRun.setText(PLAY + " Ejecutar");
    }

    protected void log(String s) {
        log.append(s + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void actualizar() {
        if (terminado) {
            estado.setText(aceptada ? OK + " Cadena ACEPTADA" : NO + " Cadena RECHAZADA");
            estado.setForeground(aceptada ? new Color(20, 130, 60) : new Color(190, 40, 30));
        } else {
            estado.setText("Listo para ejecutar...");
            estado.setForeground(Color.DARK_GRAY);
        }
        vista.repaint();
    }

    // ---------- utilidades de dibujo ----------
    protected static void texto(Graphics2D g, String s, double cx, double cy) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) (cy + fm.getAscent() / 2.0 - 2));
    }

    protected static void flecha(Graphics2D g, double x, double y, double ang) {
        Polygon p = new Polygon();
        p.addPoint((int) x, (int) y);
        p.addPoint((int) (x - 11 * Math.cos(ang - 0.4)), (int) (y - 11 * Math.sin(ang - 0.4)));
        p.addPoint((int) (x - 11 * Math.cos(ang + 0.4)), (int) (y - 11 * Math.sin(ang + 0.4)));
        g.fillPolygon(p);
    }

    protected static void cinta(Graphics2D g, int ancho, int y, String[] c, int cur, int hechas, String q) {
        int w = 40, paso = w + 3, total = c.length * paso;
        int x0 = total <= ancho - 20 ? (ancho - total) / 2 : ancho / 2 - Math.max(cur, 0) * paso - w / 2;
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        for (int i = 0; i < c.length; i++) {
            int x = x0 + i * paso;
            if (x < -w || x > ancho) continue;
            Color fondo = new Color(238, 242, 245);
            if (c[i].equals(IZQ) || c[i].equals(DER)) fondo = new Color(205, 225, 245);
            if (i < hechas) fondo = new Color(215, 215, 215);
            if (i == cur) fondo = new Color(255, 210, 94);
            g.setColor(fondo); g.fillRoundRect(x, y, w, w, 6, 6);
            g.setColor(Color.DARK_GRAY); g.drawRoundRect(x, y, w, w, 6, 6);
            texto(g, c[i], x + w / 2.0, y + w / 2.0);
            if (i == cur) {
                int cx = x + w / 2;
                g.fillPolygon(new int[]{cx - 6, cx + 6, cx}, new int[]{y - 10, y - 10, y - 2}, 3);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                texto(g, q, cx, y - 20);
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
            }
        }
    }
}