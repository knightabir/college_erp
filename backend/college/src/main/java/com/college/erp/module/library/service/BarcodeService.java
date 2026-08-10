package com.college.erp.module.library.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

/**
 * 03-srs.md [LIB-002]: Code 128 barcode, generated in-process via ZXing — pure Java, no network
 * call (08-system-architecture.md §2.1). The image is generated on demand from the barcode string
 * rather than persisted through module.storage (which doesn't exist yet) — see
 * CatalogService.getBarcodeImage javadoc.
 */
@Service
public class BarcodeService {

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 80;

    public byte[] generateCode128Png(String content) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    content, BarcodeFormat.CODE_128, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException ex) {
            // Encoding a barcode string we generated ourselves should never fail — this is an
            // internal invariant violation, not a business-rule or client-input problem.
            throw new IllegalStateException("Failed to generate barcode image for: " + content, ex);
        }
    }
}
