package com.forbutton;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.RelativeLayout;

public class KeyboardView extends RelativeLayout implements Runnable{
	
	public static final int MSG_VISIBLE  = 1;
	public static final int MSG_INVISIBLE = 2;
    private Thread thread;
    
	public KeyboardView(Context context) {
		super(context);
	}
	public KeyboardView(Context context, AttributeSet attrs){
		super(context, attrs);
	}
	public KeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	/**
     * �B�z thread �� handler
     * (KeyboardView).handler.sendMessage(Message);
     * @author LexLu
     */
    public Handler handler = new Handler() {
        public void handleMessage(Message msg) 
        {
	   	    switch (msg.what) 
		    {
	        // ���쪺Message���N�����ڭ̭��q���N���N���U�����ʧ@�C
	        case MSG_VISIBLE:
	        	setVisibility(View.VISIBLE);
	            invalidate();
		    break;
	        // ���쪺Message���N�����ڭ̭��q���N���N���U�����ʧ@�C
	        case MSG_INVISIBLE:
	        	setVisibility(View.INVISIBLE);
	            invalidate();
		    break;
            }
	    super.handleMessage(msg);
        }
    };
    
    // �إ� Thread�A�ö}�l
    public void init() {
        thread = new Thread(this);
        thread.start();
    }
    
    // Runnable ����
    public void run() {
        Message msg = new Message();
		// �w�q Message���N���Ahandler�~���D�o�Ӹ��X�O���O�ۤv�ӳB�z���C
		msg.what = MSG_INVISIBLE;
		handler.sendMessage(msg);
    }
}

