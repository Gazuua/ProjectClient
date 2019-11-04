package mobteam10.project;

import android.util.Log;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by JHG on 2017-10-03.
 */

// Singleton Pattern으로 구현한, 소켓을 이용하여 네트워크 관련 동작을 총괄 담당하는 클래스
// 전역 데이터인 UserData와 DrawData를 가지고 있어 작업자 스레드에서 가져다 쓸 수 있도록 함
public class SocketHandler {

    // 이 프로그램은 네트워크 게임으로 소켓 핸들링 클래스의 수명주기가 Application 전체와 같음
    // 따라서 프로그램 시작 시점에 필요한 객체들을 모두 static으로 생성 및 할당함
    private final static SocketHandler classInstance = new SocketHandler();
    private static Socket socket = new Socket();
    private static InetSocketAddress address =
            new InetSocketAddress("121.163.249.208", 55248);

    // 그 외의 객체 선언
    private InputStream inputStream;
    private OutputStream outputStream;
    private SocketSender socketSender;
    private SocketReceiver socketReceiver;
    private Thread senderThread;
    private Thread receiverThread;

    // 데이터 객체 선언
    private UserData userData;
    private ArrayList<DrawData> drawData; // 통신용 drawData. 그리기용은 DrawingView에 있다
    private short nowColor;
    private boolean isErase;
    private boolean isGaming;
    private boolean isDrawer;

    // 생성자는 비워둠
    private SocketHandler() { }

    // 싱글톤으로 정의되어 만들어진 객체의 인스턴스를 외부에서 참조할 수 있도록 하는 메소드
    public static SocketHandler getClassInstance() {
        return classInstance;
    }

    // 서버에 접속을 요청하고 접속이 되었는지 값을 전달하는 메소드
    public boolean connect()
    {
        String addr = getLocalOfficialAddress();
        // 웹 파싱하는 작업은 초 단위로 걸리므로, 소켓 서버 연결 전에 미리 저장해 둔다.

        try {
            // 5초 내에 서버로 접속이 되지 않을 경우 timeout
            socket.connect(address, 5000);
        } catch (IOException e) {
            e.printStackTrace();
            return false; // 접속에 오류가 있으면 실패값 반환
        }

        boolean b = socket.isConnected();
        if( b == true ) {
            try {
                // 접속이 잘 되었으면 스트림 객체를 할당함
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                return false; // 스트림 객체를 얻어오는 데 이상이 있으면 실패값 반환
            }
        }
        else return b;

        // 잘 연결이 되었으면 우선 현재 클라이언트의 IP 주소를 기반으로
        // 동적 암호화 클래스의 AES 키값을 설정(XOR)
        DynamicCrypter.getInstance().setKeyByValue(addr);

        Log.d("IPADDR", addr);

        // Sender 클래스를 생성 후 스레드 시작
        socketSender = new SocketSender();
        senderThread = new Thread(socketSender);
        senderThread.start();

        // Receiver는 조금 있다가 콜백 객체를 받고 나서 스레드를 돌림
        socketReceiver = new SocketReceiver();

        // 데이터 객체 메모리 할당
        userData = new UserData();
        drawData = null;
        isGaming = false; // 게임 시작을 안 한 상태
        isDrawer = false; // 게임 시작 전에는 모두 그림 그리는 권한이 없음
        isErase = false; // 시작하자마자 그림을 지우려고 하는 정신나간 사람은 없다

        return b; // 접속 여부를 반환
    }

    // Sender 스레드에 해당 패킷을 보내라고 요청하는 메소드
    public void sendPacket(PacketMessage msg) {
        socketSender.sendPacket(msg);
    }

    // CHAT 메세지 전송 요청하는 메소드
    public void sendChatPacket(String str) {
        String chatMsg = str;
        PacketMessage msg = new PacketMessage();
        msg.makeChatPacket(chatMsg);
        socketSender.sendPacket(msg);
    }

    // JOIN 메세지 전송 요청하는 메소드
    public void sendJoinPacket(UserData data) {
        PacketMessage msg = new PacketMessage();
        msg.makeJoinPacket(data);
        socketSender.sendPacket(msg);
    }

