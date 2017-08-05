package com.forbutton;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
/**
 * 執行順序
 * 1.onCreate方法繼承至Service類，其意義和其他Service的是一樣的。示例在這裡，做了一些非UI方面的初始化，即字符串變量詞彙分隔符的初始化。
 * 2.onInitializeInterface，這裡是進行UI初始化的地方，創建以後和配置修改以後，都會調用這個方法。示例在這裡對Keyboard進行了初始化，從XML文件中讀取軟鍵盤信息，封裝進Keyboard對象。
 * 3.onStartInput方法，在這裡，我們被綁定到了客戶端，接收所有關於編輯對象的詳細信息。
 * 4.onCreateInputView，在用戶輸入的區域要顯示時，這個方法由框架調用，輸入法首次顯示時，或者配置信息改變時，該方法就會被執行。在該方法中，對inputview進行初始化：讀取佈局文件信息，設置onKeyboardActionListener，並初始設置keyboard。
 * 5.onCreateCandidatesView，在要顯示候選詞彙的視圖時，由框架調用。和onCreateInputView類似。在這個方式中，對candidateview 進行初始化。
 * 6.onStartInputView，將inputview和當前keyboard重新關聯起來。	
 * 
 */
public class ForButton extends InputMethodService implements Runnable{
	//private static final boolean STEP = false;
	private final String TAG = "FORBUTTON";
	private final String ZAG = "Z_FORBUTTON";
	private final String STEP = "STEP";
	private final boolean DEBUG = true;

	private final int MSG_SETSUGGESTION = 1;
	private final int MSG_SETCANDIDATEVIEWSHOWN_TRUE = 2;
	private final int MSG_SETCANDIDATEVIEWSHOWN_FALSE = 3;
	private final int MSG_SHOWENGLISHKEYBOARD = 4;
	private final int MSG_SHOWNUMBERKEYBOARD  = 5;
	private final int MSG_SHOWSYMBOLKEYBOARD  = 6;
	private final int MSG_HANDLEUNDO = 7;
	private final int MSG_HANDLEREDO = 8;
	
	private final static int TOP_MARGIN = 125;
	private final static int SIDE_MARGIN = -5;
	private final static int BOTTOM_MARGIN = -10;
	
	private final float PLUS_BUTTON_SIZE = 4;
	private final float EACH_BUTTON_SIZE = 33;
	private final int LONG_PRESS_TIME = 770;
	private final int MIN_MOVE_DISTANCE = 10;
	private final int OUT_OF_BOUND = -1;

	private int mCurrentKeyboardMode = KeyboardControl.TAIWAN_MODE;
	private boolean isCounting = false;
	
	private Message mMsg = new Message();
	private KeyboardView inputView;
	private CandidateView candidateView;
	private ExtendCandidateView extraInputView;
	private ImageView iv_LeftUp;
	private ImageView iv_LeftDown;
	private ImageView iv_RightUp;
	private ImageView iv_RightDown;

	private View mLeftArea = null;
	private View mRightArea = null;
	private ButtonView leftUp;
	private ButtonView rightUp;
	private ButtonView leftDown;
	private ButtonView rightDown;
	private ButtonView center;
	
	private RelativeLayout.LayoutParams params;
	private StringBuilder composingWord = new StringBuilder();
	private StringBuilder redoStack = new StringBuilder();
	/* Keyboard Control */
	private KeyboardControl keyboardControl = new KeyboardControl();
	private ForbuttonDictionary forbuttonDictionary;
	
	private Vibrator mVibrator;
	private TimerTask mTask;
	private Timer mTimer = new Timer();
	private int pressTimeCounter;

	private com.forbutton.Suggest mSuggest;
	private String mWordSeparators;
	private String mSentenceSeparators;
	public String mBestWord;
	
    /**
     * 做了一些非UI方面的初始化，即字符串變量詞彙分隔符的初始化
     */
	@Override public void onCreate(){
		Log.d(STEP, "onCreate");
		super.onCreate();
		mVibrator = (Vibrator)getApplication().getSystemService(VIBRATOR_SERVICE);
		mSuggest = new Suggest(this, R.raw.main);
		
		//mSuggest.setCorrectionMode(mCorrectionMode);
		forbuttonDictionary = new ForbuttonDictionary(this);
		extraInputView = new ExtendCandidateView(this);
		extraInputView.setOnSelectedEventListener(new ExtendCandidateView.OnSelectedEventListener() {
			@Override public void onSelectedEvent(int selectedIndex, String selectedWord) {
				if (DEBUG) Log.d(TAG, "selectedIndex = " + selectedIndex);
				if (selectedIndex == ExtendCandidateView.mBackIndex) {
					extraInputView.dismissExtendView();
				} else if (selectedIndex == ExtendCandidateView.mCapsIndex) {
					if (keyboardControl.getMode() == KeyboardControl.ENGLISH_MODE)
						keyboardControl.setMode(KeyboardControl.ENGLISH_BIG_MODE);
					else if (keyboardControl.getMode() == KeyboardControl.ENGLISH_BIG_MODE)
						keyboardControl.setMode(KeyboardControl.ENGLISH_MODE);
					else if (keyboardControl.getMode() == KeyboardControl.SYMBOL_NARROW_MODE)
						keyboardControl.setMode(KeyboardControl.SYMBOL_WIDE_MODE);
					else if (keyboardControl.getMode() == KeyboardControl.SYMBOL_WIDE_MODE)
						keyboardControl.setMode(KeyboardControl.SYMBOL_NARROW_MODE);
					extraInputView.setSuggestions(keyboardControl.getCandidate(mCurrentKeyboardMode));
				} else if (selectedIndex == ExtendCandidateView.mUndoIndex) {
					handleUndo();
				} else if (selectedIndex == ExtendCandidateView.mRedoIndex) {
					handleRedo();
				} else {
					if (selectedIndex >= 0) {
						if (DEBUG) Log.d(TAG, "selectedWord = " + selectedWord);
						//pickSuggestionManually(selectedIndex,selectedWord);
						//英文暫時用法
						clean();
						InputConnection ic = getCurrentInputConnection();
						ic.commitText(selectedWord, 1);
					}
				}
			}
		});
		mSuggest.setUserDictionary(forbuttonDictionary);
		mWordSeparators = getResources().getString(R.string.word_separators);
		mSentenceSeparators = getResources().getString(R.string.sentence_separators);
	}
    
