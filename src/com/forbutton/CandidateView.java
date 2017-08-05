package com.forbutton;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class CandidateView extends View implements Runnable{
	private final boolean DEBUG = false;
	private final String TAG = "CANDIDATEVIEW";
	private final String ZAG = "Z_CANDIDATEVIEW";
	
	public static final int MSG_SHOW = 1;
	public static final int MSG_CLEAR  = 2;
	public static final int OUT_OF_BOUNDS = -1;

	public static final boolean BOOL_CODETYPE = true;
	public static final boolean BOOL_WORDTYPE = false;

	public static final boolean BOOL_IGNOREMODE = true;
	public static final boolean BOOL_SIMPLEMODE = false;
	private static final String mIgnoreWord = "...";

    private static final List<String> EMPTY_LIST = new ArrayList<String>();
    
    private Thread thread;
	private ForButton service;
	private Context mContext;
	private ExtendCandidateView mExtendView;
    private List<String> suggestions = EMPTY_LIST;
    private List<String> mIgnoreQueue = EMPTY_LIST;
    private List<String> codes = EMPTY_LIST;
    private int selectedIndex;
    private int mBreakIndex;
    private int touchX = OUT_OF_BOUNDS;
    private int touchY = OUT_OF_BOUNDS;
    public String selectedWord = "";
    public String selectedCode = "";
    private boolean typedWordValid;
    //�ΨӧP�_���S���r�Q�ٲ�
    private boolean mShowingMode = BOOL_SIMPLEMODE;
    
    private Rect bgPadding;

    private static final int MAX_SUGGESTIONS = 50;
    private static final int SCROLL_PIXELS = 20;

    /**
     * �Կ�r��m�Ѽ�
     * @author Homogeneous
     * @���   7/26 
     */
    private int[] wordWidth = new int[MAX_SUGGESTIONS];
    private int[] wordX     = new int[MAX_SUGGESTIONS];
    
    private static final int X_GAP = 8;

    
	// �C��
    private int colorNormal;
    private int colorRecommended;
    // 
    private int verticalPadding; 
	// �e��
    private Paint paint;
    private boolean scrolled;
    private int targetScrollX;
    // 
    private int totalWidth = 0;
    private int totalHeight = 0;
    
    private GestureDetector gestureDetector;
    
	public CandidateView(Context context) {
		super(context);
		mContext = context;
		setBackgroundDrawable(null);
		mExtendView = new ExtendCandidateView(mContext);
		mExtendView.setOnSelectedEventListener(new ExtendCandidateView.OnSelectedEventListener() {
			@Override public void onSelectedEvent(int sIndex, String sWord) {
				mExtendView.dismissExtendView();
				if (suggestions.size() != 0) {
					service.setCandidatesViewShown(true);
					onDraw(null);
				}
				if (DEBUG) Log.d(TAG, "Break Index = " + mBreakIndex);
				if (sIndex >= 0) {
					if (DEBUG) Log.d(TAG, "Extend Selected Index = " + sIndex);
					if (DEBUG) Log.d(TAG, "Extend Selected Word  = " + suggestions.get(mBreakIndex + sIndex));
					selectedIndex = sIndex + mBreakIndex;
					selectedWord  = suggestions.get(selectedIndex);
					service.pickSuggestionManually(selectedIndex,selectedWord);
				}
			}
		});
		
		Resources r = context.getResources();
		// �]�w�I���C��
		setBackgroundColor(r.getColor(R.color.candidate_background));
		
		colorNormal = r.getColor(R.color.candidate_normal);
		colorRecommended = r.getColor(R.color.candidate_recommended);
		verticalPadding = r.getDimensionPixelSize(R.dimen.candidate_vertical_padding);
		// �r���C��]�w
		paint = new Paint();
		paint.setColor(colorNormal);
		paint.setAntiAlias(true);
		paint.setTextSize(r.getDimension(R.dimen.candidate_font_height));
		paint.setStrokeWidth(0);
		
		gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                    float distanceX, float distanceY) {
                return true;
            }
        });
		// View �]�w
        setHorizontalFadingEdgeEnabled(true);
        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
	}
		
	public void setService(ForButton listener) {
		service = listener;
	}
	
	// �o�n�����ݭn�Ψ�F
	@Override public int computeHorizontalScrollRange() {
		return totalWidth;
	}
	
	@Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (DEBUG) Log.d(TAG, "onMeasure");
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (DEBUG) Log.d(TAG, "widthMeasureSpec = " + widthMeasureSpec + ", heightMeasureSpec = " + heightMeasureSpec);
		if (DEBUG) Log.d(TAG, "widthGetSize = " + MeasureSpec.getSize(widthMeasureSpec) + ", heightGetSize = " + MeasureSpec.getSize(heightMeasureSpec));
		int measuredWidth = resolveSize(50, widthMeasureSpec);
		if (DEBUG) Log.d(TAG, "measuredWidth = " + resolveSize(400, widthMeasureSpec));
		
		// ���omenu�һݪ�����
		Rect padding = new Rect();
		final int desiredHeight = ((int)paint.getTextSize()) + verticalPadding
				+ padding.top + padding.bottom + 4;

		if (DEBUG) Log.d(TAG, "suggestionsSize = " + suggestions.size());
		
		setMeasuredDimension(measuredWidth, resolveSize(desiredHeight, heightMeasureSpec));
		if (DEBUG) Log.d(TAG, "finalWidth = " + getWidth() + ", finalHeight = " + getHeight());
	}
	/**
	 * onDraw
	 * @author LexLu
	 * @last_edit_time 2011-8-2 19:14
	 */
	@Override protected void onDraw(Canvas canvas) {
		if (canvas != null) {
			super.onDraw(canvas);
		}
		if (suggestions == null) return;
		
		if (bgPadding == null) {
			bgPadding = new Rect(0, 0, 0, 0);
			if (getBackground() != null) {
				getBackground().getPadding(bgPadding);
			}
		}
		//�O�_�����
		boolean isSelected = false;
		//�Կ�r�Ӽ�
		int _count  = suggestions.size();
		final int _height = getHeight()-2;
		final Paint _textPaint = paint;
		//�ثe��쪺�y��
		final int _touchX  = touchX;
        //�Ĥ@�Ӧr�e���ťհϰ�e��
        final int space = 20;
        final int MaxWidth = getMeasuredWidth();
        
		int _x = 0 + space;
		int _y = 0;

		Paint _rectPaint = new Paint();
		_rectPaint.setAlpha(200);
		_rectPaint.setAntiAlias(true);
		_rectPaint.setStrokeWidth(2);
		
		
		for (int i = 0; i < _count; i++) {
			totalWidth = 0;
			// �Կ�r
			String _suggestion = suggestions.get(i);
 			// �r�e
			float _textWidth = _textPaint.measureText(_suggestion);
			// ��r�ؼe�� = 20 + �r�e
			final int _wordWidth = (int)paint.measureText(_suggestion) + X_GAP * 2;
			final int _wordIndex = i;				
			// wordX & wordY ������I
        	// ��i�Ӧr��x, y�y��
			wordX[i] = _x;
			wordWidth[i] = _wordWidth;
						
			// _x, _y ���n�L�X�r���۹��m
			_x = wordX[i] + (int) ((wordWidth[i] - _textWidth) / 2);
			// �P�_������suggestion���S���W�Lcandidate���e��
			if (wordX[i] + wordWidth[i] + _textPaint.measureText(mIgnoreWord) + 10 > getWidth()) {
				mShowingMode = CandidateView.BOOL_IGNOREMODE;
				mBreakIndex = i;
				_count = mBreakIndex;
				setIgnoreQueue(suggestions);
				// �]�wIgnoreMode��[...]����l��
				_suggestion  = mIgnoreWord;
				_textWidth   = _textPaint.measureText(_suggestion);
				wordWidth[i] = (int)paint.measureText(_suggestion) + X_GAP * 2;
				_x = wordX[i] + (int)((wordWidth[i] - _textWidth) / 2);
			}
			// ���H2�O�n�N��r�����ܤ�
			_y = (int) (((_height - paint.getTextSize()) / 2) - paint.ascent());
			
        	_textPaint.setColor(colorNormal);

        	//_touchX + _scrollX �O�ϥΪ̫��U����m�A��_x�M_x + _wordWidth�ӧP�_�O�_�b�خؤ�
        	//�p�G�O�h�]�w��Юت��j�p�P�n�}�l�e����m
	        if (_touchX >= wordX[i] && _touchX < wordX[i] + wordWidth[i]) {
        		selectedWord = _suggestion;
        		selectedIndex = _wordIndex;
        		isSelected = true;
        	}
        	if (canvas != null) {
				RectF _rect = new RectF(wordX[i], 2, wordX[i]+wordWidth[i], _height);

				_rectPaint.setColor(Color.LTGRAY);
				_rectPaint.setStyle(Style.FILL);
				canvas.drawRoundRect(_rect, 5.0f, 5.0f, _rectPaint);
				_rectPaint.setColor(Color.DKGRAY);
				_rectPaint.setStyle(Style.STROKE);
				canvas.drawRoundRect(_rect, 5.0f, 5.0f, _rectPaint);
				
        		if (_wordIndex == selectedIndex) {
        			paint.setFakeBoldText(true);
        			paint.setColor(colorRecommended);

					float _rectPadding = 2;
					RectF _backSelectedRect = new RectF(wordX[_wordIndex]+_rectPadding, _rectPadding,
														wordX[_wordIndex]+wordWidth[_wordIndex]-_rectPadding,
														_height-_rectPadding);

					_rectPaint.setColor(Color.DKGRAY);
					_rectPaint.setStyle(Style.FILL);
					canvas.drawRoundRect(_backSelectedRect, 5.0f, 5.0f, _rectPaint);
        		} else if (_wordIndex != 0) {
        			paint.setColor(colorNormal);
        		}
        		
        		canvas.drawText(_suggestion, _x, _y, _textPaint);
        		paint.setColor(colorNormal);
        		
        		paint.setFakeBoldText(false);
        	}
        	//_x �֥[���U�@�Ӧr��
        	_x = wordX[i] + wordWidth[i];
		}
        totalWidth = _x;
		_x = 0;
		if (isSelected == false)
			selectedIndex = -1;
	}

    public static Integer getMaxSuggest() {
        return MAX_SUGGESTIONS;
    }    
    
	public void setSuggestions(List<String> _suggestions, boolean _inputType,
			boolean _typeWordValid) {
		clear();
		if (_suggestions != null) {
			if (_inputType == BOOL_CODETYPE) {
				codes = _suggestions;
				suggestions = new ArrayList<String>(ascii2list(_suggestions));
			} else {
				suggestions = new ArrayList<String>(_suggestions);
			}
			if (_suggestions.size() != 0)
				service.setCandidatesViewShown(true);
			else
				service.setCandidatesViewShown(false);
		} else
			service.setCandidatesViewShown(false);
		
		if (service.getKeyboardMode() == KeyboardControl.TAIWAN_MODE && suggestions.size() > 10) {
			mShowingMode = CandidateView.BOOL_IGNOREMODE;
			mBreakIndex = 10;
			setIgnoreQueue(suggestions);
		}
		
		typedWordValid = _typeWordValid;
		scrollTo(0, 0);
		targetScrollX = 0;
		// �p���`�e��
		onDraw(null);
		invalidate();
		requestLayout();
	}
	
	/**
	 * �N�h�l���r�s�_��, �æb�i�}�Ҧ��ϥ�
	 * @param _suggestions
	 */
	private void setIgnoreQueue(List<String> _suggestions) {
		if (DEBUG) Log.d(TAG, "setIgnoreQueue");
		mIgnoreQueue = new ArrayList<String>();
		for (int i = mBreakIndex; i < _suggestions.size(); i++) {
			mIgnoreQueue.add(_suggestions.get(i));
		}
		if (DEBUG) Log.d(TAG, "_suggestions = " + _suggestions.toString());
		if (DEBUG) Log.d(TAG, "mIgnoreQueue = " + mIgnoreQueue.toString());
	}

	/**
	 * �]�w�ثe���s����Կ���������@��
	 * @author Zam 
	 */
	public void setTarget(float x, float y){
		
		touchX = (int) x;
		touchY = (int) y;
		invalidate();
	}
	
	/**
	 * �]�w��쪺�r
	 * @author Lex
	 */
	public void setSelection(int index){
		if (suggestions.size() > 0 && suggestions.size()  >= index){
			selectedIndex = index;
			selectedWord = suggestions.get(index);
			selectedCode = codes.get(index);
		}
	}
	
	public void clear() {
		suggestions = EMPTY_LIST;
		touchX = OUT_OF_BOUNDS;
		touchY = OUT_OF_BOUNDS;
		selectedIndex = -1;
		mShowingMode = CandidateView.BOOL_SIMPLEMODE;
		invalidate();
	}
	
	@Override public boolean onTouchEvent(MotionEvent event) {
		/*
		if (gestureDetector.onTouchEvent(event)) {
			return true;
		}
		*/
		int action = event.getAction();
		int x = (int) event.getX();
		int y = (int) event.getY();
		touchX = x;
		touchY = y;
		
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			scrolled = false;
			setTarget(x, y);
			break;
		case MotionEvent.ACTION_MOVE:
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			if (selectedIndex >= 0) {
				if (selectedWord.equals(mIgnoreWord)) {
					if (DEBUG) Log.d(TAG, "Show Extend Candidate View");
					mExtendView.showExtendView(this, mIgnoreQueue);
					service.setCandidatesViewShown(false);
				} else {
					service.pickSuggestionManually(selectedIndex,selectedWord);
				}
			}
			selectedIndex = -1;
			mBreakIndex = -1;
			removeHighlight();
			requestLayout();
			break;
		}
		return true;
	}
	
	private void removeHighlight() {
		touchX = OUT_OF_BOUNDS;
		touchY = OUT_OF_BOUNDS;
		invalidate();
	}
	
	/**
     * �B�z thread �� handler
     * (CandidateView).handler.sendMessage(Message);
     * @author LexLu
     */
    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
	   	    switch (msg.what) {
	        // ���쪺Message���N�����ڭ̭��q���N���N���U�����ʧ@�C
	   	    case MSG_SHOW:
	        	setVisibility(View.VISIBLE);
	   	    break;
	        case MSG_CLEAR:
	        	setVisibility(View.GONE);
	            clear();
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
    public void run() {}

	/**
	 * �N�ǤJ��ASCII�r���ର�r��}�C
	 * @author Amobe
	 * @param String[] s
	 * @return ArrayList<String>
	 */
	public List<String> ascii2list(List<String> s) {
		List<String> list = new ArrayList<String>();
		//String[] chars = null;
		//chars = s.split(" ");
		for (int i = 0; i < s.size(); i++) {
			char c = (char)Integer.parseInt(s.get(i));
			list.add(Character.toString(c));
		}
		return list;
	}
		
}
