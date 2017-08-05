package com.forbutton;

import java.util.EventListener;
import java.util.List;
import java.util.Timer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.content.Context;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputConnection;

public class ButtonView extends RelativeLayout implements OnTouchListener, Runnable{

	private final String TAG = "BUTTONVIEW";
	private final boolean DEBUG = true;
	private final boolean DEBUG_SELECTION = true;
	private final int MIN_MOVE_DISTANCE = 15;
	private final int LONG_PRESS_TIME = 750;
	private final int SIDE_MARGIN = 20;
	private final int BUTTONSIZE = 125;
	private final int RADIUS = 30;
	onLongPressListener onLongPressListener = null;
	
	public static boolean MODE_ENTER_OFF = false;
	public static boolean MODE_ENTER_ON = true;
	// 開啟 Enter 判斷
	private boolean ENTER_MODE = MODE_ENTER_OFF;
	// Message
	private Message mMsg;
	public final int MSG_CLEANBUTTONVIEW = 1; 
	
	//紀錄共有幾個按鈕用來判斷扇形
	private int buttonId;
	private Context mContext;
	private ForButton mService;
	private CandidateView mCandidateView;
	private KeyboardControl mKeyboardControl;
	//放大鏡
	private Magnifier mMagnifier = null;
	
	private RelativeLayout.LayoutParams params;
	private List<String> mSuggestions = new ArrayList<String>();
	private List<String> mCodes = new ArrayList<String>();
	private int mSuggestionsSize;
	private int mSelectedIndex = -1;
	private Paint paint = new Paint();
	private ImageView mCenter, mLeftFans, mRightFans;
	private Vibrator mVibrator;
	
	private Timer mTimer = new Timer();
	private TimerTask mTask = null;
	private int pressTimeCounter = 0;
	
	private boolean isCounting = false;
	private boolean isLongPress  = false;
	private boolean isModeChange = false;
		
	public ButtonView(Context context) {
		super(context);
		if (DEBUG) Log.d(TAG, "CONSTRUCTER");
        setWillNotDraw(false);
		
        mContext = context;
        mCenter = new ImageView(context);
        mLeftFans = new ImageView(context);
        mLeftFans.setDrawingCacheEnabled(true);
        mRightFans = new ImageView(context);

        mCenter.setId(1);
        mLeftFans.setId(2);
        mRightFans.setId(3);

        setInitImageResource();
        
		params = new RelativeLayout.LayoutParams(BUTTONSIZE, BUTTONSIZE);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		
		addView(mCenter, params);
		addView(mLeftFans, params);
		addView(mRightFans, params);
		
		setOnTouchListener(this);
	}
	
	@Override protected void onDraw(Canvas canvas) {
		if (DEBUG) Log.d(TAG, "onDraw");
		if(mSuggestionsSize != 0) {
			drawSuggestions(canvas, BUTTONSIZE);
		}
	}
    
