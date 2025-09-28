package com.example.ttps2025_entregable1_carballo;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

@WebServlet("/ImprimeEntrada")
public class ImprimeEntrada extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");

        // Parámetros
        String nombres  = req.getParameter("nombres");
        String apellido = req.getParameter("apellido");
        String dni      = req.getParameter("dni");
        String pelicula = req.getParameter("pelicula");

        // Random + stock de pochoclos
        ServletContext ctx = getServletContext();
        Integer stock = (Integer) ctx.getAttribute("cantPochoclos");
        boolean gana = false;
        if (stock != null && stock > 0 && ThreadLocalRandom.current().nextBoolean()) {
            ctx.setAttribute("cantPochoclos", stock - 1);
            gana = true;
        }

        // Imagen base
        try (InputStream is = ctx.getResourceAsStream("/img/cupon.jpg")) {
            BufferedImage img = ImageIO.read(is);
            Graphics2D g2 = img.createGraphics();

            // Texto en rojo, centrado
            g2.setColor(Color.RED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 50));
            int yBase = img.getHeight() - 180;
            drawCentered(g2, img, "Apellido y Nombres: " + apellido + ", " + nombres, yBase);
            drawCentered(g2, img, "DNI: " + dni, yBase + 60);

            // Texto del QR (base + final según 'gana')
            StringBuilder sb = new StringBuilder()
                    .append("Entrada para la película ").append(pelicula).append(".\n")
                    .append(nombres).append(" ").append(apellido).append(" , DNI: ").append(dni).append("\n");
            if (gana) {
                sb.append("¡¡Felicitaciones!!\n")
                        .append("Te ganaste una LATA DE POCHOCLOS. Podés retirarla con esta entrada");
            } else {
                sb.append("¡Seguí viniendo al CINE!");
            }
            String qrTexto = sb.toString();

            // QR
            int qrSize = 300;
            try {
                BitMatrix m = new QRCodeWriter().encode(qrTexto, BarcodeFormat.QR_CODE, qrSize, qrSize);
                BufferedImage qr = MatrixToImageWriter.toBufferedImage(m);
                int qrX = (img.getWidth()  - qr.getWidth())  / 2;
                int qrY = (img.getHeight() - qr.getHeight()) / 2 - 75;
                g2.drawImage(qr, qrX, qrY, null);
            } catch (WriterException ignored) { }

            g2.dispose();

            // Respuesta
            resp.setContentType("image/jpeg");
            resp.setHeader("Content-Disposition", "inline; filename=\"cupon.jpg\"");
            ImageIO.write(img, "jpg", resp.getOutputStream());
        }
    }

    // Helper mínimo para centrar horizontalmente
    private static void drawCentered(Graphics2D g2, BufferedImage img, String text, int y) {
        int x = (img.getWidth() - g2.getFontMetrics().stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }
}