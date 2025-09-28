package com.example.clasificados;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;

@WebServlet("/ImprimeCupon")
public class ImprimeCupon extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1) Entrada: texto del usuario

        req.setCharacterEncoding("UTF-8");
        String texto = req.getParameter("texto");
        if (texto == null || texto.isBlank()) {
            texto = "Festival Capital"; // valor por defecto si no llega nada
        }


        // 2) Generar código aleatorio

        int codigo = ThreadLocalRandom.current().nextInt(1_000_000, 1_000_000_000);
        String codigoStr = "#" + codigo;


         // 3) Cargar imagen base desde /webapp/img/remera.jpg
         // (si no existe, generar una imagen blanca de fallback)

        BufferedImage base;
        try (InputStream is = getServletContext().getResourceAsStream("/img/remera.jpg")) {
            if (is == null) {
                base = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = base.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, base.getWidth(), base.getHeight());
                g.setColor(Color.LIGHT_GRAY);
                g.setFont(new Font("SansSerif", Font.BOLD, 28));
                g.drawString("No se encontró img/remera.jpg", 40, 60);
                g.dispose();
            } else {
                base = ImageIO.read(is);
                // Garantizar formato compatible con JPG (sin alpha)
                if (base.getType() != BufferedImage.TYPE_INT_RGB) {
                    BufferedImage tmp = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = tmp.createGraphics();
                    g.drawImage(base, 0, 0, null);
                    g.dispose();
                    base = tmp;
                }
            }
        }

        // 4) Dibujar textos sobre la imagen

        Graphics2D g2 = base.createGraphics();
        try {
            // Suavizado de bordes y de texto (mejor calidad)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = base.getWidth();
            int H = base.getHeight();

            // Área de referencia (franja inferior) para calcular posiciones relativas
            int margin = 30;
            int bandHeight = 120;
            Rectangle areaTexto = new Rectangle(
                    margin,
                    H - bandHeight - margin / 2,
                    W - 2 * margin,
                    bandHeight
            );

            // NOMBRE (texto del usuario)

            g2.setColor(Color.WHITE);                  // Color actual (tu versión)
            int padding = 20;
            int baseSize = 44, minSize = 18;
            Font fontUser = new Font("SansSerif", Font.BOLD, baseSize);
            g2.setFont(fontUser);

            // Reducir tamaño si no entra en el ancho disponible
            FontMetrics fm = g2.getFontMetrics();
            while (fm.stringWidth(texto) > (areaTexto.width - padding) && fontUser.getSize() > minSize) {
                fontUser = fontUser.deriveFont((float) (fontUser.getSize() - 2));
                g2.setFont(fontUser);
                fm = g2.getFontMetrics();
            }

            // Posiciones EXACTAS que usabas (no toco funcionamiento)
            int xUser = areaTexto.x + 370;                          // más a la izquierda
            int yUser = areaTexto.y + (areaTexto.height + fm.getAscent() - fm.getDescent()) / 2 - 200; // un poco más arriba
            g2.drawString(texto, xUser, yUser);

            /* ---- CÓDIGO DE RETIRO ---- */
            String etiquetaCodigo = "CÓDIGO DE RETIRO " + codigoStr;
            Font fontCode = new Font("SansSerif", Font.BOLD, 26);
            g2.setFont(fontCode);
            FontMetrics fmCode = g2.getFontMetrics();

            // Si no entra, reducir levemente el tamaño
            while (fmCode.stringWidth(etiquetaCodigo) > (areaTexto.width / 2) && fontCode.getSize() > 14) {
                fontCode = fontCode.deriveFont((float) (fontCode.getSize() - 1));
                g2.setFont(fontCode);
                fmCode = g2.getFontMetrics();
            }

            int codeWidth = fmCode.stringWidth(etiquetaCodigo);
            int xCode = areaTexto.x + areaTexto.width - padding - codeWidth - 60; // un poco a la izquierda
            int yCode = areaTexto.y + areaTexto.height - 200;                      // un poco más arriba
            g2.setColor(Color.WHITE);
            g2.drawString(etiquetaCodigo, xCode, yCode);

        } finally {
            g2.dispose();
        }

        /*---------------------------
         * 5) Salida: enviar JPG al navegador
         *---------------------------*/
        resp.setContentType("image/jpeg");
        resp.setHeader("Content-Disposition", "inline; filename=\"cupon.jpg\"");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        ImageIO.write(base, "jpg", resp.getOutputStream());
        resp.getOutputStream().flush();
    }
}