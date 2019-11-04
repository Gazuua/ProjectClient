package mobteam10.project;

import android.util.Log;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZInputStream;
import com.jcraft.jzlib.ZOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by JHG on 2017-11-24.
 */

public class Jzlib {

    public static byte[] compress(byte[] data)
    {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ZOutputStream zOut = new ZOutputStream(out, JZlib.Z_BEST_COMPRESSION);
            ObjectOutputStream objOut = new ObjectOutputStream(zOut);
            objOut.writeObject(data);
            objOut.close();

            return out.toByteArray();
        }
        catch(Exception e)
        {
            Log.d("Jzlib", "ERR - Jzlib.compress");
            return null;
        }
    }

    public static byte[] decompress(byte[] data)
    {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ZInputStream zIn = new ZInputStream(in);
            ObjectInputStream objIn = new ObjectInputStream(zIn);
            byte[] ret = (byte[]) objIn.readObject();
            objIn.close();
            return ret;
        }
        catch(Exception e)
        {
            Log.d("Jzlib", "ERR - Jzlib.decompress");
            return null;
        }
    }
}