	private void drawSuggestions(Canvas canvas, int buttonSize) {
		final int TEXTSIZE = 10;
		final int CENTER_X = buttonSize / 2 - TEXTSIZE / 2 + 1;
		final int CENTER_Y = buttonSize / 2 + TEXTSIZE / 2 - 1;

		final int Y41 = -18; //-28
		final int Y42 = -6;// -10
		final int Y43 = Y42 * -1;
		final int Y44 = Y41 * -1;
		final int Y51 = -20;// -32
		final int Y52 = -11;// -18
		final int Y53 = 0;
		final int Y54 = Y52 * -1;
		final int Y55 = Y51 * -1;
		
		final int X4L1 = -18;// -37
		final int X4L2 = -24;// -47
		final int X4R1 = X4L1 * -1;
		final int X4R2 = X4L2 * -1;
		final int X5L1 = -16;// -34
		final int X5L2 = -23;// -44
		final int X5L3 = -25;// -49
		final int X5R1 = X5L1 * -1;
		final int X5R2 = X5L2 * -1;
		final int X5R3 = X5L3 * -1;
        
		paint.setColor(Color.BLACK);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(0);
		paint.setTextSize(TEXTSIZE);
		
		switch(mSuggestionsSize) {
		case 8:
			canvas.drawText(mSuggestions.get(0), CENTER_X + X4L1, CENTER_Y + Y41, paint);
			canvas.drawText(mSuggestions.get(1), CENTER_X + X4L2, CENTER_Y + Y42, paint);
			canvas.drawText(mSuggestions.get(2), CENTER_X + X4L2, CENTER_Y + Y43, paint);
			canvas.drawText(mSuggestions.get(3), CENTER_X + X4L1, CENTER_Y + Y44, paint);
			canvas.drawText(mSuggestions.get(4), CENTER_X + X4R1, CENTER_Y + Y41, paint);
			canvas.drawText(mSuggestions.get(5), CENTER_X + X4R2, CENTER_Y + Y42, paint);
			canvas.drawText(mSuggestions.get(6), CENTER_X + X4R2, CENTER_Y + Y43, paint);
			canvas.drawText(mSuggestions.get(7), CENTER_X + X4R1, CENTER_Y + Y44, paint);
			break;
		case 9:
			canvas.drawText(mSuggestions.get(0), CENTER_X + X5L1, CENTER_Y + Y51, paint);
			canvas.drawText(mSuggestions.get(1), CENTER_X + X5L2, CENTER_Y + Y52, paint);
			canvas.drawText(mSuggestions.get(2), CENTER_X + X5L3, CENTER_Y + Y53, paint);
			canvas.drawText(mSuggestions.get(3), CENTER_X + X5L2, CENTER_Y + Y54, paint);
			canvas.drawText(mSuggestions.get(4), CENTER_X + X5L1, CENTER_Y + Y55, paint);
			canvas.drawText(mSuggestions.get(5), CENTER_X + X4R1, CENTER_Y + Y41, paint);
			canvas.drawText(mSuggestions.get(6), CENTER_X + X4R2, CENTER_Y + Y42, paint);
			canvas.drawText(mSuggestions.get(7), CENTER_X + X4R2, CENTER_Y + Y43, paint);
			canvas.drawText(mSuggestions.get(8), CENTER_X + X4R1, CENTER_Y + Y44, paint);
			break;
		case 10:
			canvas.drawText(mSuggestions.get(0), CENTER_X + X5L1, CENTER_Y + Y51, paint);
			canvas.drawText(mSuggestions.get(1), CENTER_X + X5L2, CENTER_Y + Y52, paint);
			canvas.drawText(mSuggestions.get(2), CENTER_X + X5L3, CENTER_Y + Y53, paint);
			canvas.drawText(mSuggestions.get(3), CENTER_X + X5L2, CENTER_Y + Y54, paint);
			canvas.drawText(mSuggestions.get(4), CENTER_X + X5L1, CENTER_Y + Y55, paint);
			canvas.drawText(mSuggestions.get(5), CENTER_X + X5R1, CENTER_Y + Y51, paint);
			canvas.drawText(mSuggestions.get(6), CENTER_X + X5R2, CENTER_Y + Y52, paint);
			canvas.drawText(mSuggestions.get(7), CENTER_X + X5R3, CENTER_Y + Y53, paint);
			canvas.drawText(mSuggestions.get(8), CENTER_X + X5R2, CENTER_Y + Y54, paint);
			canvas.drawText(mSuggestions.get(9), CENTER_X + X5R1, CENTER_Y + Y55, paint);
			break;
		default:
			break;
		}
	}

	// TODO popup window
	private void createPopupWindow() {
		if (DEBUG) Log.d(TAG, "createPopupWindow");
		mMagnifier = new Magnifier(mContext, mService);
	}
	
