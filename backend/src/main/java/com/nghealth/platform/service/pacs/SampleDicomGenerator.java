package com.nghealth.platform.service.pacs;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.util.UIDUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** デモ・シード用の最小 DICOM（プレビュー可能なグレースケール画像付き） */
public final class SampleDicomGenerator {

    private static final int SIZE = 128;
    private static final String SOP_CLASS = UID.CTImageStorage;

    private SampleDicomGenerator() {}

    public static byte[] create(String studyUid, String modality, String bodyPart, String description) throws IOException {
        String sopInstanceUid = UIDUtils.createUID();
        Attributes attrs = new Attributes();
        attrs.setString(Tag.SOPClassUID, VR.UI, SOP_CLASS);
        attrs.setString(Tag.SOPInstanceUID, VR.UI, sopInstanceUid);
        attrs.setString(Tag.StudyInstanceUID, VR.UI, studyUid);
        attrs.setString(Tag.SeriesInstanceUID, VR.UI, studyUid + ".1");
        attrs.setString(Tag.Modality, VR.CS, modality != null ? modality : "OT");
        if (bodyPart != null && !bodyPart.isBlank()) {
            attrs.setString(Tag.BodyPartExamined, VR.CS, bodyPart);
        }
        if (description != null && !description.isBlank()) {
            attrs.setString(Tag.StudyDescription, VR.LO, description);
        }
        attrs.setString(Tag.StudyDate, VR.DA, LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        attrs.setInt(Tag.Rows, VR.US, SIZE);
        attrs.setInt(Tag.Columns, VR.US, SIZE);
        attrs.setInt(Tag.BitsAllocated, VR.US, 8);
        attrs.setInt(Tag.BitsStored, VR.US, 8);
        attrs.setInt(Tag.HighBit, VR.US, 7);
        attrs.setInt(Tag.SamplesPerPixel, VR.US, 1);
        attrs.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME2");
        attrs.setInt(Tag.PixelRepresentation, VR.US, 0);
        attrs.setBytes(Tag.PixelData, VR.OB, createSamplePixels());

        Attributes fmi = Attributes.createFileMetaInformation(
                sopInstanceUid, SOP_CLASS, UID.ExplicitVRLittleEndian);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DicomOutputStream dos = new DicomOutputStream(baos, UID.ExplicitVRLittleEndian)) {
            dos.writeDataset(fmi, attrs);
        }
        return baos.toByteArray();
    }

    private static byte[] createSamplePixels() {
        byte[] pixels = new byte[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int cx = x - SIZE / 2;
                int cy = y - SIZE / 2;
                int dist = (int) Math.sqrt(cx * cx + cy * cy);
                pixels[y * SIZE + x] = (byte) Math.max(0, 220 - dist * 4);
            }
        }
        return pixels;
    }
}
