package com.nghealth.platform.service.pacs;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.DicomInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

final class DicomAttributesReader {

    private DicomAttributesReader() {}

    static Attributes read(byte[] data) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(data))) {
            dis.readFileMetaInformation();
            return dis.readDataset(-1, -1);
        } catch (IOException e) {
            try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(data))) {
                return dis.readDataset(-1, -1);
            }
        }
    }

    static Attributes read(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return read(in.readAllBytes());
        }
    }
}
