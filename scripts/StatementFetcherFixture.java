/* SPDX-License-Identifier: Apache-2.0 */
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.cert.Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.HttpsURLConnection;

/** Runs the actual patched fetch method with a local transport, never an Internet request. */
public final class StatementFetcherFixture {
    private static final AtomicInteger opened = new AtomicInteger();
    private static final AtomicInteger disconnected = new AtomicInteger();
    private static final class LocalConnection extends HttpsURLConnection {
        LocalConnection(URL url) { super(url); }
        public void connect() {}
        public void disconnect() { disconnected.incrementAndGet(); }
        public boolean usingProxy() { return false; }
        public String getCipherSuite() { return "FIXTURE_NO_TLS_CONNECTION"; }
        public Certificate[] getLocalCertificates() { return null; }
        public Certificate[] getServerCertificates() { return new Certificate[0]; }
        public int getResponseCode() {
            if (getInstanceFollowRedirects()) throw new AssertionError("redirect protection lost");
            return 200;
        }
        public int getContentLength() { return 2; }
        public InputStream getInputStream() { return new ByteArrayInputStream(new byte[]{'[', ']'}); }
    }
    private static URL localUrl(String text) throws Exception {
        return new URL(null, text, new URLStreamHandler() {
            protected URLConnection openConnection(URL url) {
                opened.incrementAndGet();
                return new LocalConnection(url);
            }
        });
    }
    public static void main(String[] args) throws Exception {
        Class<?> fetcherClass = Class.forName("com.android.statementservice.network.retriever.UrlFetcher");
        Class<?> continuationClass = Class.forName("kotlin.coroutines.Continuation");
        Object fetcher = fetcherClass.getConstructor().newInstance();
        Method fetch = fetcherClass.getMethod("fetch", URL.class, int.class, long.class,
                Class.forName("android.net.Network"), continuationClass);
        int checks = 0;
        for (String host : new String[]{"g.co", "app.goo.gl", "G.CO.", "x.google.com"}) {
            try {
                fetch.invoke(fetcher, localUrl("https://" + host + "/.well-known/assetlinks.json"),
                        1000, 1024L, null, null);
                throw new AssertionError("policy did not reject " + host);
            } catch (InvocationTargetException failure) {
                if (!(failure.getCause() instanceof SecurityException)) throw failure;
            }
            if (opened.get() != 0) throw new AssertionError("denied host opened connection");
            checks++;
        }
        Object emptyContext = Class.forName("kotlin.coroutines.EmptyCoroutineContext").getField("INSTANCE").get(null);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        Object continuation = Proxy.newProxyInstance(continuationClass.getClassLoader(),
                new Class<?>[]{continuationClass}, (proxy, method, values) -> {
                    if (method.getName().equals("getContext")) return emptyContext;
                    if (method.getName().equals("resumeWith")) {
                        result.set(values[0]); finished.countDown(); return null;
                    }
                    if (method.getName().equals("toString")) return "LocalTransportFixture";
                    throw new AssertionError(method.getName());
                });
        Object returned = fetch.invoke(fetcher, localUrl("https://g.co.example.org/.well-known/assetlinks.json"),
                1000, 1024L, null, continuation);
        if (returned != null && returned.toString().equals("COROUTINE_SUSPENDED")) {
            if (!finished.await(15, TimeUnit.SECONDS)) throw new AssertionError("fetch timeout");
        } else result.set(returned);
        if (result.get() == null || !result.get().getClass().getName().equals("com.android.statementservice.utils.Result$Success"))
            throw new AssertionError("ordinary host fetch did not succeed: " + result.get());
        if (opened.get() != 1 || disconnected.get() != 1) throw new AssertionError("transport lifecycle changed");
        checks += 3;
        System.out.println("STATEMENT_FETCHER_PASS checks=" + checks + " real_network_connections=0");
    }
}
