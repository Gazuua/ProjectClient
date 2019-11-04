package mobteam10.project;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by JHG on 2017-10-04.
 */

// OutputStream을 이용하여 서버로 패킷을 보내는 작업을 담당하는 클래스
public class SocketSender implements Runnable {

    // 객체 선언
    private OutputStream outputStream;
    private BufferedOutputStream bos;
    private Queue<PacketMessage> outPacketQueue;

    // 생성자
    public SocketSender()
    {
        this.outPacketQueue = new LinkedList<>();
        this.outputStream = SocketHandler.getClassInstance().getOutputStream();
        this.bos = new BufferedOutputStream(outputStream);
    }

    // 스레드 돌릴 때 할 작업을 정의
    @Override
    public synchronized void run() {
        while(true)
        {
            // 큐에서 패킷을 하나 빼온다
            PacketMessage msg = outPacketQueue.poll();

            // 큐가 현재 비어있어서 null이 리턴되면 wait를 걸어서 write 동작을 하지 않게 한다
            if ( msg == null )
            {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    // 프로그램 종료 시 바깥에서 interrupt를 걸어줘서 예외가 발생됨으로써 종료된다
                    return;
                }
                continue; // 누군가 notify를 걸어줘서 빠져나오면 반복문의 처음으로 가서 큐를 읽는다
            }

            if ( !write(msg) ) return;
        }
    }

    public boolean write(PacketMessage msg)
    {
        // 패킷을 직렬화되어 있지 않은 경우 직렬화하며 그것을 바이트 버퍼에 담는다
        if ( msg.getSerializedBytes() == null ) msg.serialize();
        byte[] data = msg.getSerializedBytes();
        byte[] encrypted = DynamicCrypter.getInstance().encrypt(data);
        byte[] size = shortToByteArray((short)encrypted.length);
        byte[] buf = combineByteArrays(size, encrypted);

        Log.d("SEND", "" + data.length);

        try {
            bos.write(buf, 0, buf.length); // 서버로 보낸다
            bos.flush();    // 패킷은 하나씩만 보내므로, 보냈으면 출력 스트림을 비운다
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // short값을 원소 2개의 바이트 배열로 캐스팅하는 메소드
    public byte[] shortToByteArray(short n)
    {
        byte[] ret = new byte[2];

        ret[0] = (byte)((n>>8) & 0xFF);
        ret[1] = (byte)((n>>0) & 0xFF);

        return ret;
    }

    public byte[] combineByteArrays(byte[] a, byte[] b)
    {
        byte[] ret = new byte[a.length + b.length];

        int j = 0;

        for(byte k : a)
        {
            ret[j] = k;
            j++;
        }

        for(byte k : b)
        {
            ret[j] = k;
            j++;
        }

        return ret;
    }

    // 외부에서 패킷 전송 요청을 할 때 쓰는 메소드
    public synchronized void sendPacket(PacketMessage msg)
    {
        outPacketQueue.offer(msg);  // 큐에 패킷을 하나 추가하고
        this.notify();               // 현재 wait 상태에서 기다리는 스레드를 깨운다
    }
}