	/**
     * 這裡是進行UI初始化的地方，創建以後和配置修改以後，都會調用這個方法
     */
    @Override public void onInitializeInterface() {
    	Log.d(STEP, "onInitializeInterface");
    }
    
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        Log.d(STEP, "onStartInput");
        super.onStartInput(attribute, restarting);
    }
	
    /**
	 * 在用戶輸入的區域要顯示時，這個方法由框架調用，輸入法首次顯示時，或者配置信息改變時，該方法就會被執行。
	 * 對inputview進行初始化：讀取佈局文件信息，設置onKeyboardActionListener，並初始設置keyboard。
	 * 
	 * @author Zam & Amobe
	 *  */
	@Override public View onCreateInputView(){
		Log.d(STEP, "onCreateInputView");
		//使用客製化的keyboard
		inputView = (KeyboardView)getLayoutInflater().inflate(R.layout.input, null);
		//設定鍵盤的背景色，定義為透明
		inputView.setBackgroundColor(0);
		initImageOfButton();
		layoutButtonLocation();
		initExtraArea();
		
		return inputView;
	}

	/**
	 *  候選字 view 
	 *  在要顯示候選詞彙的視圖時，由框架調用。和onCreateInputView類似。在這個方式中，對candidateview 進行初始化。
	 *  
	 *  */
	@Override public View onCreateCandidatesView() {
		Log.d(STEP, "onCreateCandidatesView");
		candidateView = new CandidateView(this);
		candidateView.setService(this);

		center.setCandidateView(candidateView);
	    leftUp.setCandidateView(candidateView);
	    rightUp.setCandidateView(candidateView);
	    leftDown.setCandidateView(candidateView);
	    rightDown.setCandidateView(candidateView);
	    
		setCandidatesViewShown(true);
		return candidateView;
	}
	
	/**
	 *  將inputview和當前keyboard重新關聯起來。
	 *  */
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        Log.d(STEP, "onStartInputView");
    	super.onStartInputView(attribute, restarting);
        clean();
    }	
	
    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        Log.d(STEP, "onFinishInput");
    	super.onFinishInput();
        setCandidatesViewShown(false);
        clean();
    }
    
	/**
	 * 1.new imageView
	 * 2.set button id
	 * 3.set button image
	 * 4.set button onTouchEvent
	 * @author Zam & Amobe
	 */	
	private void initImageOfButton(){
		Log.d(STEP, "initImageOfButton");
        List<String> suggestions = new ArrayList<String>();
		//new imageView
		
        suggestions.clear();
        suggestions = keyboardControl.getCandidate(keyboardControl.ID_CENTER);
        
		center = new ButtonView(this);
		center.setService(this);
		center.setList(suggestions);
		center.setVibrator(mVibrator);
		center.setKeyboardContorol(keyboardControl);
		center.setEnterMode(ButtonView.MODE_ENTER_ON);
		
        suggestions.clear();
        suggestions = keyboardControl.getCandidate(keyboardControl.ID_LEFTUP);
        
        leftUp = new ButtonView(this);
        leftUp.setService(this);
        leftUp.setList(suggestions);
        leftUp.setVibrator(mVibrator);
        leftUp.setKeyboardContorol(keyboardControl);
        leftUp.setOnLongPressListener(onLongPress_LeftUp);
        
        suggestions.clear();
        suggestions = keyboardControl.getCandidate(keyboardControl.ID_RIGHTUP);
        
        rightUp = new ButtonView(this);
        rightUp.setService(this);
        rightUp.setList(suggestions);
        rightUp.setVibrator(mVibrator);
        rightUp.setKeyboardContorol(keyboardControl);
        rightUp.setOnLongPressListener(onLongPress_RightUp);
        
        suggestions.clear();
        suggestions = keyboardControl.getCandidate(keyboardControl.ID_LEFTDOWN);
        
        leftDown = new ButtonView(this);
        leftDown.setService(this);
        leftDown.setList(suggestions);
        leftDown.setVibrator(mVibrator);
        leftDown.setKeyboardContorol(keyboardControl);
        leftDown.setOnLongPressListener(onLongPress_LeftDown);
        
        suggestions.clear();
        suggestions = keyboardControl.getCandidate(keyboardControl.ID_RIGHTDOWN);
        
        rightDown = new ButtonView(this);
        rightDown.setService(this);
        rightDown.setList(suggestions);
        rightDown.setVibrator(mVibrator);
		rightDown.setKeyboardContorol(keyboardControl);
		rightDown.setOnLongPressListener(onLongPress_RightDown);
		
		//set button id
		center.setButtonId(KeyboardControl.ID_CENTER);
		leftUp.setButtonId(KeyboardControl.ID_LEFTUP);
		rightUp.setButtonId(KeyboardControl.ID_RIGHTUP);
		leftDown.setButtonId(KeyboardControl.ID_LEFTDOWN);
		rightDown.setButtonId(KeyboardControl.ID_RIGHTDOWN);
	}
	
	/**
	 * add button into layout
	 * 1    2
	 * 
	 * 3    4
	 * @author Zam & Amobe
	 * @last_edit_time 2011-9-19 13:36
	 */	
	private void layoutButtonLocation(){
		Log.d(STEP, "layoutButtonLocation");
		
        RelativeLayout.LayoutParams params;
		//relativelayout版本
		//1
		params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.leftMargin = SIDE_MARGIN;
		params.topMargin = BOTTOM_MARGIN;
		inputView.addView(leftUp, params);

		//2
		params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.rightMargin = SIDE_MARGIN;
		params.topMargin = BOTTOM_MARGIN;
		inputView.addView(rightUp, params);
		
		//3
		params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.leftMargin = SIDE_MARGIN;
		params.topMargin = TOP_MARGIN;
		inputView.addView(leftDown, params);
		
		//4
		params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		params.rightMargin = SIDE_MARGIN;
		params.topMargin = TOP_MARGIN;
		inputView.addView(rightDown, params);
		
		//5
        params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		inputView.addView(center, params);
	}
	
	/**
	 * initExtraArea
	 */
	private void initExtraArea() {
		final int areaWidth = 115;
		final int areaHeight = 70;
		final int BOTTOM_MARGIN = -100;
		
		mLeftArea = new View(this);
		mRightArea = new View(this);
		
		mLeftArea.setBackgroundColor(Color.argb(25, 255, 0, 0));
		mLeftArea.setOnTouchListener(new OnTouchListener(){
			@Override public boolean onTouch(View v, MotionEvent event) {
				if (DEBUG) Log.d(TAG, "LeftAreaTouchEvent");
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (!isCounting) {
						mTask = new TimerTask() {
							@Override public void run() {
								mMsg = new Message();
								mMsg.what = MSG_HANDLEUNDO;
								if (pressTimeCounter < LONG_PRESS_TIME) {
									pressTimeCounter += 70;
									isCounting = true;
								} else {
									mVibrator.vibrate(15);
									handler.sendMessage(mMsg);
								}
							}
						}; // 偵測時的判斷
						mTimer.schedule(mTask, 0, 70); // 每0.07 秒偵測一次
					}
					break;
				case MotionEvent.ACTION_UP:
					if (isCounting) {
						pressTimeCounter = 0;
						mTimer.purge();
						mTask.cancel();
						isCounting = false;
						handleUndo();
					}
					break;
				}
				return true;
			}
		});
        params = new RelativeLayout.LayoutParams(areaWidth, areaHeight);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		params.bottomMargin = BOTTOM_MARGIN;
		inputView.addView(mLeftArea, params);
		
		mRightArea.setBackgroundColor(Color.argb(25, 255, 0, 0));
		mRightArea.setOnTouchListener(new OnTouchListener(){
			@Override public boolean onTouch(View v, MotionEvent event) {
				if (DEBUG) Log.d(TAG, "RightAreaTouchEvent");
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (!isCounting) {
						mTask = new TimerTask() {
							@Override public void run() {
								mMsg = new Message();
								mMsg.what = MSG_HANDLEREDO;
								if (pressTimeCounter < LONG_PRESS_TIME) {
									pressTimeCounter += 70;
									isCounting = true;
								} else {
									handler.sendMessage(mMsg);
								}
							}
						}; // 偵測時的判斷
						mTimer.schedule(mTask, 0, 70); // 每 0.07 秒偵測一次
					}
					break;
				case MotionEvent.ACTION_UP:
					if (isCounting) {
						pressTimeCounter = 0;
						mTimer.purge();
						mTask.cancel();
						isCounting = false;
						handleRedo();
					}
					break;
				}
				return true;
			}
		});
        params = new RelativeLayout.LayoutParams(areaWidth, areaHeight);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		params.bottomMargin = BOTTOM_MARGIN;
		inputView.addView(mRightArea, params);
	}
	
	/**
	 * 處理長按後該做那個按鈕的事件
	 * @author Homogeneous
	 * @Last_Edit_Time 2011/10/12 23:56
	 */
	private ButtonView.onLongPressListener onLongPress_LeftUp = new ButtonView.onLongPressListener() {
		@Override public void onLongPress() {
			Log.d(TAG, "Touch Event : LeftUp Long press.");
			keyboardControl.setMode(KeyboardControl.ENGLISH_MODE);
			mMsg = new Message();
			mMsg.what = MSG_SHOWENGLISHKEYBOARD;
			handler.sendMessage(mMsg);
		}
	};
	
	private ButtonView.onLongPressListener onLongPress_RightUp = new ButtonView.onLongPressListener() {
		@Override public void onLongPress() {
			Log.d(TAG, "Touch Event : RightUp Long press.");
			keyboardControl.setMode(KeyboardControl.SYMBOL_NARROW_MODE);
			mMsg = new Message();
			mMsg.what = MSG_SHOWSYMBOLKEYBOARD;
			handler.sendMessage(mMsg);
		}
	};
	
	private ButtonView.onLongPressListener onLongPress_LeftDown = new ButtonView.onLongPressListener() {
		@Override public void onLongPress() {
			Log.d(TAG, "Touch Event : LeftDown Long press.");
			keyboardControl.setMode(KeyboardControl.NUMBER_MODE);
			mMsg = new Message();
			mMsg.what = MSG_SHOWNUMBERKEYBOARD;
			handler.sendMessage(mMsg);
		}
	};
	
	private ButtonView.onLongPressListener onLongPress_RightDown = new ButtonView.onLongPressListener() {
		@Override public void onLongPress() {
			Log.d(TAG, "Touch Event : RightDown Long press.");
			//TODO BUG
			clean();
			requestHideSelf(0);
			candidateView.clear();
			composingWord.setLength(0);
			mMsg = new Message();
			mMsg.what = MSG_SETCANDIDATEVIEWSHOWN_FALSE;
			handler.sendMessage(mMsg);
		}
	};
	
    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
    	Log.d(STEP, "commitTyped");
        if (composingWord.length() > 0) {
            inputConnection.commitText(composingWord, 1);
            //composingWord.setLength(0);
//            updateCandidates();
        }
    }
	
	/** 候選字更新 
	 * 
	 * @author Zam
	 * @time 2011/7/25/ 9:21pm
//	private void updateCandidates() {
//		if (composingWord.length() >0){
//			List<String> list = new ArrayList<String>();
//			list.add(composingWord.toString());
//			Log.d(ZAG, "composingWord " + composingWord);
//            setSuggestions(list, CandidateView.BOOL_WORDTYPE, true);
//        } else {
//            setSuggestions(null, CandidateView.BOOL_WORDTYPE, false);
//		}
//	}

	/**
	 * 依據當時的鍵盤模式選擇自己的candidate候選列
	 * @author Zam
	 * @time 2011/8/19 
	 */
    public void selectShowType(){
    	Log.d(STEP, "selectShowType");
    	int[] keyCodes = new int[12];
		switch(keyboardControl.getMode()){
		case KeyboardControl.TAIWAN_MODE :
			keyCodes[0] = Integer.parseInt(candidateView.selectedCode);
			if (isAlphabet(keyCodes[0])){
				if (keyCodes[0] > 710 && keyCodes[0] < 730) {
					if (!forbuttonDictionary.isSmartSearching) {
						handleCharacter(keyCodes[0], keyCodes);
					}
				} else {
					handleCharacter(keyCodes[0], keyCodes);
				}
			} else {
				handleSeparator(keyCodes[0]);
			}
			break;
		case KeyboardControl.ENGLISH_MODE :
		case KeyboardControl.ENGLISH_BIG_MODE :
		case KeyboardControl.SYMBOL_NARROW_MODE :
			composingWord.append(candidateView.selectedWord);
			getCurrentInputConnection().commitText(String.valueOf(candidateView.selectedWord), 1);
			break;
		}
    }
    
	/**
	 * 換鍵盤時，把前一個鍵盤所輸入的字直接送出，清空composing 與 suggestion的字
	 * @author Zam
	 * @time 2011/8/19 
	 */
    private void clean(){
    	Log.d(STEP, "clean");
        mPredicting = false;
        composingWord.setLength(0);
        redoStack.setLength(0);
		//CompletionInfo ci = mCompletions[index];
		InputConnection ic = getCurrentInputConnection();
		if (ic != null) {
			ic.finishComposingText();
			//清除舊資料
			mWord.reset();
			forbuttonDictionary.clear();
		}
		if (candidateView != null)
			candidateView.setSuggestions(null, false, false);
    }
    
	/**
	 *  設定建議字 
	 *  
	 */
	public void setSuggestions(List<String> suggestions, boolean inputType,
            boolean typedWordValid) {
		Log.d(STEP, "setSuggestions");
		if (suggestions != null && suggestions.size() > 0) {
			setCandidatesViewShown(true);
		} else if (isExtractViewShown()) {
			setCandidatesViewShown(true);
		}
		if (typedWordValid) {
			setCandidatesViewShown(false);
		}
		if (candidateView != null) {
			candidateView.setSuggestions(suggestions, inputType, typedWordValid);
		}
	}
	
	/**
	 * 判斷最小移動距離  做為 LongPress 的緩衝
	 * @author Homogeneous
	 * @Last_Edit_Time 2011/08/15 21:41
	 */
	public boolean isMove(float x1, float y1, float x2, float y2) {
		Log.d(STEP, "isMove");
		double distance = Math.pow(Math.abs(x1 - x2), 2) + Math.pow(Math.abs(y1 - y2), 2);
		if (Math.pow(MIN_MOVE_DISTANCE, 2) < distance) {
			return true;
		}
		return false;
	}
	
	/**
	 * 判斷第2下按到的是哪個按鈕
	 * @author Homogeneous
	 * @Last_Edit_Time 2011/08/15 21:41
	 */
	public int getSecondButton(float x, float y) {
		Log.d(STEP, "getSecondButton");
		Log.d(TAG, "l=" + iv_LeftUp.getLeft() + ",r=" + iv_LeftUp.getRight());
		if (x > iv_LeftUp.getLeft() - PLUS_BUTTON_SIZE && x < iv_LeftUp.getRight() + PLUS_BUTTON_SIZE) {
			if (y > iv_LeftUp.getTop() - PLUS_BUTTON_SIZE && y < iv_LeftUp.getBottom() + PLUS_BUTTON_SIZE) {
				return KeyboardControl.ID_LEFTUP;	
			}
		}
		if (x > iv_RightUp.getLeft() - PLUS_BUTTON_SIZE && x < iv_RightUp.getRight() + PLUS_BUTTON_SIZE) {
			if (y > iv_RightUp.getTop() - PLUS_BUTTON_SIZE && y < iv_RightUp.getBottom() + PLUS_BUTTON_SIZE) {
				return KeyboardControl.ID_RIGHTUP;	
			}
		}
		if (x > iv_LeftDown.getLeft() - PLUS_BUTTON_SIZE && x < iv_LeftDown.getRight() + PLUS_BUTTON_SIZE) {
			if (y > iv_LeftDown.getTop() - PLUS_BUTTON_SIZE && y < iv_LeftDown.getBottom() + PLUS_BUTTON_SIZE) {
				return KeyboardControl.ID_LEFTDOWN;	
			}
		}
		if (x > iv_RightDown.getLeft() - PLUS_BUTTON_SIZE && x < iv_RightDown.getRight() + PLUS_BUTTON_SIZE) {
			if (y > iv_RightDown.getTop() - PLUS_BUTTON_SIZE && y < iv_RightDown.getBottom() + PLUS_BUTTON_SIZE) {
				return KeyboardControl.ID_RIGHTDOWN;	
			}
		}
		return -1;
	}
	
	/** TODO
	 * 當為注音或英文時，由此function為控制的
	 * @param suggestion
     * @author Zam
     * @Last_Edit_Time 2011/8/19
	 */
	private void handleCharacter(int primaryCode, int[] keyCodes) {
		Log.d(STEP, "handleCharacter");
		Log.d("handleCharacter", "isAlphabet: " + isAlphabet(primaryCode));
		Log.d("handleCharacter", "mPredictionOn: " + mPredictionOn);
		Log.d("handleCharacter", "mPredicting: " + mPredicting);
		redoStack.setLength(0);
		if (isAlphabet(primaryCode) && mPredictionOn && !mPredicting) { 
			mPredicting = true;
			forbuttonDictionary.clearAssciationWord();
			composingWord.setLength(0);
			mWord.reset();	
		}
		Log.d("handleCharacter", "mPredicting: " + mPredicting);
		
		if (mPredicting) {			
			Log.d("handleCharacter", "mComposing.length(): " + composingWord.length());
			Log.d("handleCharacter", "(char) primaryCode: " + (char) primaryCode);
			Log.d("handleCharacter", "primaryCodek " + primaryCode);
			Log.d("handleCharacter", "keyCodes: " + keyCodes);
			
			//將unicode組在一起
			composingWord.append((char) primaryCode);
			Log.d("handleCharacter", "mComposing: " + composingWord);
			//放入到word裡面   primaryCode為unicode, keyCodes為他附近的字  為int[]
			mWord.add(primaryCode, keyCodes);
			
			InputConnection ic = getCurrentInputConnection();
			//設定正在輸入的字給輸入框
			if (ic != null) {
				Log.d("handleCharacter", "ic composingWord " + composingWord);
				ic.setComposingText(composingWord, 1);
			}
		}
		
		updateSuggestions();
	}

	private void handleSeparator(int primaryCode) {
		Log.d("test", "handleSeparator");
		
		InputConnection ic = getCurrentInputConnection();
		if (ic != null) {
			ic.beginBatchEdit();
		}
		
		if (mPredicting) {
			if (primaryCode != 32){
				if (mBestWord != null){
					pickSuggestion(mBestWord);
					composingWord.append((char) primaryCode);
					ic.commitText(composingWord,1);
				} else {
					composingWord.append((char) primaryCode);
					ic.commitText(composingWord,1);
				}
				mPredicting = false;
			} else {
				if (!forbuttonDictionary.isSmartSearching) {
					forbuttonDictionary.spacePressed = true;
					updateSuggestions();
				}
			}
		} else {
			composingWord.setLength(0);
			composingWord.append((char) primaryCode);
			ic.commitText(composingWord,1);
		}
		
		if (ic != null) {
			ic.endBatchEdit();
		}
	}
	
	/**
	 * 刪除字
	 * @param suggestion
     * @author Zam
     * @Last_Edit_Time 2011/8/26
	 */
	public void handleUndo() {
		Log.d(STEP, "handleUndo");
		InputConnection ic = getCurrentInputConnection();
		if (ic == null)
			return;
			final int length = composingWord.length();
			if (length > 0 && mWord.getTypedWord() != null) {
//				redoStack.append(ic.getTextBeforeCursor(1, 0));
				forbuttonDictionary.delete(composingWord.length());		//需要寫在composinWord.delete前面
				composingWord.delete(length - 1, length);
				mWord.deleteLast();
				ic.setComposingText(composingWord, 1);
				if (composingWord.length() == 0) {
					mPredicting = false;
					forbuttonDictionary.isSmartLearning = false;
				}
				updateSuggestions();
			} else {
				redoStack.append(ic.getTextBeforeCursor(1, 0));
				ic.deleteSurroundingText(1, 0);
				composingWord.setLength(0);
				mWord.reset();
				mPredicting = false;
			}
	}

	/**
	 * 回復字
	 * @param suggestion
     * @author Zam
     * @Last_Edit_Time 2011/8/26
	 */
	public void handleRedo() {
		Log.d(STEP, "handleRedo");
		InputConnection ic = getCurrentInputConnection();
		final int redoLength = redoStack.length();
		final int composingLength = composingWord.length();
		if (ic == null)
			return;
		
		if (redoLength > 0){
			if (composingLength > 0){
				composingWord.append(redoStack.substring(redoLength - 1));
				ic.setComposingText(composingWord, 1);
				redoStack.delete(redoLength - 1, redoLength);
			}
			else {
				ic.commitText(redoStack.substring(redoLength - 1), 1);
				redoStack.delete(redoLength - 1, redoLength);
			}
			
		}
	}
	
    /** TODO 將選字寫得更完整一點
     * 選取候選字
     * @author Zam
     * @Last_Edit_Time 2011/8/19
     * */
	public void pickSuggestionManually(int index, String selectedWord) {
		Log.d(STEP, "pickSuggestionManually");
		
//		if (mCompletionOn && mCompletions != null && index >= 0 && index < mCompletions.length) {
//			CompletionInfo ci = mCompletions[index];
//			InputConnection ic = getCurrentInputConnection();
//			if (ic != null) {
//				ic.commitCompletion(ci);
//			}
//			mCommittedLength = suggestion.length();
//			if (candidateView != null) {
//				candidateView.clear();
//			}
//			return;
//		}
		
		pickSuggestion(selectedWord);
		//下一個字的候選列表 
		if(forbuttonDictionary.isSmartLearning || forbuttonDictionary.isSmartSearching){
			if (DEBUG) Log.d(TAG, "forbuttonDictionary.isSmartLearning " + forbuttonDictionary.isSmartLearning);
			if (DEBUG) Log.d(TAG, "forbuttonDictionary.isSmartSearching " + forbuttonDictionary.isSmartSearching);
			forbuttonDictionary.isPicked = true;
			forbuttonDictionary.learning(index, selectedWord);
			if (mWord.getTypedWord() != null){	
				if (forbuttonDictionary.getNumOfPhoentic(index) == 0) {
					mWord.deleteLast();
				} else {
					mWord.deletePickedCode(forbuttonDictionary.getNumOfPhoentic(index));					
				}
				composingWord.append(mWord.getTypedWord());
				if (DEBUG) Log.d(TAG, "composingWord " + composingWord);
				if (mWord.getTypedWord() == null){
					if (DEBUG) Log.d(TAG, "composingWord in " + composingWord);
					forbuttonDictionary.isSmartLearning = false;
					forbuttonDictionary.isSmartSearching = false;
					forbuttonDictionary.addNewPhraseToDB();
					forbuttonDictionary.clear();
					composingWord.setLength(0);
				}
			} 
			InputConnection ic = getCurrentInputConnection();
			if(forbuttonDictionary.isSmartLearning || forbuttonDictionary.isSmartSearching){
				if (ic != null) {
					ic.setComposingText(composingWord, 1);
				}
				updateSuggestions();
			} else {
				if (!forbuttonDictionary.isSmartSearching){
					updateAssociations(selectedWord,index);
					//清除舊資料
					mWord.reset();
					forbuttonDictionary.clear();
					mPredicting = false;
				} 
			}	
		} else {
			updateAssociations(selectedWord,index);
			//清除舊資料
			mWord.reset();
			forbuttonDictionary.clear();
			mPredicting = false;
		}	

		
		// Follow it with a space
		//if (mAutoSpace) {
		//	sendSpace();
		//sendKeyChar((char) ' ');
		//}
	}
	
	/** TODO 將選字寫得更完整一點
	 * 
	 * @param suggestion
     * @author Zam
     * @Last_Edit_Time 2011/8/19
	 */
	private void pickSuggestion(String selectedWord) {
		Log.d(STEP, "pickSuggestion");
		InputConnection ic = getCurrentInputConnection();
		if (ic != null) {
			selectedWord = selectedWord.toString().replace(" ", "");
			ic.commitText(selectedWord, 1);
		}
		forbuttonDictionary.useWordDB(selectedWord.toString());
        
		mCommittedLength = selectedWord.length();
		
		if (candidateView != null) {
			candidateView.setSuggestions(null, false, false);
		}
        composingWord.setLength(0);
		
//		if (mPredictionOn && !mPredicting) { 
//			//mPredicting = true;
//			composingWord.setLength(0);
//			mWord.reset();	
//		}
	}

	/** TODO 設定最佳的候選字
	 * 
	 * @param suggestion
     * @author Zam
     * @Last_Edit_Time 2011/8/19
	 */
	private void updateSuggestions() {
		Log.d(STEP, "updateSuggestions");
		// Check if we have a suggestion engine attached.
		if (mSuggest == null || !mPredictionOn) {
			return;
		}
		
		if (DEBUG) Log.d("TAG", "updateSuggestions composing: " + composingWord);
		List<String> stringList = listCharSequenceToListString(mSuggest.getSuggestions(inputView, mWord, false));

		boolean correctionAvailable = mSuggest.hasMinimalCorrection();
		 //|| mCorrectionMode == mSuggest.CORRECTION_FULL;
		CharSequence typedWord = mWord.getTypedWord();
		 //If we're in basic correct
		boolean typedWordValid = mSuggest.isValidWord(typedWord);
		if (mCorrectionMode == Suggest.CORRECTION_FULL) {
			correctionAvailable |= typedWordValid;
		}
		//將候選list字串傳進候選列中
		if (DEBUG) Log.d(TAG, "stringList " + stringList);
		candidateView.setSuggestions(stringList, CandidateView.BOOL_WORDTYPE, typedWordValid);
		if (stringList.size() > 0) {
			if (mWord.size() > 1){
				mBestWord = stringList.get(0);
			} else if (stringList.size() > 1){
				mBestWord = stringList.get(1);
			}
		} else {
			mBestWord = null;
		}
		
//		setCandidatesViewShown(isCandidateStripVisible() || mCompletionOn);
	}

