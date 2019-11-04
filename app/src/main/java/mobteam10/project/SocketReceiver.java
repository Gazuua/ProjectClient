package mobteam10.project;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by JHG on 2017-11-09.
 */

// InputStream을 이용하여 서버에서 패킷을 받아오고, 그 데이터를 다루며 뷰를 업데이트하는 작업을 담당하는 클래스
public class SocketReceiver implements Runnable {

    // 객체 선언
    private SocketHandler socketHandler; // UserData와 같은 전역 데이터를 다루기 위해 싱글톤 객체 받아옴
    private InputStream inputStream;
    private BufferedInputStream bis;

    private Thread timer;

    private ViewHandlerCallback callback;

    // Activity의 View를 다루기 위해 Callback Interface 정의
    public static interface ViewHandlerCallback {
        public void showChatMessage(String msg, int index, int namelen);
        public void showToastMessage(String msg);
        public void setTimer(int milis);
        public void showAnswerText(String answer, int flag);
        public void setScore(int index);
        public void playSound(int flag);
        public void stopSound();
        public void showUserListView(String name, int index);
        public void deleteUserListView(int index);
        public void showExitDialog();
        public void showDisconDialog();
    }

    public SocketReceiver()
    {
        socketHandler = SocketHandler.getClassInstance();
        inputStream = socketHandler.getInputStream();
        bis = new BufferedInputStream(inputStream);
    }

    @Override
    public void run() {

        while (true) {
            byte[] buf = null;
            buf = read();

            if(buf == null) // 예외 발생(연결 이상 등)으로 buf에 null값이 올 경우 예외처리
            {
                callback.showDisconDialog(); // 서버 문제로 종료 시 접속 종료 알림 생성
                return;
            }

            final PacketMessage msg = PacketMessage.deserialize(buf);
            if (msg == null) continue;

            // 우선 채팅 메세지가 있으면 그거부터 뿌린다
            if (msg.getChatMsg() != null && msg.getMsgCode() != MessageCode.CHAT) {
                String message = msg.getChatMsg();
                callback.showToastMessage(message);
            }

            // 메세지 코드에 따른 분기 처리
            switch (msg.getMsgCode()) {
                case MessageCode.JOIN: // 누군가 들어오면 유저 리스트 데이터를 뽑아서 뷰에 보여준다
                    UserData userData = socketHandler.getUserData();
                    if (userData.getIndex() == -1) // 들어온 사람이 자신이라면 서버에서 받은 인덱스를 설정해준다
                        userData.setIndex(msg.getUserData().getIndex());
                    ArrayList<UserData> userlist = msg.getUserList();
                    Iterator iterator = userlist.iterator();
                    while (iterator.hasNext()) {
                        UserData data = (UserData) iterator.next();
                        if (data.getIndex() != -1)
                            callback.showUserListView(data.getName(), data.getIndex());
                    }
                    break;

                case MessageCode.CHAT:
                    String message = msg.getChatMsg();
                    callback.showChatMessage(message,
                            msg.getUserData().getIndex(), msg.getUserData().getName().length());
                    break;

                case MessageCode.START:
                    callback.showAnswerText(msg.getAnswer(), 0);
                    callback.playSound(0);
                    socketHandler.setErase(true);
                    socketHandler.setDrawer(false);
                    socketHandler.setGaming(true);
                    startTimer(120000);
                    break;

                case MessageCode.DRAW:
                    socketHandler.getDrawPacketAndPut(msg.getDrawDatas());
                    break;

                case MessageCode.DRAWER:
                    socketHandler.setDrawer(true);
                    callback.showAnswerText(msg.getAnswer(), 0);
                    break;

                case MessageCode.CORRECT:
                    callback.setScore(msg.getUserData().getIndex());
                    break;

                case MessageCode.EXITSTOP:
                    stopTimer();
                    socketHandler.setDrawer(false);
                    socketHandler.setGaming(false);
                    socketHandler.setErase(true);
                case MessageCode.EXIT: // 누군가 나가면 나간 놈만 뷰에서 뺀다
                    callback.deleteUserListView(msg.getUserData().getIndex());
                    break;

                case MessageCode.TIMEOUT:
                    callback.setScore(-1); // setScore 인자가 -1이면 전부 점수를 50점 깐다
                    callback.showAnswerText(msg.getAnswer(), 1);
                    callback.playSound(2);
                    stopTimer();
                    socketHandler.setDrawer(false);
                    socketHandler.setGaming(false);
                    socketHandler.setErase(true);
                    break;

                case MessageCode.FINISH:
                    callback.showAnswerText(msg.getAnswer(), 1);
                    callback.playSound(1);
                    stopTimer();
                    socketHandler.setDrawer(false);
                    socketHandler.setGaming(false);
                    socketHandler.setErase(true);
                    break;

                case MessageCode.DEFAULT:
                    socketHandler.setErase(true);
                    break;
            }

            if (buf != null)
                buf = null;
        }
    }

    // 소켓 스트림 버퍼에서 데이터를 읽는 메소드
    public byte[] read()
    {
        byte[] ret = null;
        byte[] size = new byte[2];
        short packetSize;
        int recvbytes;

        try {
            recvbytes = bis.read(size);
            packetSize = byteArrayToShort(size);
            ret = new byte[packetSize];
            recvbytes = 0;

            while( packetSize != recvbytes )
            {
                int temp = recvbytes;
                recvbytes += bis.read(ret, temp, packetSize - temp);
                Log.d("offsets", "now => " + recvbytes);
            }

            Log.d("BYTES", "recv = " + recvbytes + " / packet = " + packetSize
            + "ret size = " + ret.length);

        } catch (IOException e) {
            callback.showDisconDialog(); // 서버 문제로 종료 시 접속 종료 알림 생성
            return null;
        }

        ret = DynamicCrypter.getInstance().decrypt(ret);

        return ret;
    }

    // 원소 2개의 바이트 배열을 short값으로 캐스팅하는 메소드
    public short byteArrayToShort(byte[] arr)
    {
        short ret = 0;
        ret = (short) (((arr[0] & 0xFF) << 8) + (arr[1] & 0xFF));

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

    public void startTimer(int time) {

        final int t = time;
        // 이미 타이머가 도는 중이면 타이머를 중단시키고 null로 만든 후 다시 타이머를 가동한다
        if (timer != null) {
            if (timer.getState() == Thread.State.TIMED_WAITING || timer.isAlive())
                stopTimer();
        }

        timer = new Thread(new Runnable() {
            @Override
            public void run() {
                int rtime = t;
                while(rtime > 0) {
                    callback.setTimer(rtime); // 남은 시간만큼 타이머 텍스트뷰에 표시해 준다
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        callback.stopSound();
                        callback.setTimer(0);
                        // 바깥에서 interrupt를 걸면 타이머를 0으로 맞추고 종료한다
                        return;
                    }
                    rtime -= 1000;
                    if( rtime <= 10000 ) callback.playSound(3);
                }
                callback.stopSound();
                callback.setTimer(0);
            }
        });
        timer.start();
    }

    public void stopTimer() {
        if (timer != null) {
            timer.interrupt();
            timer = null;
        }
    }

    // 콜백 인스턴스를 얻어오는 메소드
    public boolean getCallbackInstance(ViewHandlerCallback callback) {
        if ( this.callback == null ) {
            this.callback = callback;
            return true;
        }
        return false;
    }
}