    public void sendDrawPacket(ArrayList<DrawData> drawData)
    {
        if (!isDrawer()) return;
        PacketMessage msg = new PacketMessage();
        msg.makeDrawDataPacket(drawData);
        socketSender.sendPacket(msg);
    }

    public void getDrawPacketAndPut(ArrayList<DrawData> drawData)
    {
        if (drawData == null) isErase = true;
        else setDrawData(drawData);
    }

    // 외부 라이브러리인 jericho를 사용하는 중요한 메소드
    // 이 메소드는 자신의 공인 ip를 알려주는 사이트인 db-ip.com의
    // HTML 소스를 파싱하여, 그 중 자신의 공인 ip가 적힌 태그의 id인 search_input값으로
    // 태그를 얻어오고, 그 태그에서 ip가 적혀있는 속성인 value의 값을 String으로 꺼내는 메소드로
    // 자신의 공인 ip를 리눅스 커널의 gethostaddr() 함수와 맵핑된
    // InetAddress.getLocalHost().getHostAddress()로 얻어올 수 없는 안드로이드 기기의 특성상
    // 쓸 수밖에 없는 매우매우매우매우 중요한 메소드이다!!!!!!
    public String getLocalOfficialAddress()
    {
        String url = "https://db-ip.com/";
        Source source = null;
        try {
            source = new Source(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
            return null; // 오류가 있으면 널값을 퉤 뱉는다
        }

        Element c = (Element) source.getElementById("search_input");
        return (String) c.getAttributeValue("value");
    }

    // Receiver 스레드에서 Callback 객체를 얻도록 요청하는 메소드
    public void getCallbackInstance(SocketReceiver.ViewHandlerCallback callback) {
        if ( socketReceiver.getCallbackInstance(callback) ) {
            // 최초로 콜백 인스턴스를 전달받았을 경우
            receiverThread = new Thread(socketReceiver); // UI를 다룰 준비가 되므로
            receiverThread.start(); // Receiver 스레드를 시작

            // 모든 스레드가 잘 시작되었으므로 서버에 자신의 등장을 알림
            sendJoinPacket(userData);
        }
    }

    // 프로그램을 종료할 때 데이터를 전부 정리하기 위해 부르는 메소드
    public void finish()
    {
        // Sender Thread는 Wait 하고 있을 것이므로 무조건 강제로 interrupt를 걸어 예외처리를 통해 종료한다
        if( senderThread != null ) {
            senderThread.interrupt();
            senderThread = null;
        }
        // Receiver Thread는 종료 전에는 항상 무한 루프를 돌고 있으므로 정상적으로 interrupt를 걸어 종료한다
        if( receiverThread != null ) {
            receiverThread.interrupt();
            receiverThread = null;
        }

        if ( inputStream != null ) {
            inputStream = null;
        }
        if ( outputStream != null ) {
            outputStream = null;
        }

        if ( socketSender != null ) socketSender = null;
        if ( socketReceiver != null ) socketReceiver = null;

        if ( userData != null ) userData = null;

        try {
            socket.close(); // 전부 정리를 했다면 최종적으로 서버와 접속을 끊는다.
        } catch (IOException e) {
        }
        finally {
            Log.d("DISCONNECTED", "EXIT(0)");
            socket = null;
            address = null;
        }
    }

    // Getters
    public InetSocketAddress getAddress() {
        return address;
    }
    public InputStream getInputStream() {
        return inputStream;
    }
    public OutputStream getOutputStream() {
        return outputStream;
    }
    public UserData getUserData() {
        return userData;
    }
    public ArrayList<DrawData> getDrawData() {
        return drawData;
    }
    public boolean isGaming() { return isGaming; }
    public boolean isDrawer() {
        return isDrawer;
    }
    public boolean isErase() { return isErase; }
    public short getNowColor() { return nowColor; }


    // Setters
    public void setUserData(UserData userData) {
        this.userData = userData;
    }
    public void setDrawData(ArrayList<DrawData> drawData) {
        this.drawData = drawData;
    }
    public void setGaming(boolean gaming) { isGaming = gaming; }
    public void setDrawer(boolean drawer) {
        isDrawer = drawer;
    }
    public void setNowColor(short color) { nowColor = color; }
    public void setErase(boolean erase) { isErase = erase; }

}
