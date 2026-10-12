package local.njmu.autologin;
import android.content.*;import android.security.keystore.*;import android.util.Base64;import java.security.KeyStore;import javax.crypto.*;import javax.crypto.spec.GCMParameterSpec;
public class Vault {
 static final String KEY="njmu-auth";
 static javax.crypto.SecretKey key() throws Exception {KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);if(!k.containsAlias(KEY)){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(KEY,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}return (javax.crypto.SecretKey)k.getKey(KEY,null);}
 static void save(Context c,String text)throws Exception{Cipher x=Cipher.getInstance("AES/GCM/NoPadding");x.init(Cipher.ENCRYPT_MODE,key());c.getSharedPreferences("auth",0).edit().putString("iv",Base64.encodeToString(x.getIV(),2)).putString("secret",Base64.encodeToString(x.doFinal(text.getBytes("UTF-8")),2)).apply();}
 static String load(Context c)throws Exception{android.content.SharedPreferences p=c.getSharedPreferences("auth",0);Cipher x=Cipher.getInstance("AES/GCM/NoPadding");x.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p.getString("iv",""),2)));return new String(x.doFinal(Base64.decode(p.getString("secret",""),2)),"UTF-8");}
}