	public boolean isCenterClick(float x, float y) {
		if (DEBUG) Log.d(TAG, "isCenterClick");
		double radius = Math.pow(Math.abs((BUTTONSIZE/2)-x), 2) + Math.pow(Math.abs((BUTTONSIZE/2)-y), 2);
		radius = Math.pow(radius, 0.5);
		if (radius <= RADIUS)
			return true;
		return false;
	}
	
	public boolean isLeftClick(float x) {
		if (DEBUG) Log.d(TAG, "isLeftClick");
		if (x < BUTTONSIZE/2)
			return true;
		return false;
	}
	
	/**
	 * 判斷最小移動距離  做為 LongPress 的緩衝
	 * @author Homogeneous
	 * @Last_Edit_Time 2011/08/15 21:41
	 */
	public boolean isMove(float x1, float y1, float x2, float y2) {
		if (DEBUG) Log.d(TAG, "isMove");
		double distance = Math.pow(Math.abs(x1 - x2), 2) + Math.pow(Math.abs(y1 - y2), 2);
		if (Math.pow(MIN_MOVE_DISTANCE, 2) < distance && !isLongPress) {
			return true;
		}
		return false;
	}

	public int getButtonId() {
		if (DEBUG) Log.d(TAG, "getButtonId");
		return buttonId;
	}

	public void setButtonId(int id) {
		if (DEBUG) Log.d(TAG, "setButtonId");
		buttonId = id;
	}

	public void setInitImageResource() {
		if (DEBUG) Log.d(TAG, "setInitImageResource");
        switch(mSuggestionsSize) {
		case 8:
		        setCenterImageResource(R.drawable.center1);
		        setLeftFansImageResource(R.drawable.ll4);
		        setRightFansImageResource(R.drawable.rl4);
			break;
		case 9:
		        setCenterImageResource(R.drawable.center1);
		        setLeftFansImageResource(R.drawable.ll5);
		        setRightFansImageResource(R.drawable.rl4);
			break;
		case 10:
		        setCenterImageResource(R.drawable.center1);
		        setLeftFansImageResource(R.drawable.ll5);
		        setRightFansImageResource(R.drawable.rl5);
			break;
		default:
			break;
		}
	}
	
	/*
	 * 設定多工模式 on/off
	 */
	public void setEnterMode(boolean Mode) {
		ENTER_MODE = Mode;
	}

	public void setService(ForButton fb) {
		if (DEBUG) Log.d(TAG, "setService");
		mService = fb;
		createPopupWindow();
	}
	
	public void setVibrator(Vibrator vi) {
		if (DEBUG) Log.d(TAG, "setVibrator");
		mVibrator = vi;
	}
	
	public void setKeyboardContorol(KeyboardControl kc) {
		if (DEBUG) Log.d(TAG, "setKeyboardContorol");
		mKeyboardControl = kc;
	}
	
	public void setCandidateView(CandidateView cv) {
		if (DEBUG) Log.d(TAG, "setCandidateView");
		mCandidateView = cv;
	}
	
	public void setList(List<String> suggestions) {
		if (DEBUG) Log.d(TAG, "setList");
		if (suggestions != null) {
			mCodes = new ArrayList<String>(suggestions);
			mSuggestions = new ArrayList<String>(ascii2list(suggestions));
			mSuggestionsSize = mSuggestions.size();
		}
		else
			mSuggestionsSize = 0;
		
		setInitImageResource();
		invalidate();
	}
	
	public void setCenterImageResource(int resId) {
		if (DEBUG) Log.d(TAG, "setCenterImageResource");
		mCenter.setImageResource(resId);
	}
	
	public void setLeftFansImageResource(int resId) {
		if (DEBUG) Log.d(TAG, "setLeftFansImageResource");
		mLeftFans.setImageResource(resId);
	}
	
	public void setRightFansImageResource(int resId) {
		if (DEBUG) Log.d(TAG, "setRightFansImageResource");
		mRightFans.setImageResource(resId);
	}
	
