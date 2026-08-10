package com.college.erp.module.asset.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

/**
 * 03-srs.md [AST-001]: Code 128 barcode over the asset tag, generated in-process via ZXing — same
 * approach as module.library.service.BarcodeService. Duplicated rather than shared across modules
 * to keep the module boundary rule intact (07-coding-standards.md §4.4) — this is a small,
 * stateless value-add with no persistence, not a case that needs a cross-module accessor.
 */
@Service
public class AssetBarcodeService {

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
            throw new IllegalStateException("Failed to generate barcode image for: " + content, ex);
        }
    }
}
