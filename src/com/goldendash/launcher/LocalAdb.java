package com.goldendash.launcher;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;

/**
 * Perintah shell lewat ADB headunit itu sendiri (adbd di 127.0.0.1:5555), tanpa komputer. Dipakai untuk hal yang
 * butuh hak shell, seperti property launcher NWD. Hanya bekerja bila USB debugging aktif dan adbd mendengar port 5555
 * (umum di headunit Android 10). Bila adbd meminta autentikasi, GoldenDash memakai kunci RSA-nya sendiri, yang
 * diizinkan sekali lewat dialog "Allow USB debugging?".
 *
 * Protokol dan autentikasinya diadaptasi dari Tweaks.java milik KlikDash (MIT, github.com/ashafizullah/klikdash).
 */
final class LocalAdb {
    private static final int CNXN = 0x4e584e43, OPEN = 0x4e45504f, OKAY = 0x59414b4f,
            WRTE = 0x45545257, CLSE = 0x45534c43, AUTH = 0x48545541;
    private static final int AUTH_TOKEN = 1, AUTH_SIGNATURE = 2, AUTH_RSAPUBLICKEY = 3;

    /** Hasil {@link #run}. */
    static final int DONE = 0, NO_ADB = 1, NOT_ALLOWED = 2;

    interface Result {
        void done(int result, String output);
    }

    /** adbd meminta autentikasi dan kunci GoldenDash belum (atau tidak) diizinkan. */
    private static final class NotAllowed extends IOException {
        NotAllowed() { super("adbd meminta autentikasi"); }
    }

    private static KeyPair key;

    private LocalAdb() { }

    static boolean debuggingOn(Context c) {
        return Settings.Global.getInt(c.getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
    }

    /**
     * Jalankan {@code cmd} di latar. Bila adbd belum mengenal kunci GoldenDash, Android menampilkan dialog izin;
     * {@code onAsk} dipanggil saat itu. Keduanya dipanggil di thread latar.
     */
    static void run(Context c, String cmd, Runnable onAsk, Result done) {
        final File dir = c.getApplicationContext().getFilesDir();
        new Thread(() -> {
            int r = DONE;
            String out = "";
            try {
                out = exec(dir, cmd, onAsk);
            } catch (NotAllowed e) {
                r = NOT_ALLOWED;
            } catch (IOException e) {
                r = NO_ADB;
            }
            done.done(r, out);
        }, "local-adb").start();
    }

    private static String exec(File dir, String cmd, Runnable onAsk) throws IOException {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", 5555), 3000);
            s.setSoTimeout(15000);
            DataInputStream in = new DataInputStream(s.getInputStream());
            OutputStream out = s.getOutputStream();
            send(out, CNXN, 0x01000000, 4096, "host::goldendash\0");
            connect(dir, s, in, out, onAsk);
            s.setSoTimeout(15000);
            return service(in, out, "shell:" + cmd);
        }
    }