    private int whichFansSelected_8(double angle, boolean direction) {
		if (DEBUG) Log.d(TAG, "whichFansSelected_8");
    	if (!isMove(downX, downY, moveX, moveY)) {
			setRightFansImageResource(R.drawable.rb4);
			setLeftFansImageResource(R.drawable.lb4);
			return 0;
    	}
    	if (direction) {
			setRightFansImageResource(R.drawable.rb4);
		//往左邊
			if (90 > angle && angle >= 30) {
				setLeftFansImageResource(R.drawable.ls41);
				return 1;
			} else if (30 > angle  && angle >=  0) {
				setLeftFansImageResource(R.drawable.ls42);
				return 2;
			} else if (0 > angle  && angle >= -30) {
				setLeftFansImageResource(R.drawable.ls43);
				return 3;
			} else if (-30 > angle && angle >= -90) {
				setLeftFansImageResource(R.drawable.ls44);
				return 4;
			}
		} else {
			setLeftFansImageResource(R.drawable.lb4);
		//往右邊
			angle *= -1;
			if (90 > angle && angle >= 30) {
				setRightFansImageResource(R.drawable.rs41);
				return 5;
			} else if (30 > angle  && angle >=  0) {
				setRightFansImageResource(R.drawable.rs42);
				return 6;
			} else if (0 > angle  && angle >= -30) {
				setRightFansImageResource(R.drawable.rs43);
				return 7;
			} else if (-30 > angle && angle >= -90) {
				setRightFansImageResource(R.drawable.rs44);
				return 8;
			}
		}
    	return -1;
    }
    
    private int whichFansSelected_9(double angle, boolean direction) {
		if (DEBUG) Log.d(TAG, "whichFansSelected_9");
    	if (!isMove(downX, downY, moveX, moveY)) {
			setRightFansImageResource(R.drawable.rb4);
			setLeftFansImageResource(R.drawable.lb5);
			return 0;
    	}
    	if (direction) {
			setRightFansImageResource(R.drawable.rb4);
		//往左邊
			if (90 > angle && angle >= 39) {
				setLeftFansImageResource(R.drawable.ls51);
				return 1;
			} else if (39 > angle  && angle >=  13) {
				setLeftFansImageResource(R.drawable.ls52);
				return 2;
			} else if (13 > angle  && angle >= -13) {
				setLeftFansImageResource(R.drawable.ls53);
				return 3;
			} else if (-13 > angle && angle >= -39) {
				setLeftFansImageResource(R.drawable.ls54);
				return 4;
			} else if (-39 > angle && angle >= -90) {
				setLeftFansImageResource(R.drawable.ls55);
				return 5;
			}
		} else {
			setLeftFansImageResource(R.drawable.lb5);
		//往右邊
			angle *= -1;
			if (90 > angle && angle >= 30) {
				setRightFansImageResource(R.drawable.rs41);
				return 6;
			} else if (30 > angle  && angle >=  0) {
				setRightFansImageResource(R.drawable.rs42);
				return 7;
			} else if (0 > angle  && angle >= -30) {
				setRightFansImageResource(R.drawable.rs43);
				return 8;
			} else if (-30 > angle && angle >= -90) {
				setRightFansImageResource(R.drawable.rs44);
				return 9;
			}
		}
    	return -1;
    }
    
