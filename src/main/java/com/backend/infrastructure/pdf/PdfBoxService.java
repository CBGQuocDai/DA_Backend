package com.backend.infrastructure.pdf;

import com.backend.application.adapter.pdf.PdfDocument;
import com.backend.application.adapter.pdf.PdfService;
import org.springframework.stereotype.Component;

@Component
public class PdfBoxService implements PdfService {

    @Override
    public PdfDocument create() {
        return new PdfBoxDocument();
    }
}
