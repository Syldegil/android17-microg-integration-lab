# Evidence — historical experiments dated 2026-11-06

Variants are named by their construction, not identified by private artifact hashes:

| Variant | Method/environment | Historical result | Limits |
|---|---|---|---|
| Initial integrated virtual framework/microG | Android17 emulator, real system_server Binder |16 PM checks PASS; SELinux Enforcing | Self-Check attempts failed (UI extraction exit137, then missing entry); not a Self-Check success |
| Integrated negative-controls/Self-Check variant | Emulator, negative APKs |7 negative PM checks, wrongly signed installer rejection and16 PM regression checks PASS | Default Self-Check8/18;18/18 only with temporary permissions, subsequently revoked; cleanup reported without error |
| Permanent App Links refusal variant | Cold emulator boot; policy, actual APK methods with mocked local transport |16 PM;23 policy+7 fetcher+4 verifier=34 historical checks; temporary Self-Check18/18 then restoration | No network/hardware equivalence; some bounded windows had no ANR, other variants had ANRs |
| Targeted kernel/VINTF check | Host AOSP kernel version/config/SELinux logic | One baseline accepted, three negatives rejected | Not full HAL/APEX/VINTF; no Phone boot of candidate ROM |

These are rewritten reports, not fresh executions of Android scenarios. Private manifests retain exact source/image correspondence; no hash of an unshared private image is exported. That limits independent replay of the historical image-specific results. Fresh host checks are in VALIDATION.md. Emulator Android17 is not a full Nothing hardware simulator. No banking compatibility, notifications, installable Phone ROM, universal Google-traffic absence or long-term stability is proven.
