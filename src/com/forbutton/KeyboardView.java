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
     * 處理 thread 的 handler
     * (KeyboardView).handler.sendMessage(Message);
     * @author LexLu
     */
    public Handler handler = new Handler() {
        public void handleMessage(Message msg) 
        {
	   	    switch (msg.what) 
		    {
	        // 當收到的Message的代號為我們剛剛訂的代號就做下面的動作。
	        case MSG_VISIBLE:
	        	setVisibility(View.VISIBLE);
	            invalidate();
		    break;
	        // 當收到的Message的代號為我們剛剛訂的代號就做下面的動作。
	        case MSG_INVISIBLE:
	        	setVisibility(View.INVISIBLE);
	            invalidate();
		    break;
            }
	    super.handleMessage(msg);
        }
    };
    
    // 建立 Thread，並開始
    public void init() {
        thread = new Thread(this);
        thread.start();
    }
    
    // Runnable 介面
    public void run() {
        Message msg = new Message();
		// 定義 Message的代號，handler才知道這個號碼是不是自己該處理的。
		msg.what = MSG_INVISIBLE;
		handler.sendMessage(msg);
    }
}

