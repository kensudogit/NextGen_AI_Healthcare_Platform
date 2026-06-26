package com.nghealth.platform.service.pacs;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

/** DICOM (RGB / 圧縮含む) から PNG プレビューを生成 */
final class DicomPreviewGenerator {

    static {
        ImageIO.scanForPlugins();
    }

    private DicomPreviewGenerator() {}

    static byte[] toPng(byte[] dicom) {
        byte[] viaImageIo = renderWithImageIo(dicom);
        if (viaImageIo != null) {
            return viaImageIo;
        }
        return renderGrayscaleFallback(dicom);
    }

    private static byte[] renderWithImageIo(byte[] dicom) {
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("DICOM");
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(dicom))) {
                reader.setInput(input);
                BufferedImage image = reader.read(0, reader.getDefaultReadParam());
                if (image == null) {
                    return null;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                return baos.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static byte[] renderGrayscaleFallback(byte[] dicom) {
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(dicom))) {
            Attributes attrs = dis.readDataset(-1, -1);
            if (!attrs.containsValue(Tag.PixelData)) {
                return null;
            }
            int rows = attrs.getInt(Tag.Rows, 0);
            int cols = attrs.getInt(Tag.Columns, 0);
            if (rows <= 0 || cols <= 0) {
                return null;
            }
            byte[] pixels = attrs.getBytes(Tag.PixelData);
            BufferedImage img = new BufferedImage(cols, rows, BufferedImage.TYPE_BYTE_GRAY);
            int idx = 0;
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols && idx < pixels.length; x++, idx++) {
                    int gray = pixels[idx] & 0xFF;
                    int rgb = (gray << 16) | (gray << 8) | gray;
                    img.setRGB(x, y, rgb);
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
