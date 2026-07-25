package com.sengame.gdxsynfig.synfig.gwt;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

final class GzipSupport {
    private GzipSupport() {
    }

    static InputStream unwrap(InputStream in) throws IOException {
        return new BufferedInputStream(in);
    }
}
