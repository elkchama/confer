package com.example.confer.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.confer.model.Producto;
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
public class ReporteProductoPDFService {

    private static final Logger logger = LoggerFactory.getLogger(ReporteProductoPDFService.class);
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    private static final String SIN_IMAGEN = "Sin imagen";

    public ByteArrayInputStream generarReporteProductos(List<Producto> productos) {
        Document documento = new Document(PageSize.A4.rotate(), 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, out);
            documento.open();

            // Logo
            try {
                Image logo = Image.getInstance("src/main/resources/static/img/logo.png");
                logo.scaleToFit(100, 100);
                logo.setAlignment(Image.ALIGN_CENTER);
                documento.add(logo);
            } catch (Exception e) {
                logger.warn("No se pudo cargar el logo: {}", e.getMessage());
            }

            // Título
            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph encabezado = new Paragraph("Reporte de Productos", titulo);
            encabezado.setAlignment(Element.ALIGN_CENTER);
            documento.add(encabezado);

            // Fecha
            Font fechaFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            String fechaActual = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            Paragraph fecha = new Paragraph("Generado el: " + fechaActual, fechaFont);
            fecha.setAlignment(Element.ALIGN_LEFT);
            documento.add(fecha);

            documento.add(Chunk.NEWLINE);

            // Tabla organizada
            PdfPTable tabla = new PdfPTable(6);
            tabla.setWidthPercentage(105);
            tabla.setWidths(new float[]{3, 4, 3, 3, 2.2f, 3});
            tabla.setSpacingBefore(10f);
            tabla.setHeaderRows(1);

            agregarCeldaEncabezado(tabla, "Vendedor");
            agregarCeldaEncabezado(tabla, "Nombre");
            agregarCeldaEncabezado(tabla, "Categoría");
            agregarCeldaEncabezado(tabla, "Marca");
            agregarCeldaEncabezado(tabla, "Precio");
            agregarCeldaEncabezado(tabla, "Imagen");

            for (Producto p : productos) {
                agregarCeldaDato(tabla, p.getVendedor() != null ? p.getVendedor().getNombre() : "N/A");
                agregarCeldaDato(tabla, p.getNombre());
                agregarCeldaDato(tabla, p.getCategoria());
                agregarCeldaDato(tabla, p.getMarca());

                String precio = p.getPrecio() != null ? "$" + PRICE_FORMAT.format(p.getPrecio()) : "N/A";
                agregarCeldaDato(tabla, precio, Element.ALIGN_RIGHT);

                tabla.addCell(crearCeldaImagenProducto(p.getImagenUrl()));
            }

            documento.add(tabla);

            // Gráfico: productos por categoría
            Image graficoCategorias = generarGraficoCategorias(productos);
            if (graficoCategorias != null) {
                graficoCategorias.scaleToFit(520, 320);
                graficoCategorias.setAlignment(Element.ALIGN_CENTER);
                documento.add(Chunk.NEWLINE);
                documento.add(graficoCategorias);
            }

            // QR
            documento.add(Chunk.NEWLINE);
            Paragraph textoQR = new Paragraph("Código QR de verificación:", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            textoQR.setSpacingBefore(20f);
            documento.add(textoQR);

            Image qr = generarQR("Reporte de productos generado por Confer - " + fechaActual);
            qr.scaleToFit(100, 100);
            documento.add(qr);

        } catch (IOException | WriterException | DocumentException e) {
            logger.error("Error al generar reporte", e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void agregarCeldaEncabezado(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        celda.setBackgroundColor(BaseColor.LIGHT_GRAY);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(6f);
        tabla.addCell(celda);
    }

    private void agregarCeldaDato(PdfPTable tabla, String texto) {
        agregarCeldaDato(tabla, texto, Element.ALIGN_LEFT);
    }

    private void agregarCeldaDato(PdfPTable tabla, String texto, int alignment) {
        PdfPCell celda = new PdfPCell(new Phrase(texto != null ? texto : "", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        celda.setPadding(6f);
        celda.setHorizontalAlignment(alignment);
        tabla.addCell(celda);
    }

    private PdfPCell crearCeldaImagenProducto(String imagenUrl) {
        final float maxLado = 70f;
        if (imagenUrl == null || imagenUrl.isBlank()) {
            return celdaTextoImagen(SIN_IMAGEN);
        }

        try {
            Image imagen = cargarImagen(imagenUrl);
            if (imagen == null) {
                return celdaTextoImagen(SIN_IMAGEN);
            }
            imagen.scaleToFit(maxLado, maxLado);
            PdfPCell celda = new PdfPCell(imagen, true);
            celda.setFixedHeight(maxLado + 10f);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
            celda.setPadding(4f);
            return celda;
        } catch (Exception e) {
            return celdaTextoImagen(SIN_IMAGEN);
        }
    }

    private PdfPCell celdaTextoImagen(String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9)));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setFixedHeight(70f);
        return celda;
    }

    private Image cargarImagen(String imagenUrl) {
        // 1) Ruta en uploads (sistema de archivos)
        try {
            Path rutaFs = Paths.get("uploads").resolve(imagenUrl);
            if (Files.exists(rutaFs)) {
                return Image.getInstance(rutaFs.toAbsolutePath().toString());
            }
        } catch (BadElementException | IOException e) {
            // Imagen no encontrada en sistema de archivos
        }

        // 2) Ruta en recursos estáticos dentro del jar (por si se despliega así)
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("static/uploads/" + imagenUrl)) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                return Image.getInstance(bytes);
            }
        } catch (BadElementException | IOException | NullPointerException e) {
            // Imagen no encontrada en recursos del jar
        }

        return null;
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

    private Image generarGraficoCategorias(List<Producto> productos) {
        Map<String, Long> conteo = productos.stream()
                .collect(Collectors.groupingBy(p -> p.getCategoria() != null ? p.getCategoria() : "Sin categoría",
                        Collectors.counting()));

        if (conteo.isEmpty()) return null;

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        conteo.forEach((categoria, cantidad) -> dataset.addValue(cantidad, "Productos", categoria));

        JFreeChart chart = ChartFactory.createBarChart(
                "Productos por categoría",
                "Categoría",
                "Cantidad",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                false,
                false
        );

        try {
            ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(chartOut, chart, 640, 360);
            return Image.getInstance(chartOut.toByteArray());
        } catch (IOException | DocumentException e) {
            return null;
        }
    }
}
