package com.sengame.gdxsynfig.synfig;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

final class GzipSupport {
    private static final int GZIP_MAGIC_BYTE_0 = 0x1f;
    private static final int GZIP_MAGIC_BYTE_1 = 0x8b;

    private GzipSupport() {
    }

    static InputStream unwrap(InputStream in) throws IOException {
        BufferedInputStream buffered = new BufferedInputStream(in);
        return isGzipped(buffered) ? new GZIPInputStream(buffered) : buffered;
    }

    private static boolean isGzipped(BufferedInputStream in) throws IOException {
        in.mark(2);
        int b0 = in.read();
        int b1 = in.read();
        in.reset();
        return b0 == GZIP_MAGIC_BYTE_0 && b1 == GZIP_MAGIC_BYTE_1;
    }
}
