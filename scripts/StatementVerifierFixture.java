/* SPDX-License-Identifier: Apache-2.0 */
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.cert.Certificate;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.HttpsURLConnection;

/** Actual verifier control flow with explicit local network fixtures; no real network. */
public final class StatementVerifierFixture {
    private static final AtomicInteger googleOpened = new AtomicInteger();
    private static final class LocalConnection extends HttpsURLConnection {
        private final byte[] body;
        LocalConnection(URL url) {
            super(url);
            body = (url.getHost().equals("delegation.example")
                    ? "[{\"include\":\"https://g.co/.well-known/assetlinks.json\"}]" : "[]")
                    .getBytes(StandardCharsets.UTF_8);
        }
        public void connect() {}
        public void disconnect() {}
        public boolean usingProxy() { return false; }
        public String getCipherSuite() { return "FIXTURE_NO_TLS_CONNECTION"; }
        public Certificate[] getLocalCertificates() { return null; }
        public Certificate[] getServerCertificates() { return new Certificate[0]; }
        public int getResponseCode() { return 200; }
        public int getContentLength() { return body.length; }
        public InputStream getInputStream() { return new ByteArrayInputStream(body); }
    }
    private static Object call(Object verifier, Method method, String host,
                               Class<?> continuationClass) throws Exception {
        Object context = Class.forName("kotlin.coroutines.EmptyCoroutineContext").getField("INSTANCE").get(null);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        Object continuation = Proxy.newProxyInstance(continuationClass.getClassLoader(),
                new Class<?>[]{continuationClass}, (proxy, invoked, values) -> {
                    if (invoked.getName().equals("getContext")) return context;
                    if (invoked.getName().equals("resumeWith")) {
                        result.set(values[0]); finished.countDown(); return null;
                    }
                    if (invoked.getName().equals("toString")) return "VerifierLocalFixture";
                    throw new AssertionError(invoked.getName());
                });
        System.out.println("VERIFIER_CASE " + host);
        Object returned = method.invoke(verifier, host, null, null, continuation);
        if (returned != null && returned.toString().equals("COROUTINE_SUSPENDED")) {
            if (!finished.await(15, TimeUnit.SECONDS)) throw new AssertionError("verifier timeout");
        } else result.set(returned);
        if (result.get() == null || !result.get().getClass().getName().equals("kotlin.Triple"))
            throw new AssertionError("verifier did not return a result: " + result.get());
        return result.get();
    }
    private static void expect(Object triple, String resultType, int status) throws Exception {
        Object work = triple.getClass().getMethod("component1").invoke(triple);
        Object state = triple.getClass().getMethod("component2").invoke(triple);
        Object statement = triple.getClass().getMethod("component3").invoke(triple);
        if (!work.getClass().getName().equals("androidx.work.ListenableWorker$Result$" + resultType)
                || ((Integer) state.getClass().getMethod("getValue").invoke(state)) != status
                || statement != null) throw new AssertionError("unexpected verifier decision " + triple);
    }
    public static void main(String[] args) throws Exception {
        URL.setURLStreamHandlerFactory(protocol -> !protocol.equals("https") ? null : new URLStreamHandler() {
            protected URLConnection openConnection(URL url) {
                if (url.getHost().equals("g.co")) googleOpened.incrementAndGet();
                if (url.getHost().equals("transient.example"))
                    throw new SecurityException("ordinary transport fixture exception");
                return new LocalConnection(url);
            }
        });
        Class<?> type = Class.forName("com.android.statementservice.domain.DomainVerifier");
        Class<?> continuation = Class.forName("kotlin.coroutines.Continuation");
        Constructor<?> constructor = type.getDeclaredConstructor(Class.forName("android.content.Context"),
                Class.forName("android.content.pm.verify.domain.DomainVerificationManager"));
        constructor.setAccessible(true);
        // This private path never consults PM/context or a matcher when transport fails or returns [].
        Object verifier = constructor.newInstance(null, null);
        Method method = type.getDeclaredMethod("verifyHost", String.class,
                Class.forName("com.android.statementservice.retriever.AbstractAssetMatcher"),
                Class.forName("android.net.Network"), continuation);
        method.setAccessible(true);
        expect(call(verifier, method, "g.co", continuation), "Failure", 1024);
        expect(call(verifier, method, "delegation.example", continuation), "Failure", 1024);
        expect(call(verifier, method, "transient.example", continuation), "Retry", 1028);
        expect(call(verifier, method, "empty.example", continuation), "Failure", 1026);
        Object companion = Class.forName("com.android.statementservice.domain.VerifyStatus").getField("Companion").get(null);
        Method retry = companion.getClass().getMethod("shouldRetry", int.class);
        if ((Boolean) retry.invoke(companion, 1024) || !(Boolean) retry.invoke(companion, 1028))
            throw new AssertionError("retry classification changed");
        if (googleOpened.get() != 0) throw new AssertionError("Google transport opened");
        System.out.println("STATEMENT_VERIFIER_PASS cases=4 google_connections=0 policy_retry=false transient_retry=true");
    }
}
