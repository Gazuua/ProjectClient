package mobteam10.project;

import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by JHG on 2017-11-30.
 */

// 동적 대칭 키를 이용하여 암,복호화를 하는 클래스
public class DynamicCrypter {

    private static final DynamicCrypter crypter = new DynamicCrypter(); // Singleton

    private byte[] keyBase = "Mobile10Project!".getBytes(); // Key = 16Bytes
    private byte[] key;
    private Cipher cipher; // 암호화를 실질적으로 작업하는 객체
    private SecretKeySpec spec; // 암호화 키에 대해 서술할 수 있는 객체

    private DynamicCrypter()
    {
        try {
            cipher = Cipher.getInstance("AES");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 싱글톤
    public static DynamicCrypter getInstance()
    {
        return crypter;
    }

    public boolean isReady() {
        if ( key != null && spec != null ) return true;
        else return false;
    }

    // 클라이언트의 ip주소로 동적 대칭 키를 생성하는 메소드
    public void setKeyByValue(String value)
    {
        key = keyBase.clone();
        byte[] val = value.getBytes();

        for(int i=0; i<key.length; i++)
            for (int j=0; j<val.length; j++)
                key[i] ^= val[j];

        spec = new SecretKeySpec(key, "AES");
    }

    // 암호화
    public byte[] encrypt(byte[] data)
    {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, spec);
            return cipher.doFinal(data);
        }
        catch (Exception e)
        {
            Log.d("DynamicCrypter", "ERR - encrypt()");
            return null;
        }
    }

    // 복호화
    public byte[] decrypt(byte[] data)
    {
        try {
            cipher.init(Cipher.DECRYPT_MODE, spec);
            return cipher.doFinal(data);
        }
        catch (Exception e)
        {
            Log.d("DynamicCrypter", "ERR - decrypt()");
            return null;
        }
    }
}
