# Reproduction

Host: JDK17+, Python3.11+. No network/device access in these fixtures:

```sh
sh test-host.sh /tmp/microg-lab-new
```

This runs23 pure-JVM URL-policy cases, Python CT/RKP transformation tests and renders the Smali template from reviewed public certificate constants. The template is a binary-framework integration component, not a standalone ROM builder. The source framework patch and Smali approach are separate integration paths.

Acquire source revisions from config/sources.lock.json. In the matching AOSP frameworks/base tree, `git apply --check /path/to/export/patches/0001-restrict-microg-query-signatures.patch` validates the source hook. Apply the permanent StatementService patch in the same frameworks/base tree; do not stack it with the superseded retry-policy patch. Build the platform with its own supported toolchain and preserve upstream notices. Full platform compilation is NOT_RUN here.

Android lab test sources require a disposable Android17 emulator with the exact integrated image and official microG v0.3.17.252432 system packages, correct privileged permissions and metadata. Compile against matching Android37 android.jar, convert classes with matching SDK d8, and run the resulting dex through app_process as shell in that emulator. IntegratedPmChecks expects16 checks; NegativePmChecks requires the unrelated synthetic APK and checks7 cases. These commands are not an installation recipe for a Phone. The fetcher/verifier fixtures need the matching Android/Kotlin StatementService classes, not merely a desktop JDK. KeystoreSmoke creates and later deletes synthetic aliases: it is not read-only.

For benign negative APK inputs, scripts/build_negative_apks.py accepts explicit --build-tools, --framework-res, --lab-key, --lab-cert and fresh --output. Use newly generated throwaway laboratory signing material; none is distributed. It builds an unrelated package and deliberately wrongly signed GMS package without application code. Installation testing belongs only in the disposable emulator. Never reuse a production key or run against a personal device.

CT functions accept reviewed disassembled strings and reject unexpected URL sets or semantic changes. They are not a complete APEX signer/builder. offline_rkp_policy.without_google_endpoint accepts only the exact expected property/service configuration. No firmware stock, APK, key, system image or phone flashing tool is supplied. Public AOSP test keys are publicly known test material, never production credentials; acquire only from official AOSP for a separately authorized emulator lab.

NegativePmChecks requires the throwaway laboratory certificate SHA-256 hex digest as its argument (without colons). KeystoreSmoke requires literal argument ISOLATED_ROM_LAB_CONFIRMED and writes/deletes test aliases. Use an absolute export patch path when applying from the upstream checkout. Generated APKs and signature logs remain private and have not been audited for publication; they can contain metadata from the supplied certificate.
