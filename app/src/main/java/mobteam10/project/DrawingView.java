package mobteam10.project;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

/**
 * Created by JHG on 2017-11-18.
 */

// 그림판 기능을 담당하는 SurfaceView
public class DrawingView extends SurfaceView implements SurfaceHolder.Callback {

    public static final short COLOR_RED = 0;
    public static final short COLOR_GREEN = 1;
    public static final short COLOR_BLUE = 2;

    private SocketHandler socketHandler;
    private DrawingThread thread; // SurfaceView에 그리는 작업을 담당하는 스레드
    private Context context;    // 컨텍스트 객체 참조용
    private SurfaceHolder holder;

    private ArrayList<ArrayList<DrawData>> drawDatas; // 지금까지 그린 모든 그림 데이터
    private ArrayList<DrawData> drawData; // 현재 드래그로 만들어진 그림 데이터
    private Paint paint; // 페인트(색상, 굵기 지정)
    private Paint whitePaint; // 백그라운드용 하얀 페인트

    class DrawingThread extends Thread {
        SurfaceHolder mHolder;   // SurfaceView에 접근하기 위한 Holder
        boolean waitFlag;

        public DrawingThread(Context context, SurfaceHolder holder)
        {
            this.mHolder = holder;
            Resources res = context.getResources();

            drawDatas = new ArrayList<ArrayList<DrawData>>();
            drawData = new ArrayList<DrawData>();

            waitFlag = false;
        }

        @Override
        public void run()
        {
            while(true)
            {
                if (waitFlag) {
                    try {
                        synchronized (this){ wait(); }
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                getDataFromSocket();

                Canvas canvas = mHolder.lockCanvas();
                if (canvas == null) continue;

                synchronized (mHolder)
                {
                    canvas.drawRect(0, 0, 2000, 2000, whitePaint);
                    drawAll(canvas);
                    drawLine(canvas, drawData);
                }

                mHolder.unlockCanvasAndPost(canvas);

                try {
                    Thread.sleep(16); // 초당 60 프레임 유지를 위한 sleep
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        public void drawAll(Canvas canvas) {
            synchronized (drawDatas) {
                for (ArrayList<DrawData> data : drawDatas)
                    drawLine(canvas, data);
            }
        }

        public void drawLine(Canvas canvas, ArrayList<DrawData> data)
        {
            if ( data.isEmpty() ) return;
            int c = data.size();
            for(int i=0; i<c-1; i++)
            {
                DrawData p1 = data.get(i);
                DrawData p2 = data.get(i+1);
                switch (p1.getColor()) {
                    case COLOR_RED:
                        paint.setColor(Color.RED);
                        break;
                    case COLOR_GREEN:
                        paint.setColor(Color.GREEN);
                        break;
                    case COLOR_BLUE:
                        paint.setColor(Color.BLUE);
                        break;
                }
                canvas.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY(), paint);
            }
        }

        public void wakeThread() { this.notify(); }
    }

    public DrawingView(Context context, AttributeSet attr) {
        super(context, attr);

        socketHandler = SocketHandler.getClassInstance();
        this.context = context;
        holder = getHolder();
        holder.addCallback(this);

        thread = new DrawingThread(context, holder);

        paint = new Paint();
        paint.setColor(Color.RED); // 색상 빨강
        paint.setStrokeWidth(5); // 굵기 3
        paint.setAntiAlias(true); // 안티 앨리어싱 적용

        whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        if ( socketHandler.isGaming() && !socketHandler.isDrawer() ) return true;
        int action = event.getAction();

        if ( action == MotionEvent.ACTION_DOWN
                || action == MotionEvent.ACTION_MOVE )
        {
            DrawData pt = new DrawData((short) event.getX(), (short) event.getY(),
                    socketHandler.getNowColor());
            drawData.add(pt);
        }
        else if ( action == MotionEvent.ACTION_UP )
        {
            ArrayList<DrawData> temp = drawData;
            synchronized (drawDatas) {
                drawDatas.add(temp);
            }
            socketHandler.sendDrawPacket(temp);
            drawData = new ArrayList<DrawData>();
        }

        return true;
    }

    public void getDataFromSocket()
    {
        if(socketHandler.isErase()) {
            synchronized (drawDatas) {
                drawDatas.clear();
            }
            drawData.clear();
            socketHandler.setErase(false);
        }

        if (socketHandler.getDrawData() != null)
        {
            synchronized (drawDatas) {
                drawDatas.add(socketHandler.getDrawData());
            }
            socketHandler.setDrawData(null);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Thread.State state = thread.getState();
        if (state == Thread.State.NEW) // 스레드가 처음 만들어진 상태면 시작만 해준다
            thread.start();
        else { // 스레드가 어떠한 이유로 중지된 상태면 다시 시작을 해 준다
            thread.waitFlag = false;
            synchronized (thread) { thread.notify(); }
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        thread.waitFlag = true;
    }
}
