package app.lchanc3.extension.jptt;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Recognises PTT's DDoS/BOT detection turning a connection away, and recovers
 * what it said.
 *
 * <p>The detection answers before PTT has worked out the terminal's encoding,
 * and answers in UTF-8. JPTT reads the WebSocket and telnet transports as Big5
 * (UAO), so the screen shows garbage, nothing on it matches what JPTT knows, and
 * when PTT then closes the connection JPTT reconnects into it every few seconds.
 * Decoding the screen back is not possible: Big5 has no character for most of
 * the byte pairs UTF-8 makes, so they are already lost by then.
 *
 * <p>So the bytes are kept before anything decodes them. Each stream JPTT builds
 * its terminal reader on is wrapped in a {@link Tap} that copies the start of
 * the connection, which is where the detection's message is, and the tap is
 * kept against the socket it belongs to. When that socket loses its connection
 * before reaching the main menu, {@link #message(Object)} looks for the
 * detection's own name, which is ASCII, in what was copied.
 */
@SuppressWarnings("unused")
public final class BotBlockPatch {

    /** How much of each connection is kept. PTT's first screen fits many times over. */
    private static final int KEPT_BYTES = 16 * 1024;

    private static final byte[] MARKER = "DDoS/BOT".getBytes(StandardCharsets.US_ASCII);

    /** The tap of the connection each socket last made. */
    private static final Map<Object, Tap> TAPS = new WeakHashMap<>();

    /**
     * Called just before an {@code InputStreamReader} is built on {@code stream}
     * for the terminal. The reader reads through the returned tap.
     */
    public static InputStream tap(InputStream stream) {
        if (stream == null || stream instanceof Tap) {
            return stream;
        }
        try {
            return new Tap(stream);
        } catch (Throwable ex) {
            Log.e(JpttContext.LOG_TAG, "Could not tap the terminal stream", ex);
            return stream;
        }
    }

    /**
     * Called once the reader is stored in {@code socket}, with what
     * {@link #tap(InputStream)} returned. Gives back the stream it wrapped, so
     * the rest of the method goes on with the object it had.
     */
    public static InputStream attach(Object socket, InputStream stream) {
        if (!(stream instanceof Tap)) {
            return stream;
        }
        Tap tap = (Tap) stream;
        if (socket != null) {
            synchronized (TAPS) {
                TAPS.put(socket, tap);
            }
        }
        return tap.wrapped();
    }

    /**
     * What PTT said when its DDoS/BOT detection turned {@code socket}'s
     * connection away, or null when that is not what happened.
     */
    public static String message(Object socket) {
        try {
            Tap tap;
            synchronized (TAPS) {
                tap = TAPS.get(socket);
            }
            if (tap == null) {
                return null;
            }
            byte[] bytes = tap.kept();
            if (indexOf(bytes, MARKER) < 0) {
                return null;
            }
            String text = screenText(decode(bytes));
            StringBuilder message = new StringBuilder();
            if (!text.isEmpty()) {
                message.append(text).append("\n\n");
            }
            return message.append("JPTT 不會自動重試，請過一段時間再登入。").toString();
        } catch (Throwable ex) {
            Log.e(JpttContext.LOG_TAG, "Could not read the DDoS/BOT detection message", ex);
            return null;
        }
    }

    /**
     * UTF-8, which is what the detection sends, unless that leaves more broken
     * characters than Big5 would: then this is some screen PTT sent in Big5.
     */
    private static String decode(byte[] bytes) {
        String utf8 = decode(bytes, StandardCharsets.UTF_8);
        try {
            String big5 = decode(bytes, Charset.forName("Big5"));
            if (count(big5, '�') < count(utf8, '�')) {
                return big5;
            }
        } catch (Throwable ignored) {
            // No Big5 on this device: UTF-8 it is.
        }
        return utf8;
    }

    private static String decode(byte[] bytes, Charset charset) {
        try {
            return charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * The words of a terminal screen: escape sequences dropped, a cursor move to
     * another row taken as a line break, and blank lines left out.
     */
    private static String screenText(String raw) {
        StringBuilder lines = new StringBuilder();
        StringBuilder line = new StringBuilder();
        int length = raw.length();
        for (int i = 0; i < length; i++) {
            char c = raw.charAt(i);
            if (c == 0x1b) {
                if (i + 1 < length && raw.charAt(i + 1) == '[') {
                    // A CSI sequence runs to its final byte, 0x40 to 0x7e.
                    int end = i + 2;
                    while (end < length && (raw.charAt(end) < 0x40 || raw.charAt(end) > 0x7e)) {
                        end++;
                    }
                    if (end < length && (raw.charAt(end) == 'H' || raw.charAt(end) == 'J')) {
                        endLine(lines, line);
                    }
                    i = end;
                } else {
                    i++;
                }
            } else if (c == '\n') {
                endLine(lines, line);
            } else if (c >= 0x20 && c != 0x7f) {
                line.append(c);
            }
        }
        endLine(lines, line);
        return lines.toString().trim();
    }

    private static void endLine(StringBuilder lines, StringBuilder line) {
        String trimmed = line.toString().trim();
        line.setLength(0);
        if (!trimmed.isEmpty()) {
            lines.append(trimmed).append('\n');
        }
    }

    private static int count(String text, char wanted) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == wanted) {
                count++;
            }
        }
        return count;
    }

    private static int indexOf(byte[] bytes, byte[] wanted) {
        outer:
        for (int i = 0; i + wanted.length <= bytes.length; i++) {
            for (int j = 0; j < wanted.length; j++) {
                if (bytes[i + j] != wanted[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /** Passes everything through, keeping a copy of the first {@link #KEPT_BYTES}. */
    private static final class Tap extends FilterInputStream {

        private final ByteArrayOutputStream kept = new ByteArrayOutputStream();

        Tap(InputStream in) {
            super(in);
        }

        InputStream wrapped() {
            return in;
        }

        synchronized byte[] kept() {
            return kept.toByteArray();
        }

        private synchronized void keep(byte[] buffer, int offset, int count) {
            int room = KEPT_BYTES - kept.size();
            if (count > 0 && room > 0) {
                kept.write(buffer, offset, Math.min(count, room));
            }
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0) {
                keep(new byte[] {(byte) b}, 0, 1);
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = super.read(buffer, offset, length);
            keep(buffer, offset, count);
            return count;
        }

        @Override
        public long skip(long n) throws IOException {
            // Read rather than skip, so what is skipped is kept too.
            byte[] buffer = new byte[(int) Math.min(n, 4096)];
            int count = read(buffer, 0, buffer.length);
            return Math.max(count, 0);
        }
    }

    private BotBlockPatch() {
    }
}