    private int whichFansSelected_10(double angle, boolean direction) {
		if (DEBUG) Log.d(TAG, "whichFansSelected_10");
    	if (!isMove(downX, downY, moveX, moveY)) {
			setRightFansImageResource(R.drawable.rb5);
			setLeftFansImageResource(R.drawable.lb5);
			return 0;
    	}
    	if (direction) {
			setRightFansImageResource(R.drawable.rb5);
		//往左邊
			if (90 > angle && angle >= 39) {
				setLeftFansImageResource(R.drawable.ls51);
				return 1;
			} else if (39 > angle  && angle >=  13) {
				setLeftFansImageResource(R.drawable.ls52);
				return 2;
			} else if (13 > angle  && angle >= -13) {
				setLeftFansImageResource(R.drawable.ls53);
				return 3;
			} else if (-13 > angle && angle >= -39) {
				setLeftFansImageResource(R.drawable.ls54);
				return 4;
			} else if (-39 > angle && angle >= -90) {
				setLeftFansImageResource(R.drawable.ls55);
				return 5;
			}
		} else {
			setLeftFansImageResource(R.drawable.lb5);
		//往右邊
			angle *= -1;
			if (90 > angle && angle >= 39) {
				setRightFansImageResource(R.drawable.rs51);
				return 6;
			} else if (39 > angle  && angle >=  13) {
				setRightFansImageResource(R.drawable.rs52);
				return 7;
			} else if (13 > angle  && angle >= -13) {
				setRightFansImageResource(R.drawable.rs53);
				return 8;
			} else if (-13 > angle && angle >= -39) {
				setRightFansImageResource(R.drawable.rs54);
				return 9;
			} else if (-39 > angle && angle >= -90) {
				setRightFansImageResource(R.drawable.rs55);
				return 10;
			}
		}
		return -1; 
    }
	