//	/** TODO 設定最佳的候選字
//	 * 
//	 * @param suggestion
//     * @author Zam
//     * @Last_Edit_Time 2011/8/19
//	 */
//	private void updateCombinationOfSuggestion(String[] wordList) {
//		Log.d(STEP, "updateSuggestionByCombination");
//		// Check if we have a suggestion engine attached.
//		Log.d("updateSuggestions", "mSuggest: " + mSuggest);
//		if (mSuggest == null || !mPredictionOn) {
//			return;
//		}
//		//List<String> stringList = listCharSequenceToListString(mSuggest.getCombinationOfSuggestions(inputView, mWord));
//
////		boolean correctionAvailable = mSuggest.hasMinimalCorrection();
////		|| mCorrectionMode == mSuggest.CORRECTION_FULL;
//		CharSequence typedWord = mWord.getTypedWord();
//		 //If we're in basic correct
//		boolean typedWordValid = mSuggest.isValidWord(typedWord);
//		Log.d(ZAG, "typedWordValid " + mSuggest.isValidWord(typedWord));
////		if (mCorrectionMode == Suggest.CORRECTION_FULL) {
////			correctionAvailable |= typedWordValid;
////		}
//		//將候選list字串傳進候選列中
//		Log.d(TAG, "stringList " + stringList);
//		candidateView.setSuggestions(stringList, CandidateView.BOOL_WORDTYPE, typedWordValid);
//		if (stringList.size() > 0) {
//			if (mWord.size() > 1){
//				mBestWord = stringList.get(0);
//			} else if (stringList.size() > 1){
//				mBestWord = stringList.get(1);
//			}
//		} else {
//			mBestWord = null;
//		}
////		Log.d("updateSuggestions", "mCompletionOn: " + mCompletionOn);
////		setCandidatesViewShown(isCandidateStripVisible() || mCompletionOn);
//	}
	
	/** 
	 * 更新按完字後的關聯字建議列表
	 * @param selectedWord 被選擇的字,index 被選擇的字在suggestion上的順序
     * @author Zam
     * @Last_Edit_Time 2011/8/27
	 */
	private void updateAssociations(String selectedWord,int index){
		List<String> stringList = listCharSequenceToListString(mSuggest.getAssociations(inputView, mWord,selectedWord,index));

		boolean correctionAvailable = mSuggest.hasMinimalCorrection();
		 //|| mCorrectionMode == mSuggest.CORRECTION_FULL;
		CharSequence typedWord = mWord.getTypedWord();
		 //If we're in basic correct
		boolean typedWordValid = mSuggest.isValidWord(typedWord);
		if (mCorrectionMode == Suggest.CORRECTION_FULL) {
			correctionAvailable |= typedWordValid;
		}
		//將候選list字串傳進候選列中
		candidateView.setSuggestions(stringList, CandidateView.BOOL_WORDTYPE, typedWordValid);
	}
	
	/**
	 * 將List<CharSequence>轉換成List<String>
	 * @param List<CharSequence> cs
	 * @return  List<String>
	 * @author Zam
	 * @time 2011/8/19
	 * */
	private List<String> listCharSequenceToListString(List<CharSequence> cs){
		Log.d(STEP, "listCharSequenceToListString");
		List<String> s = new ArrayList<String>();
		for(int i = 0 ; i < cs.size(); i++)
			s.add(cs.get(i).toString());
		return s;
	} 

	/**
	 * 從unicode中判斷是否為字元   英文中文等...
	 * @param code 
	 * @return  boolean
	 * @author Zam
	 * @time 2011/8/19
	 * */
	private boolean isAlphabet(int code) {
		Log.d(STEP, "isAlphabet");
		if (code > 710 && code < 730){
			return true;
		}if (Character.isLetter(code)) {
			return true;
		} else {
			return false;
		}
	}
	
	/** TODO
	 * 測試用區域，先將要測試的程式碼寫在這裡，可以work後在放到上面去
	 * */

	/**
	 * 無法在service中使用
	 * @param title
	 * @return
	 */
	public AlertDialog getAlertDialog(String title) {
		Log.d(STEP, "getAlertDialog");
	    LayoutInflater factory = LayoutInflater.from(this);
	    final View view = factory.inflate(R.layout.keyboard_mode, null);
	    
		Builder builder = new AlertDialog.Builder(ForButton.this);
		builder.setTitle(title);
		builder.setView(view);
		return builder.create();
	}
	
	/**
	 * 給candidateView用
	 * @return
	 */
	public int getKeyboardMode() {
		Log.d(STEP, "getKeyboardMode");
		return keyboardControl.getMode();
	}
	
    public void onText(CharSequence text) {
    	Log.d(STEP, "onText");
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (composingWord.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
    }

	@Override public void run() {
	}
	
	@Override public void setCandidatesViewShown(boolean shown) {
		Log.d(STEP, "setCandidatesViewShown");
		// TODO: Remove this if we support candidates with hard keyboard
		if (onEvaluateInputViewShown()) {
			super.setCandidatesViewShown(shown);
		}
	}
	
	/**
	 * 主要處理在執行過程中, view 不能被更改的問題
	 * @Last-Edit-Time 2011/08/22 21:00
	 */
    public Handler handler = new Handler() {
    	
        public void handleMessage(Message msg) {
	   	    switch (msg.what) {
	   	    case MSG_SETCANDIDATEVIEWSHOWN_TRUE:
				setCandidatesViewShown(true);
		   	    break;
	   	    case MSG_SETCANDIDATEVIEWSHOWN_FALSE:
				setCandidatesViewShown(false);
		   	    break;
	   	    case MSG_SETSUGGESTION:
				List<String> list = keyboardControl.getCandidate(0);
				Log.d("ZAG", "6 : " + list);
				setSuggestions(list, CandidateView.BOOL_CODETYPE, false);
	   	    break;
	   	    case MSG_SHOWENGLISHKEYBOARD:
	   	    	extraInputView.setExtraEnglishConfigure();
	   	    	extraInputView.showExtendView(inputView, keyboardControl.getCandidate(mCurrentKeyboardMode));
   	    	break;
	   	    case MSG_SHOWNUMBERKEYBOARD:
	   	    	extraInputView.setExtraNumberConfigure();
	   	    	extraInputView.showExtendView(inputView, keyboardControl.getCandidate(mCurrentKeyboardMode));
   	    	break;
	   	    case MSG_SHOWSYMBOLKEYBOARD:
	   	    	extraInputView.setExtraSymbolConfigure();
	   	    	extraInputView.showExtendView(inputView, keyboardControl.getCandidate(mCurrentKeyboardMode));
   	    	break;
	   	    case MSG_HANDLEUNDO:
	   	    	handleUndo();
   	    	break;
	   	    case MSG_HANDLEREDO:
	   	    	handleRedo();
   	    	break;
	   	    /**
	   	     * 新增方法
	   	     * 先宣告一個 MSG 的整數
			 * private final int 'MSG' = 'int';
			 * 在裡面加 case
	   	     * case 'MSG':
	   	     * 內容
	   	     * break;
	   	     */
		    }
	    super.handleMessage(msg);
        }
    };
    
    private boolean mCompletionOn = false;
    private CompletionInfo[] mCompletions;
    private int mCommittedLength;
	public boolean mPredicting = false;
	private WordComposer mWord = new WordComposer();
	private int mCorrectionMode;
	private boolean mPredictionOn = true;
	
	/**
	 * 將List<String>轉換成List<CharSequence>
	 * @param List<String> s 
	 * @return List<CharSequence>
	 * @author Zam
	 * @Last_Edit_Time 2011/8/19
	 * */	
	private List<CharSequence> listStringToListCharSequence(List<String> s){
		Log.d(STEP, "listStringToListCharSequence");
		List<CharSequence> cs= new ArrayList<CharSequence>();
		for(int i = 0 ; i < s.size(); i++)
			cs.add(new String(s.get(i)));
		return cs;
	}
}