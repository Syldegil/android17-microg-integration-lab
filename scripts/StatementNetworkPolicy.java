/* SPDX-License-Identifier: Apache-2.0 */
package com.android.statementservice.network.retriever;

import java.net.IDN;
import java.net.URL;
import java.util.Locale;

/** System App Links transport policy. Never grants domain verification. */
public final class StatementNetworkPolicy {
    private StatementNetworkPolicy() {}

    /** Distinguishes a fixed system policy from transient transport failures. */
    public static final class DeniedException extends SecurityException {
        private static final long serialVersionUID = 1L;
        public DeniedException() {
            super("App Links verification denied by system network policy");
        }
    }

    // Explicit service-domain inventory, not an assertion covering every Google IP/domain.
    private static final String[] DOMAINS = {
        "google.com", "googleapis.com", "gstatic.com", "googleusercontent.com",
        "googleadservices.com", "googlesyndication.com", "doubleclick.net",
        "firebaseio.com", "crashlytics.com", "app-measurement.com", "g.co", "goo.gl"
    };
    private static final String[] EXACT = {
        "time.android.com", "connectivitycheck.android.com"
    };

    public static boolean denied(URL url) {
        String rawHost = url.getHost();
        // IPv6 literals cannot match a DNS suffix; preserve the existing fetch behavior.
        if (rawHost.startsWith("[") && rawHost.endsWith("]")) return false;
        String host = IDN.toASCII(rawHost).toLowerCase(Locale.ROOT);
        while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        for (String domain : DOMAINS) {
            if (host.equals(domain) || host.endsWith("." + domain)) return true;
        }
        for (String domain : EXACT) if (host.equals(domain)) return true;
        return false;
    }

    /** Called for every fetch, including delegated statements, before any connection. */
    public static void enforce(URL url) {
        if (denied(url)) {
            throw new DeniedException();
        }
    }
}
