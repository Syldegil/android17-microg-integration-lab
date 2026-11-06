# Policy and limitations

Signature compatibility applies only to system packages named com.google.android.gms or com.android.vending, with the exact official microG signing certificate and expected fake-signature metadata. It changes query results for GET_SIGNATURES/GET_SIGNING_CERTIFICATES. Real package signing identity, installer checks, checkSignatures, hasSigningCertificate and signature permissions are not replaced. This is not arbitrary caller-granted signature spoofing. Public Google/microG certificate constants are trust-policy inputs, not private signing keys.

App Links refuses explicitly listed service domains before connection; permanent policy denial leaves domains unverified without policy retries, while transient failures retain retry behavior. Automatic app-link association can be lost for those domains. It is not a system-wide firewall or exhaustive domain/IP classifier; IPv6 literals remain allowed by this policy.

Empty RKP hostname makes remote provisioning unavailable; it does not fake attestation. Hardware keystore, DRM and banking consequences need separate qualification. CT uses gstatic.grapheneos.org distribution while retaining verifier/trust anchors: the network dependency moves, not disappears. Matching string transforms do not prove a bootable signed APEX.

Historical ANRs and initially failed Self-Check attempts remain failures. Permission restoration is part of the temporary18/18 observation. Neither an emulator check nor a checksum qualifies OEM firmware, AVB, recovery or an installable ROM. No full platform build is claimed.
