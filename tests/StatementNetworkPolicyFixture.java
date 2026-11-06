import com.android.statementservice.network.retriever.StatementNetworkPolicy;
import java.net.URL;

public final class StatementNetworkPolicyFixture {
    public static void main(String[] args) throws Exception {
        String[] blocked = {
            "https://g.co/.well-known/assetlinks.json",
            "https://app.goo.gl/.well-known/assetlinks.json",
            "https://APP.GOO.GL.:443/test", "https://x.google.com/test",
            "https://google.com/", "https://time.android.com/",
            "https://user:password@g.co/path", "https://gstatic.com:444/",
            "https://firebaseio.com/", "https://a.firebaseio.com/"
        };
        String[] allowed = {
            "https://example.org/.well-known/assetlinks.json",
            "https://g.co.example.org/", "https://notgoogle.com/",
            "https://gstatic.grapheneos.org/", "https://android.com/",
            "https://google.com@example.org/", "https://example.org/google.com",
            "https://example.org/?url=https://g.co/", "https://example.org/#g.co",
            "https://connectivitycheck.android.com.example.org/",
            "https://[::1]/", "https://192.0.2.1/", "https://éxample.org/"
        };
        int checks = 0;
        for (String value : blocked) {
            URL url = new URL(value);
            if (!StatementNetworkPolicy.denied(url)) throw new AssertionError(value);
            try { StatementNetworkPolicy.enforce(url); throw new AssertionError(value); }
            catch (SecurityException expected) { checks++; }
        }
        for (String value : allowed) {
            URL url = new URL(value);
            if (StatementNetworkPolicy.denied(url)) throw new AssertionError(value);
            StatementNetworkPolicy.enforce(url); checks++;
        }
        System.out.println("STATEMENT_POLICY_PASS checks=" + checks);
    }
}
