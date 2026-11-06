import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Local cryptography checks, no attestation request and no personal aliases. */
public final class KeystoreSmoke {
    private static int checks;
    private static void require(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        checks++;
        System.out.println("CHECK_PASS " + name);
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !"ISOLATED_ROM_LAB_CONFIRMED".equals(args[0]))
            throw new IllegalArgumentException("laboratory guard required");
        String aesAlias="rom-lab-smoke-aes-20261106", ecAlias="rom-lab-smoke-ec-20261106";
        // app_process skips ActivityThread's normal provider registration.
        // Register the device's real provider, never substitute cryptography.
        if (Security.getProvider("AndroidKeyStore")==null)
            Class.forName("android.security.keystore2.AndroidKeyStoreProvider").getMethod("install").invoke(null);
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if (store.containsAlias(aesAlias) || store.containsAlias(ecAlias))
            throw new IllegalStateException("refusing to overwrite existing aliases");
        boolean madeAes=false, madeEc=false;
        try {
            KeyGenerator kg=KeyGenerator.getInstance("AES","AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(aesAlias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            SecretKey aes=kg.generateKey();madeAes=true;
            require(aes.getEncoded()==null,"AES key bytes non-exportable");
            byte[] input="ROM laboratory known plaintext".getBytes(StandardCharsets.UTF_8);
            Cipher enc=Cipher.getInstance("AES/GCM/NoPadding");enc.init(Cipher.ENCRYPT_MODE,aes);
            byte[] encrypted=enc.doFinal(input), iv=enc.getIV();
            Cipher dec=Cipher.getInstance("AES/GCM/NoPadding");
            dec.init(Cipher.DECRYPT_MODE,store.getKey(aesAlias,null),new GCMParameterSpec(128,iv));
            require(Arrays.equals(input,dec.doFinal(encrypted)),"AES-GCM roundtrip from stored key");
            encrypted[0]^=1;boolean rejected=false;
            try {dec.init(Cipher.DECRYPT_MODE,aes,new GCMParameterSpec(128,iv));dec.doFinal(encrypted);}
            catch (AEADBadTagException expected) {rejected=true;}
            require(rejected,"AES-GCM tampered ciphertext rejected");
            KeyInfo aesInfo=(KeyInfo)SecretKeyFactory.getInstance("AES","AndroidKeyStore").getKeySpec(aes,KeyInfo.class);
            System.out.println("AES_SECURITY_LEVEL "+aesInfo.getSecurityLevel());
            KeyPairGenerator gen=KeyPairGenerator.getInstance("EC","AndroidKeyStore");
            gen.initialize(new KeyGenParameterSpec.Builder(ecAlias,KeyProperties.PURPOSE_SIGN|KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256).build());
            KeyPair ec=gen.generateKeyPair();madeEc=true;
            require(ec.getPrivate().getEncoded()==null,"EC private key bytes non-exportable");
            Signature sig=Signature.getInstance("SHA256withECDSA");sig.initSign((PrivateKey)store.getKey(ecAlias,null));sig.update(input);
            byte[] signature=sig.sign();sig.initVerify(ec.getPublic());sig.update(input);
            require(sig.verify(signature),"ECDSA stored key signature verifies");
            input[0]^=1;sig.initVerify(ec.getPublic());sig.update(input);
            require(!sig.verify(signature),"ECDSA altered message rejected");
            KeyInfo ecInfo=KeyFactory.getInstance("EC","AndroidKeyStore").getKeySpec(ec.getPrivate(),KeyInfo.class);
            System.out.println("EC_SECURITY_LEVEL "+ecInfo.getSecurityLevel());
        } finally {
            if (madeAes) store.deleteEntry(aesAlias);
            if (madeEc) store.deleteEntry(ecAlias);
        }
        require(!store.containsAlias(aesAlias)&&!store.containsAlias(ecAlias),"test aliases removed");
        System.out.println("KEYSTORE_SMOKE_PASS checks="+checks+" remote_attestation=NOT_REQUESTED hardware=NOT_QUALIFIED");
    }
}
