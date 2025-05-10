
// --- File: common/IOUtil.java ---
package common;

import java.io.*;

/**
 * Mesajların seri hâle getirilmesi ve iletim için yardımcı sınıf.
 */
public final class IOUtil {
    private IOUtil() {}

    public static void writeMessage(OutputStream out, Message msg) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(msg);
        }
        byte[] data = bos.toByteArray();
        DataOutputStream dout = new DataOutputStream(out);
        dout.writeInt(data.length);
        dout.write(data);
        dout.flush();
    }

    public static Message readMessage(InputStream in) throws IOException {
        try {
            DataInputStream din = new DataInputStream(in);
            int len = din.readInt();
            byte[] buf = din.readNBytes(len);
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buf))) {
                return (Message) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        } catch (EOFException eof) {
            // Bağlantı sonlandı
            return null;
        }
    }
}
