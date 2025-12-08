package com.example.confer.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;

import com.example.confer.model.Usuario;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class ReportePDFService {

    public ByteArrayInputStream generarReporteUsuarios(List<Usuario> usuarios, String rol) {
        Document documento = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, out);
            documento.open();

            // 👉 Logo (cargar desde classpath)
            try (InputStream is = getClass().getResourceAsStream("/static/img/logo.png")) {
                if (is != null) {
                    byte[] logoBytes = is.readAllBytes();
                    Image logo = Image.getInstance(logoBytes);
                    logo.scaleToFit(100, 100);
                    logo.setAlignment(Image.ALIGN_CENTER);
                    documento.add(logo);
                } else {
                    System.out.println("Advertencia: logo no encontrado en classpath");
                }
            } catch (Exception e) {
                System.out.println("Advertencia: No se pudo cargar el logo");
            }

            // 👉 Título
            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph encabezado = new Paragraph("Reporte de Usuarios", titulo);
            encabezado.setAlignment(Element.ALIGN_CENTER);
            documento.add(encabezado);

            // 👉 Fecha actual
            Font fechaFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            String fechaActual = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            Paragraph fecha = new Paragraph("Generado el: " + fechaActual, fechaFont);
            fecha.setAlignment(Element.ALIGN_CENTER);
            documento.add(fecha);

            documento.add(Chunk.NEWLINE);

            // Filtrar por rol si se selecciona
            if (rol != null && !rol.isEmpty()) {
                int rolInt = 0;
                String tituloRol = "";
                switch (rol) {
                    case "admin": rolInt = 1; tituloRol = "Admin"; break;
                    case "cliente": rolInt = 2; tituloRol = "Usuario"; break;
                    case "vendedor": rolInt = 3; tituloRol = "Vendedor"; break;
                }
                agregarTablaPorRol(documento, usuarios, rolInt, tituloRol);
            } else {
                // Todos los roles
                agregarTablaPorRol(documento, usuarios, 1, "Admin");
                agregarTablaPorRol(documento, usuarios, 2, "Usuario");
                agregarTablaPorRol(documento, usuarios, 3, "Vendedor");
            }

            documento.add(Chunk.NEWLINE);

            // 👉 Gráfico de usuarios por rol
            Image graficoRoles = generarGraficoRoles(usuarios);
            if (graficoRoles != null) {
                graficoRoles.scaleToFit(420, 280);
                graficoRoles.setAlignment(Image.ALIGN_CENTER);
                documento.add(graficoRoles);
                documento.add(Chunk.NEWLINE);
            }

            // 👉 Código QR
            Paragraph textoQR = new Paragraph("Código QR de verificación:", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            textoQR.setSpacingBefore(20f);
            documento.add(textoQR);

            Image qr = generarQR("Reporte generado por Confer - " + fechaActual);
            qr.scaleToFit(100, 100);
            documento.add(qr);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void agregarTablaPorRol(Document doc, List<Usuario> usuarios, int rol, String tituloRol) throws DocumentException {
        List<Usuario> filtrados = usuarios.stream()
                .filter(u -> u.getIdRol() != null && u.getIdRol() == rol)
                .toList();

        if (filtrados.isEmpty()) return;

        Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Paragraph p = new Paragraph("Usuarios - " + tituloRol, subtitulo);
        p.setSpacingBefore(15f);
        doc.add(p);

        PdfPTable tabla;
        if (rol == 3) {
            tabla = new PdfPTable(7); // columnas específicas para vendedor
            tabla.setWidths(new float[]{2, 4, 3, 2.5f, 3, 3.5f, 2});
        } else {
            tabla = new PdfPTable(4);
            tabla.setWidths(new float[]{2, 4, 3, 2});
        }
        tabla.setWidthPercentage(110);
        tabla.setSpacingBefore(8f);
        tabla.setHeaderRows(1);

        // Encabezados
        if (rol == 3) {
            Stream.of("Nombre", "Correo", "Teléfono", "Empresa", "NIT", "Dirección", "Rol")
                    .forEach(h -> agregarCeldaEncabezado(tabla, h));
        } else {
            Stream.of("Nombre", "Correo", "Teléfono", "Rol")
                    .forEach(h -> agregarCeldaEncabezado(tabla, h));
        }

        for (Usuario u : filtrados) {
            agregarCeldaDato(tabla, u.getNombre());
            agregarCeldaDato(tabla, u.getCorreo());
            agregarCeldaDato(tabla, u.getTelefono() != null ? u.getTelefono() : "");
            if (rol == 3) {
                agregarCeldaDato(tabla, u.getEmpresa() != null ? u.getEmpresa() : "");
                agregarCeldaDato(tabla, u.getNit() != null ? u.getNit() : "");
                agregarCeldaDato(tabla, u.getDireccion() != null ? u.getDireccion() : "");
            }
            agregarCeldaDato(tabla, tituloRol); // Rol explícito
        }

        doc.add(tabla);

        Paragraph total = new Paragraph("Total " + tituloRol + "s: " + filtrados.size(), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
        total.setSpacingAfter(10f);
        doc.add(total);
    }

    private void agregarCeldaEncabezado(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        celda.setBackgroundColor(BaseColor.LIGHT_GRAY);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(6f);
        tabla.addCell(celda);
    }

    private void agregarCeldaDato(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto != null ? texto : "", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        celda.setPadding(6f);
        tabla.addCell(celda);
    }

    private Image generarQR(String texto) throws WriterException, IOException, BadElementException {
        int size = 150;
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrWriter.encode(texto, BarcodeFormat.QR_CODE, size, size);

        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int grayValue = bitMatrix.get(x, y) ? 0 : 255;
                img.setRGB(x, y, (grayValue == 0 ? java.awt.Color.BLACK : java.awt.Color.WHITE).getRGB());
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return Image.getInstance(baos.toByteArray());
    }

    private Image generarGraficoRoles(List<Usuario> usuarios) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        long admins = usuarios.stream().filter(u -> u.getIdRol() != null && u.getIdRol() == 1).count();
        long clientes = usuarios.stream().filter(u -> u.getIdRol() != null && u.getIdRol() == 2).count();
        long vendedores = usuarios.stream().filter(u -> u.getIdRol() != null && u.getIdRol() == 3).count();

        if (admins == 0 && clientes == 0 && vendedores == 0) return null;

        if (admins > 0) dataset.setValue("Admin", admins);
        if (clientes > 0) dataset.setValue("Usuario", clientes);
        if (vendedores > 0) dataset.setValue("Vendedor", vendedores);

        JFreeChart chart = ChartFactory.createPieChart("Usuarios por rol", dataset, true, false, false);
        try {
            ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(chartOut, chart, 520, 320);
            return Image.getInstance(chartOut.toByteArray());
        } catch (Exception e) {
            return null; // si falla el gráfico, continuamos sin él
        }
    }
}
