# Privacy and network behavior

Exported code contains only public service names, public certificate constants and synthetic test inputs. user:password in the URL-policy fixture is a deliberately fake URL-userinfo canary; no connection occurs. org.example.microglab.spoofnegative is a synthetic package. Device IDs, private paths, account/app inventory, raw captures, keys, private image hashes and session links are excluded.

Host fixtures do not contact networks. An integrated Android system can still contact services outside the explicit policy. CT contacts the GrapheneOS mirror; RKP with empty hostname cannot provision remotely. microG network features depend on configuration and can contact Google (including enabled push services); a successful Self-Check is not network silence. There is no added tracking. Repository association with Syldegil does not guarantee anonymity.