    /** Buka satu layanan adb dan kumpulkan keluarannya sampai adbd menutupnya. */
    private static String service(DataInputStream in, OutputStream out, String service) throws IOException {
        send(out, OPEN, 1, 0, service + "\0");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while (true) {
            int[] m = recv(in, output);
            if (m[0] == WRTE) {
                send(out, OKAY, 1, m[1], "");
            } else if (m[0] == CLSE) {
                send(out, CLSE, 1, m[1], "");
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }

    /**
     * Tunggu CNXN dari adbd. Bila diminta autentikasi: tanda tangani token; bila kuncinya belum dikenal, kirim kunci
     * publiknya (Android menampilkan dialog izin) dan tunggu jawabannya hingga 60 detik.
     */
    private static void connect(File dir, Socket s, DataInputStream in, OutputStream out, Runnable onAsk) throws IOException {
        boolean signed = false, asked = false;
        while (true) {
            ByteArrayOutputStream token = new ByteArrayOutputStream();
            int[] m;
            try {
                m = recv(in, token);
            } catch (IOException e) {
                // Dialog izin ditolak, dibiarkan, atau koneksi diputus adbd selama menunggu.
                if (asked) throw new NotAllowed();
                throw e;
            }
            if (m[0] == CNXN) return;
            if (m[0] != AUTH || m[1] != AUTH_TOKEN) throw new IOException("balasan adbd tidak dikenal");
            KeyPair k = key(dir);
            if (k == null) throw new NotAllowed();
            if (!signed) {
                send(out, AUTH, AUTH_SIGNATURE, 0, sign(k, token.toByteArray()));
                signed = true;
            } else if (!asked) {
                send(out, AUTH, AUTH_RSAPUBLICKEY, 0, publicKey((RSAPublicKey) k.getPublic()));
                s.setSoTimeout(60_000);
                if (onAsk != null) onAsk.run();
                asked = true;
            } else {
                throw new NotAllowed();
            }
        }
    }

    /** Kunci RSA 2048-bit milik GoldenDash, dibuat sekali lalu disimpan di penyimpanan aplikasi. */
    private static synchronized KeyPair key(File dir) {
        if (key != null) return key;
        File f = new File(dir, "adbkey");
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            if (f.exists()) {
                byte[] enc = new byte[(int) f.length()];
                try (DataInputStream i = new DataInputStream(new FileInputStream(f))) {
                    i.readFully(enc);
                }
                RSAPrivateCrtKey priv = (RSAPrivateCrtKey) kf.generatePrivate(new PKCS8EncodedKeySpec(enc));
                key = new KeyPair(kf.generatePublic(new RSAPublicKeySpec(priv.getModulus(), priv.getPublicExponent())), priv);
            } else {
                KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
                g.initialize(2048);
                KeyPair k = g.generateKeyPair();
                File tmp = new File(dir, "adbkey.tmp");
                try (FileOutputStream o = new FileOutputStream(tmp)) {
                    o.write(k.getPrivate().getEncoded());
                }
                if (!tmp.renameTo(f)) throw new IOException("adbkey tidak tersimpan");
                key = k;
            }
        } catch (Exception e) {
            android.util.Log.w("GoldenDash", "adbkey", e);
        }
        return key;
    }

    /** PKCS#1 v1.5 atas token 20 byte yang diperlakukan adbd sebagai hash SHA-1. */
    private static byte[] sign(KeyPair k, byte[] token) throws IOException {
        byte[] block = new byte[256];
        byte[] sha1 = {0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14};
        int pad = block.length - sha1.length - token.length;
        block[1] = 1;
        for (int i = 2; i < pad - 1; i++) block[i] = (byte) 0xff;
        System.arraycopy(sha1, 0, block, pad, sha1.length);
        System.arraycopy(token, 0, block, pad + sha1.length, token.length);
        try {
            Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, k.getPrivate());
            return c.doFinal(block);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /** Format kunci publik adbd (struct RSAPublicKey mincrypt, base64) diikuti nama kunci. */
    private static byte[] publicKey(RSAPublicKey k) {
        BigInteger n = k.getModulus(), r32 = BigInteger.ONE.shiftLeft(32);
        ByteBuffer b = ByteBuffer.allocate(4 + 4 + 256 + 256 + 4).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(64).putInt(n.mod(r32).modInverse(r32).negate().mod(r32).intValue());
        putLittleEndian(b, n);
        putLittleEndian(b, BigInteger.ONE.shiftLeft(2048).modPow(BigInteger.valueOf(2), n));
        b.putInt(k.getPublicExponent().intValue());
        return (Base64.encodeToString(b.array(), Base64.NO_WRAP) + " goldendash@" + android.os.Build.MODEL + "\0")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static void putLittleEndian(ByteBuffer b, BigInteger v) {
        byte[] be = v.toByteArray();
        for (int i = 0; i < 256; i++) b.put(i < be.length ? be[be.length - 1 - i] : 0);
    }

    private static void send(OutputStream out, int cmd, int arg0, int arg1, String data) throws IOException {
        send(out, cmd, arg0, arg1, data.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(OutputStream out, int cmd, int arg0, int arg1, byte[] d) throws IOException {
        int sum = 0;
        for (byte b : d) sum += b & 0xff;
        ByteBuffer h = ByteBuffer.allocate(24 + d.length).order(ByteOrder.LITTLE_ENDIAN);
        h.putInt(cmd).putInt(arg0).putInt(arg1).putInt(d.length).putInt(sum).putInt(~cmd).put(d);
        out.write(h.array());
        out.flush();
    }

    /** {command, arg0, arg1}; payload WRTE/AUTH ditambahkan ke {@code sink}. */
    private static int[] recv(DataInputStream in, ByteArrayOutputStream sink) throws IOException {
        byte[] raw = new byte[24];
        in.readFully(raw);
        ByteBuffer h = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        int cmd = h.getInt(), a0 = h.getInt(), a1 = h.getInt(), len = h.getInt();
        byte[] data = new byte[len];
        in.readFully(data);
        if (sink != null && (cmd == WRTE || cmd == AUTH)) sink.write(data, 0, len);
        return new int[]{cmd, a0, a1};
    }
}
