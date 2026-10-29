package com.goldendash.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tile peta 256 px dari server tile OpenStreetMap (tanpa API key; tema Gelap memakai filter warna di LiveMap).
 * Mengikuti kebijakan tile OSM: User-Agent yang jelas, atribusi, dan cache, tanpa unduhan massal di muka.
 * Disimpan di memori (±8 MB, RGB_565) dan di cache disk (7 hari, maks. ±40 MB), jadi jalan yang sering dilewati
 * tidak diunduh ulang. Satu thread pengunduh; permintaan terbaru didahulukan karena mobil terus bergerak.
 */
final class Tiles {
    static final String OSM = "osm";
    static final String ATTRIBUTION = "© OpenStreetMap contributors";
    /** Server tile per gaya; ganti di sini bila memakai penyedia lain (mis. dengan API key sendiri). */
    private static final String OSM_URL = "https://tile.openstreetmap.org/";

    interface Listener {
        void onTile();
    }

    private static final long MAX_AGE = 7L * 24 * 3600_000L, RETRY = 30_000L, DISK_MAX = 40L * 1024 * 1024;
    private static final int QUEUE_MAX = 24;
    private static final String AGENT = "GoldenDash/0.2 (+https://github.com/agungpmbd/goldendash)";

    private static Tiles instance;

    private final Context ctx;
    private final File dir;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> mem = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) {
            return b.getByteCount();
        }
    };
    private final LinkedList<String> queue = new LinkedList<>();
    private final Set<String> pending = new HashSet<>();
    private final Map<String, Long> failed = new HashMap<>();
    private final List<Listener> listeners = new ArrayList<>();
    private int writes;

    static Tiles get(Context c) {
        if (instance == null) instance = new Tiles(c.getApplicationContext());
        return instance;
    }

    private Tiles(Context c) {
        ctx = c;
        dir = new File(c.getCacheDir(), "tiles");
        Thread t = new Thread(this::work, "tiles");
        t.setDaemon(true);
        t.start();
    }

    void add(Listener l) {
        listeners.add(l);
    }

    void remove(Listener l) {
        listeners.remove(l);
    }

    /** Tile dari memori, atau null sambil memintanya dari disk/jaringan (pemanggil diberi tahu lewat Listener). */
    Bitmap get(String style, int z, int x, int y) {
        String key = style + "/" + z + "/" + x + "/" + y;
        Bitmap b = mem.get(key);
        if (b != null) return b;
        synchronized (queue) {
            Long f = failed.get(key);
            if (pending.contains(key) || (f != null && System.currentTimeMillis() - f < RETRY)) return null;
            pending.add(key);
            queue.addFirst(key);
            while (queue.size() > QUEUE_MAX) pending.remove(queue.removeLast());
            queue.notify();
        }
        return null;
    }

    private void work() {
        while (true) {
            String key;
            synchronized (queue) {
                while (queue.isEmpty()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                key = queue.removeFirst();
            }
            Bitmap b = load(key);
            synchronized (queue) {
                pending.remove(key);
                if (b == null) failed.put(key, System.currentTimeMillis());
                else failed.remove(key);
            }
            if (b != null) {
                final Bitmap fb = b;
                final String fk = key;
                main.post(() -> {
                    mem.put(fk, fb);
                    for (Listener l : new ArrayList<>(listeners)) l.onTile();
                });
            }
        }
    }

    private Bitmap load(String key) {
        File f = new File(dir, key.replace('/', '_') + ".png");
        boolean fresh = f.exists() && System.currentTimeMillis() - f.lastModified() < MAX_AGE;
        if (fresh || (f.exists() && !online())) {
            Bitmap b = decode(f);
            if (b != null) return b;
        }
        if (!online()) return null;
        try {
            byte[] data = download(OSM_URL + key.substring(key.indexOf('/') + 1) + ".png");
            Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, opts());
            if (b == null) return null;
            save(f, data);
            return b;
        } catch (Exception e) {
            return f.exists() ? decode(f) : null;
        }
    }

    private static BitmapFactory.Options opts() {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPreferredConfig = Bitmap.Config.RGB_565;
        return o;
    }

    private static Bitmap decode(File f) {
        return BitmapFactory.decodeFile(f.getPath(), opts());
    }

    private boolean online() {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressWarnings("deprecation") NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    private static byte[] download(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(8000);
        con.setReadTimeout(8000);
        con.setRequestProperty("User-Agent", AGENT);
        try (InputStream in = con.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
            return out.toByteArray();
        } finally {
            con.disconnect();
        }
    }

    private void save(File f, byte[] data) {
        try {
            if (!dir.exists() && !dir.mkdirs()) return;
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(data);
            }
            if (++writes % 50 == 0) trim();
        } catch (Exception ignored) { }
    }

    /** Hapus tile terlama bila cache disk melebihi batas. */
    private void trim() {
        File[] files = dir.listFiles();
        if (files == null) return;
        long total = 0;
        for (File f : files) total += f.length();
        if (total <= DISK_MAX) return;
        Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File f : files) {
            if (total <= DISK_MAX * 3 / 4) break;
            total -= f.length();
            if (!f.delete()) break;
        }
    }
}
