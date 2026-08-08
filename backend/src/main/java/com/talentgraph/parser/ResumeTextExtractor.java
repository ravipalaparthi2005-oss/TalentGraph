package com.talentgraph.parser;

import java.io.InputStream;

public interface ResumeTextExtractor {

    boolean supports(String mimeType);

    String extractText(InputStream inputStream);
}