	private float downX, downY;
	private float moveX, moveY;
	private float upX, upY;
	private List<String> list;
	//缺敏感度強化
	public boolean onTouch(View v, MotionEvent event) {
		if (DEBUG) Log.d(TAG, "onTouch");
		if (mSuggestionsSize == 0)
			return false;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			downX = event.getX();
			downY = event.getY();
			if (isCenterClick(downX, downY)){
				setInitImageResource();
		        setCenterImageResource(R.drawable.center2);
				mKeyboardControl.setMode(KeyboardControl.TAIWAN_MODE);
				list = mKeyboardControl.getCandidate(buttonId);
				mService.setSuggestions(list, CandidateView.BOOL_CODETYPE, false);
				mService.setCandidatesViewShown(false);
				if (ENTER_MODE) {
					mMagnifier.setEnterMode(ENTER_MODE);
				}
				mMagnifier.setSuggestions(mSuggestions);
				mMagnifier.showMagnifier(this);
				if (!isCounting) {
					mTask = getTimerTask(); // 偵測時的判斷
					mTimer.schedule(mTask, 0, 10); // 每 0.01 秒偵測一次
				}
			}
		break;
		case MotionEvent.ACTION_MOVE:
			moveX = event.getX();
			moveY = event.getY();
			if (isCounting && isMove(downX, downY, moveX, moveY)) {
				if (DEBUG) Log.d(TAG, "Timer Stop : Pointer MOVE.");
				clearTimerTask();
			}
			
			double value = (moveY-downY) / (moveX-downX);
			//角度
			double angle = Math.atan(value)*180/Math.PI;
			//設定放大鏡的角度
			mMagnifier.setAngle(angle, (moveX-downX < 0));
			if (isCenterClick(downX, downY) && !isLongPress){
				if (mSuggestionsSize == 8)
					mSelectedIndex = whichFansSelected_8(angle, (moveX-downX < 0));
				else if (mSuggestionsSize == 9)
					mSelectedIndex = whichFansSelected_9(angle, (moveX-downX < 0));
				else if (mSuggestionsSize == 10)
					mSelectedIndex = whichFansSelected_10(angle, (moveX-downX < 0));
				//設定放大鏡的選取所引
				mMagnifier.setSelectedIndex(mSelectedIndex);
			}
		break;
		case MotionEvent.ACTION_UP:
			upX = event.getX();
			upY = event.getY();
			if (DEBUG_SELECTION) Log.d(TAG, "mSelectedIndex = " + mSelectedIndex);
			if (isMove(downX, downY, upX, upY)) {
				mCandidateView.setSelection(mSelectedIndex-1);
				mService.selectShowType();
				mService.setCandidatesViewShown(true);
			} else if (mSelectedIndex == 0 && ENTER_MODE) {
				if (DEBUG) Log.d(TAG, "Enter Handler");
				//處理 Enter 事件
				InputConnection ic = mService.getCurrentInputConnection();
				if (mService.mPredicting)
					if(mService.mBestWord != null)
						ic.commitText(mService.mBestWord, 1);
				ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
				ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
			}
			mMagnifier.dismissMagnifier();
			setInitImageResource();
			buttonUpCleanEvent();
		break;
		}
		return true;
	}
	
	/**
	 * 手指離開按鈕後的重置動作
	 * @author Homogeneous
	 * @Last-Edit-Time 2011/08/22 21:20
	 */
	private void buttonUpCleanEvent() {
		if (DEBUG) Log.d(TAG, "buttonUpCleanEvent");
		if (isCounting) {
			if (DEBUG) Log.d(TAG, "Timer Stop : Pointer UP.");
			clearTimerTask();
		}
		isLongPress = false;
		isModeChange = false;
	}
	
	/**
	 * 給計時器使用的task method, 當時間＞長按時間時，執行事件
	 * @author LexLu
	 * @Last_Edit_Time 2011/08/15  01:40
	 */
	public TimerTask getTimerTask() {
		if (DEBUG) Log.d(TAG, "getTimerTask");
		TimerTask task = new TimerTask() {
			@Override public void run() {
				if (pressTimeCounter < LONG_PRESS_TIME) {
					pressTimeCounter += 10;
					isCounting = true;
				} else {
					if (DEBUG) Log.d(TAG, "Timer Stop : End of Count.");
					mVibrator.vibrate(50);  // 震動
					onLongPress();
					clearTimerTask();
					mMsg = new Message();
					mMsg.what = MSG_CLEANBUTTONVIEW;
					handler.sendMessage(mMsg);
				}
			}
		};
		return task;
	}
	
	/**
	 * 將 Timer 停止以及將 Task 取消，並將 Timer 初始化
	 * @author LexLu
	 * @Last_Edit_Time 2011/08/15  01:40
	 */
	private void clearTimerTask() {
		if (DEBUG) Log.d(TAG, "clearTimerTask");
		pressTimeCounter = 0;
		mTimer.purge();
		mTask.cancel();
		isCounting = false;
	}
	
	/**
	 * 將傳入的ASCII字串轉為字串陣列
	 * @author Amobe
	 * @param String[] s
	 * @return ArrayList<String>
	 */
	public List<String> ascii2list(List<String> s) {
		if (DEBUG) Log.d(TAG, "ascii2list");
		List<String> list = new ArrayList<String>();
		//String[] chars = null;
		//chars = s.split(" ");
		for (int i = 0; i < s.size(); i++) {
			char c = (char)Integer.parseInt(s.get(i));
			list.add(Character.toString(c));
		}
		return list;
	}
	
	/**
	 * 呼叫後會喚醒 onLongPress Event
	 * @author LexLu
	 * @Last_Edit_Time 2011/10/15
	 */
	private void onLongPress() {
		if (DEBUG) Log.d(TAG, "onLongPress");
		
		isLongPress = true;
		if (onLongPressListener != null) {
			onLongPressListener.onLongPress();
		}
	}

	/**
	 * 設定 onLongPress Event 的 listener
	 * @author LexLu 
	 * @param listener = ButtonView.onLongPressListener
	 * @Last_Edit_Time 2011/10/15
	 */
	public void setOnLongPressListener(ButtonView.onLongPressListener listener) {
		onLongPressListener = listener;
	}

	/**
	 * 實做自訂的  listener
	 * @author LexLu
	 * 2011/10/15
	 */
	public interface onLongPressListener extends EventListener{
		public abstract void onLongPress();
	}

	@Override public void run() {}
	
	public Handler handler = new Handler(){
		public void handleMessage(Message msg){
			switch(msg.what) {
			case MSG_CLEANBUTTONVIEW:
				mMagnifier.dismissMagnifier();
				setInitImageResource();
				//buttonUpCleanEvent();
				return;
			}
		    super.handleMessage(msg);
		}
	};
}
