package com.forbutton;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

// TODO 缺少數字模式
// TODO 英文大小寫 空白鍵需加大 輸出
public class ExtendCandidateView{
	private final String TAG = "EXTENDCANDIDATEVIEW";
	private final boolean DEBUG = true;
	private final boolean DEBUG_SCROLL = false;
	private final boolean DEBUG_SELECTED = true;
	private final boolean DEBUG_SUGGESTION = false;
	OnSelectedEventListener  onSelectedEventListener  = null;

    private static final List<String> EMPTY_LIST = new ArrayList<String>();
    private static final int SCROLL_PIXELS = 20;
    private static final int MODE_DEFAULT = 1;
    private static final int MODE_ENGLISH = 2;
    private static final int MODE_NUMBER  = 3;
    private static final int MODE_SYMBOL  = 4;
    private static final String mTrimWord = "  SPACE  ";
	private static final String mBackWord = "BACK";
	public static final int mBackIndex = -2;
	private static final String mCapsWord = "CAPS";
	private static final String mCapsWordNarrow = "換半形";
	private static final String mCapsWordWide = "換全形";
	public static final int mCapsIndex = -3;
	private static final String mRedoWord = "REDO";
	public static final int mRedoIndex = -4;
	private static final String mUndoWord = "UNDO";
	public static final int mUndoIndex = -5;
	//參數
    private float X_GAP;
    private float ROW_HEIGHT;
	private float mWordPadding;				// 字與字的間格
	private int mScreenWidth, mScreenHeight;
	private int mStartY;					// 畫面的最頂端是多少		x = -1 * mDisBuffer
	private int mCurrentMode;

	private boolean isScrolled = false;
    private boolean isSelected = false;
    private boolean isBackSelected = false;
    private boolean isCapsSelected = false;
    private boolean isUndoSelected = false;
    private boolean isRedoSelected = false;
    private boolean isNarrowSymbol = false;
    
	private PopupWindow mPopupWindow = null;
	private RelativeLayout mExtendLayout = null;
	private GestureDetector mGestureDetector = null;
	
	private int mTotalHeight;
	private int mSelectedIndex = -1;
    private int colorRecommended;
	private int mTargetScrollY;
	private List<String> mSuggestions = EMPTY_LIST;
	private Paint mPaint = new Paint();
	private float mWordX[] = new float[50];
	private float mWordY[] = new float[50];
	private float mWordWidth[] = new float[50];
	private float mTouchX, mTouchY;
	private float mBackX, mBackY;
	
	public ExtendCandidateView(Context context) {
		mExtendLayout = new ExtendLayout(context);
		setDefaultConfigure();
		if (DEBUG) Log.d(TAG, "mScreenWidth = " + mScreenWidth + ", mScreenHeight = " + mScreenHeight);
		mPopupWindow.setBackgroundDrawable(null);
		mPopupWindow.setOutsideTouchable(false);
		mPopupWindow.setClippingEnabled(false);
		Resources r = context.getResources();
		colorRecommended = r.getColor(R.color.candidate_recommended);
	}
	
	private class ExtendLayout extends RelativeLayout {
		
		public ExtendLayout(Context context) {
			super(context);
	        setWillNotDraw(false);
	        
	        mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
	            @Override
	            public boolean onScroll(MotionEvent e1, MotionEvent e2,
	                    float distanceX, float distanceY) {
	            	if (DEBUG_SCROLL) Log.d(TAG, "onScroll");
	            	isScrolled = true;
	                int sy = getScrollY();
	                int disY = -1 * (int)distanceY;
	            	if (DEBUG_SCROLL) Log.d(TAG, "getHeight = " + getHeight());
	            	if (DEBUG_SCROLL) Log.d(TAG, "mTotalHeight = " + mTotalHeight);
	            	if (DEBUG_SCROLL) Log.d(TAG, "ScrollY = " + getScrollY());
	            	if (DEBUG_SCROLL) Log.d(TAG, "distanceY = " + disY);
	                sy += disY;
	                if (sy > 0) {
	                	sy = 0;
	                }
	            	if (DEBUG_SCROLL) Log.d(TAG, "1:sy = " + sy);
	                if (mScreenHeight - sy > mTotalHeight) {                    
	                	sy = mScreenHeight - mTotalHeight;
	                }
	            	if (DEBUG_SCROLL) Log.d(TAG, "2:sy = " + sy);
	                mTargetScrollY = sy;
	                scrollTo(getScrollX(), sy);
	                cleanSelection();
	                invalidate();
	                return true;
	            }
	        });
	        setHorizontalScrollBarEnabled(false);
	        setVerticalScrollBarEnabled(true);
		}
		
		@Override public int computeVerticalScrollRange() {
	        return mTotalHeight;
		}
		
		@Override protected void onDraw(Canvas canvas) {
			if (DEBUG_SUGGESTION) Log.d(TAG, "mSuggestions = " + mSuggestions.toString());
			// 抗鋸齒設定
			canvas.setDrawFilter(new PaintFlagsDrawFilter(0,
									Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
			int CenterX = getWidth()/2;
			int CenterY = getHeight()/2;
			if (DEBUG_SUGGESTION) Log.d(TAG, "CenterX = " + CenterX + ", CenterY = " + CenterY);
			int OriginX = 1;
			int OriginY = mStartY;
			isSelected = false;

			Paint _textPaint = new Paint();
			Paint _rectPaint = new Paint();
			
			_textPaint.setColor(Color.BLACK);
			_textPaint.setAntiAlias(true);
			_textPaint.setStyle(Style.STROKE);
			//大小可設為參數
			_textPaint.setTextSize(20);
			_textPaint.setStrokeWidth(0);
			
			_rectPaint.setAlpha(200);
			_rectPaint.setAntiAlias(true);
			_rectPaint.setStrokeWidth(2);
			
			mTotalHeight = 0;

			int _wordIndex = 0;
			float _x = OriginX;
			float _y = OriginY + getScrollY();
			float _edgePadding = X_GAP * 4;

			//draw BACK
			float _backPadding = -8;
			float _backEdgePadding = _edgePadding/2;
			float _backWidth = _textPaint.measureText(mBackWord)+_backEdgePadding;
			_y += _backPadding;
			RectF _rectBack = new RectF(_x, _y-ROW_HEIGHT, _x+_backWidth, _y);
			if (mTouchX >= _x && mTouchX < _x+_backWidth)
				if (mTouchY >= _y-ROW_HEIGHT && mTouchY < _y)
					isBackSelected = true;
			_rectPaint.setColor(Color.LTGRAY);
			_rectPaint.setStyle(Style.FILL);
			canvas.drawRoundRect(_rectBack, 5.0f, 5.0f, _rectPaint);
			_rectPaint.setColor(Color.DKGRAY);
			_rectPaint.setStyle(Style.STROKE);
			canvas.drawRoundRect(_rectBack, 5.0f, 5.0f, _rectPaint);
			if (isBackSelected) {
				if(DEBUG_SELECTED) Log.d(TAG, "isBackSelected draw");
				mSelectedIndex = mBackIndex;
				float _rectPadding = 2;
				RectF _backSelectedRect = new RectF(_x+_rectPadding,
													_y-ROW_HEIGHT+_rectPadding,
													_x+_backWidth-_rectPadding,
													_y-_rectPadding);
				_rectPaint.setColor(Color.DKGRAY);
				_rectPaint.setStyle(Style.FILL);
				canvas.drawRoundRect(_backSelectedRect, 5.0f, 5.0f, _rectPaint);
				isBackSelected = false;
			}
			_textPaint.setColor(Color.RED);
			_textPaint.setTypeface(Typeface.DEFAULT_BOLD);
			_y = _y - ((ROW_HEIGHT - _textPaint.getTextSize()) / 2) + _textPaint.ascent() - _backPadding;
			canvas.drawText(mBackWord, _x+_backEdgePadding/2-2, _y-_backPadding, _textPaint);
			
			if (mCurrentMode != MODE_DEFAULT) {
				//draw Undo
				_x += _backWidth + X_GAP*2;
				_y = OriginY + getScrollY() + _backPadding;
				float _undoWidth = _textPaint.measureText(mUndoWord) + _backEdgePadding;
				RectF _rectUndo = new RectF(_x, _y-ROW_HEIGHT, _x+_undoWidth, _y);
				if (mTouchX >= _x && mTouchX < _x+_undoWidth)
					if (mTouchY >= _y-ROW_HEIGHT && mTouchY < _y)
						isUndoSelected = true;
				_rectPaint.setColor(Color.LTGRAY);
				_rectPaint.setStyle(Style.FILL);
				canvas.drawRoundRect(_rectUndo, 5.0f, 5.0f, _rectPaint);
				_rectPaint.setColor(Color.DKGRAY);
				_rectPaint.setStyle(Style.STROKE);
				canvas.drawRoundRect(_rectUndo, 5.0f, 5.0f, _rectPaint);
				if (isUndoSelected) {
					if(DEBUG_SELECTED) Log.d(TAG, "isUndoSelected draw");
					mSelectedIndex = mUndoIndex;
					float _rectPadding = 2;
					RectF _undoSelectedRect = new RectF(_x+_rectPadding,
														_y-ROW_HEIGHT+_rectPadding,
														_x+_undoWidth-_rectPadding,
														_y-_rectPadding);
					_rectPaint.setColor(Color.DKGRAY);
					_rectPaint.setStyle(Style.FILL);
					canvas.drawRoundRect(_undoSelectedRect, 5.0f, 5.0f, _rectPaint);
					isUndoSelected = false;
				}
				_y = _y - ((ROW_HEIGHT - _textPaint.getTextSize()) / 2) + _textPaint.ascent() - _backPadding;
				canvas.drawText(mUndoWord, _x+_backEdgePadding/2, _y-_backPadding, _textPaint);
			//draw Redo
				_x += _undoWidth + X_GAP*2;
				_y = OriginY + getScrollY() + _backPadding;
				float _redoWidth = _textPaint.measureText(mRedoWord) + _backEdgePadding;
				RectF _rectRedo = new RectF(_x, _y-ROW_HEIGHT, _x+_redoWidth, _y);
				if (mTouchX >= _x && mTouchX < _x+_redoWidth)
					if (mTouchY >= _y-ROW_HEIGHT && mTouchY < _y)
						isRedoSelected = true;
				_rectPaint.setColor(Color.LTGRAY);
				_rectPaint.setStyle(Style.FILL);
				canvas.drawRoundRect(_rectRedo, 5.0f, 5.0f, _rectPaint);
				_rectPaint.setColor(Color.DKGRAY);
				_rectPaint.setStyle(Style.STROKE);
				canvas.drawRoundRect(_rectRedo, 5.0f, 5.0f, _rectPaint);
				if (isRedoSelected) {
					if(DEBUG_SELECTED) Log.d(TAG, "isRedoSelected draw");
					mSelectedIndex = mRedoIndex;
					float _rectPadding = 2;
					RectF _redoSelectedRect = new RectF(_x+_rectPadding,
														_y-ROW_HEIGHT+_rectPadding,
														_x+_redoWidth-_rectPadding,
														_y-_rectPadding);
					_rectPaint.setColor(Color.DKGRAY);
					_rectPaint.setStyle(Style.FILL);
					canvas.drawRoundRect(_redoSelectedRect, 5.0f, 5.0f, _rectPaint);
					isRedoSelected = false;
				}
				_y = _y - ((ROW_HEIGHT - _textPaint.getTextSize()) / 2) + _textPaint.ascent() - _backPadding;
				canvas.drawText(mRedoWord, _x+_backEdgePadding/2, _y-_backPadding, _textPaint);
			//draw Caps
				_x += _redoWidth + X_GAP*2;
				_y = OriginY + getScrollY() + _backPadding;
				String _capsWord = null;
				if (mCurrentMode == MODE_SYMBOL) {
					if (isNarrowSymbol) {
						_capsWord = mCapsWordNarrow;
					} else {
						_capsWord = mCapsWordWide;
					}
				} else
					_capsWord = mCapsWord;
				float _capsWidth = _textPaint.measureText(_capsWord) + _backEdgePadding;
				RectF _rectCaps = new RectF(_x, _y-ROW_HEIGHT, _x+_capsWidth, _y);
				if (mTouchX >= _x && mTouchX < _x+_capsWidth)
					if (mTouchY >= _y-ROW_HEIGHT && mTouchY < _y){
						isCapsSelected = !isCapsSelected;
						mSelectedIndex = mCapsIndex;
					}
				_rectPaint.setColor(Color.LTGRAY);
				_rectPaint.setStyle(Style.FILL);
				canvas.drawRoundRect(_rectCaps, 5.0f, 5.0f, _rectPaint);
				_rectPaint.setColor(Color.DKGRAY);
				_rectPaint.setStyle(Style.STROKE);
				canvas.drawRoundRect(_rectCaps, 5.0f, 5.0f, _rectPaint);
				if (isCapsSelected) {
					if(DEBUG_SELECTED) Log.d(TAG, "isCapsSelected draw");
					float _rectPadding = 2;
					RectF _capsSelectedRect = new RectF(_x+_rectPadding,
														_y-ROW_HEIGHT+_rectPadding,
														_x+_capsWidth-_rectPadding,
														_y-_rectPadding);
					_rectPaint.setColor(Color.DKGRAY);
					_rectPaint.setStyle(Style.FILL);
					canvas.drawRoundRect(_capsSelectedRect, 5.0f, 5.0f, _rectPaint);
				}
				_y = _y - ((ROW_HEIGHT - _textPaint.getTextSize()) / 2) + _textPaint.ascent() - _backPadding;
				canvas.drawText(_capsWord, _x+_backEdgePadding/2, _y-_backPadding, _textPaint);
			}
			
			//draw WORD
			_x = OriginX;
			_y = OriginY + getScrollY();
			_textPaint.setColor(Color.BLACK);
			_textPaint.setTypeface(Typeface.DEFAULT);
			for (int i = 0; _wordIndex < mSuggestions.size(); i++) {
				_x = OriginX;
				_y = OriginY + getScrollY() + i * (ROW_HEIGHT + X_GAP);
				if (DEBUG_SUGGESTION) Log.d(TAG, "OriginX = " + _x + ", OriginY = " + _y);
				for (int j = 0; _wordIndex < mSuggestions.size(); j++) {
					if (DEBUG_SUGGESTION) Log.d(TAG, "i = " + i + ", j = " + j);
					if (DEBUG_SUGGESTION) Log.d(TAG, "mSuggestions.size() = " + mSuggestions.size());
					if (DEBUG_SUGGESTION) Log.d(TAG, "_wordIndex = " + _wordIndex);
					String _suggestion = mSuggestions.get(_wordIndex);
					if (mCurrentMode == MODE_ENGLISH && _suggestion.equals(" ")) //英文模式空白增長
						_suggestion = mTrimWord;
					if (mCurrentMode == MODE_SYMBOL && (_suggestion.equals(" ") || _suggestion.equals("　"))) //符號模式空白增長
						_suggestion = mTrimWord;
					float _textWidth = _textPaint.measureText(_suggestion);
					if (mCurrentMode == MODE_ENGLISH && !_suggestion.equals(mTrimWord)) //英文模式字寬調整
						_textWidth = X_GAP * 2;
					if (mCurrentMode == MODE_NUMBER) //數字模式字寬調整
						_textWidth = X_GAP * 4;
					if (mCurrentMode == MODE_SYMBOL && _suggestion.equals(mTrimWord)) //符號模式空白增長
						_textWidth = X_GAP * 22;
					if (mCurrentMode == MODE_SYMBOL && !_suggestion.equals(mTrimWord)) //符號模式空白增長
						_textWidth = X_GAP * 2;
					float _suggestionWidth = _textWidth + _edgePadding;
					if (DEBUG_SUGGESTION) Log.d(TAG, "_suggestion = " + _suggestion);
					if (DEBUG_SUGGESTION) Log.d(TAG, "_suggestionWidth = " + _suggestionWidth);
					mWordX[_wordIndex] = _x;
					mWordY[_wordIndex] = _y;
					if (DEBUG_SUGGESTION) Log.d(TAG, "X = " + _x + ", Y = " + _y);
					mWordWidth[_wordIndex] = _textWidth;
					if (DEBUG_SUGGESTION) Log.d(TAG, "TextWidth = " + _textWidth);
					if (_x + _suggestionWidth > getWidth()) {
						if (DEBUG_SUGGESTION) Log.d(TAG, "OVER_WIDTH");
						break;
					}
					// Selected event
					if (mTouchX >= mWordX[_wordIndex] && mTouchX < mWordX[_wordIndex] + _suggestionWidth) {
						if (mTouchY >= mWordY[_wordIndex] && mTouchY < mWordY[_wordIndex] + ROW_HEIGHT + X_GAP) {
							if(DEBUG_SELECTED) Log.d(TAG, "sIndex = " + _wordIndex + ", sWord = " + _suggestion);
							isSelected = true;
							mSelectedIndex = _wordIndex;
						}
					}
					
					RectF _rect = new RectF(_x, _y, _x+_suggestionWidth, _y+ROW_HEIGHT);

					_rectPaint.setColor(Color.LTGRAY);
					_rectPaint.setStyle(Style.FILL);
					canvas.drawRoundRect(_rect, 5.0f, 5.0f, _rectPaint);
					_rectPaint.setColor(Color.DKGRAY);
					_rectPaint.setStyle(Style.STROKE);
					canvas.drawRoundRect(_rect, 5.0f, 5.0f, _rectPaint);
					// _textPaint.measureText(_suggestion) 不改成 _textWidth 是為了英文的字寬不一而用的
					_x = mWordX[_wordIndex] + (_suggestionWidth-_textPaint.measureText(_suggestion))/2;
					_y = mWordY[_wordIndex] + ((ROW_HEIGHT - _textPaint.getTextSize()) / 2) - _textPaint.ascent() - 1;
					if (DEBUG_SUGGESTION) Log.d(TAG, "T_X = " + _x + ", T_Y = " + _y);
					canvas.drawText(_suggestion, _x, _y, _textPaint);
					
					if (mSelectedIndex == _wordIndex) {
						if(DEBUG_SELECTED) Log.d(TAG, "isSelected draw");
						mSelectedIndex = _wordIndex;
						float _rectPadding = 2;
						RectF _backSelectedRect = new RectF(mWordX[_wordIndex]+_rectPadding,
															mWordY[_wordIndex]+_rectPadding,
															mWordX[_wordIndex]+_suggestionWidth-_rectPadding,
															mWordY[_wordIndex]+ROW_HEIGHT-_rectPadding);
						_rectPaint.setColor(Color.DKGRAY);
						_rectPaint.setStyle(Style.FILL);
						canvas.drawRoundRect(_backSelectedRect, 5.0f, 5.0f, _rectPaint);
						_textPaint.setColor(colorRecommended);
						_textPaint.setTypeface(Typeface.DEFAULT_BOLD);
						canvas.drawText(_suggestion, _x, _y, _textPaint);
						_textPaint.setColor(Color.BLACK);
						_textPaint.setTypeface(Typeface.DEFAULT);
					}
					
					_x = mWordX[_wordIndex] + _suggestionWidth + mWordPadding;
					_y = mWordY[_wordIndex];
					_wordIndex++;
				}
			}
			if (isSelected == false && mSelectedIndex >= 0)
				mSelectedIndex = -1;
			mTotalHeight = (int)_y - (int)getScrollY() + (int)ROW_HEIGHT;
			if (mTotalHeight < mScreenHeight)
				mTotalHeight = mScreenHeight;
			if (mTargetScrollY != getScrollY()) {
	            scrollToTarget();
			}
		}
	    
	    private void scrollToTarget() {
	        int sy = getScrollY();
	        if (mTargetScrollY > sy) {
	            sy += SCROLL_PIXELS;
	            if (sy >= mTargetScrollY) {
	                sy = mTargetScrollY;
	                requestLayout();
	            }
	        } else {
	            sy -= SCROLL_PIXELS;
	            if (sy <= mTargetScrollY) {
	                sy = mTargetScrollY;
	                requestLayout();
	            }
	        }
	        scrollTo(getScrollX(), sy);
	        invalidate();
	    }
		
		@Override public boolean onTouchEvent(MotionEvent me) {
	        if (mGestureDetector.onTouchEvent(me)) {
	            return true;
	        }
	        
			switch(me.getAction()) {
			case MotionEvent.ACTION_DOWN:
				isScrolled = false;
	            setTarget(me.getX(), me.getY());
				break;
			case MotionEvent.ACTION_MOVE:
				break;
			case MotionEvent.ACTION_UP:
				if (DEBUG) Log.d(TAG, "ACTION_UP");
	            if (!isScrolled) {
	                if (mSelectedIndex != -1) {
							onSelected(mSelectedIndex);
	                }
	            }
	            cleanSelection();
	    		mExtendLayout.invalidate();
				break;
			}
			return true;
		}
	}
	
	public void clean() {
		mTouchX = -1;
		mTouchY = -1;
		mSelectedIndex = -1;
		isScrolled = false;
		isSelected = false;
		isBackSelected = false;
		isCapsSelected = false;
		isNarrowSymbol = false;
		isRedoSelected = false;
		isUndoSelected = false;
		mSuggestions = EMPTY_LIST;
	}
	
	public void cleanSelection() {
		mTouchX = -1;
		mTouchY = -1;
		mSelectedIndex = -1;
		isSelected = false;
	}
	
	public void setTarget(float x, float y) {
		if (DEBUG) Log.d(TAG, "setTarget");
		mTouchX = x;
		mTouchY = y;
		mExtendLayout.invalidate();
	}
	
	public int getSelectionIndex() {
		return mSelectedIndex;
	}
	
	/**
	 * 顯示放大鏡
	 * @param v:要顯示放大鏡的 view
	 */
	public void showExtendView(View v, List<String> suggestions) {
		mSuggestions = new ArrayList<String>(suggestions);
        mExtendLayout.scrollTo(0, -50);
        mTargetScrollY = 0;
		mPopupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
	}
	
	public void setSuggestions(List<String> suggestions) {
		mSuggestions = new ArrayList<String>(suggestions);
	}

	public void setDefaultConfigure() {
		mCurrentMode = MODE_DEFAULT;
		X_GAP = 6;
		mStartY = 40;
		ROW_HEIGHT = 25;
		mWordPadding = 2 * X_GAP;
		mScreenWidth = 270;
		mScreenHeight = 270;
		mPopupWindow = new PopupWindow(mExtendLayout, mScreenWidth, mScreenHeight);
	}

	public void setExtraEnglishConfigure() {
		mCurrentMode = MODE_ENGLISH;
		X_GAP = (float)5;
		mStartY = 60;
		ROW_HEIGHT = 35;
		mWordPadding = X_GAP / 2;
		mScreenWidth = 295;
		mScreenHeight = 270;
		mPopupWindow = new PopupWindow(mExtendLayout, mScreenWidth, mScreenHeight);
	}

	public void setExtraNumberConfigure() {
		mCurrentMode = MODE_NUMBER;
		X_GAP = (float)9;
		mStartY = 60;
		ROW_HEIGHT = 45;
		mWordPadding = X_GAP + 8;
		mScreenWidth = 255;
		mScreenHeight = 270;
		mPopupWindow = new PopupWindow(mExtendLayout, mScreenWidth, mScreenHeight);
	}

	public void setExtraSymbolConfigure() {
		mCurrentMode = MODE_SYMBOL;
		X_GAP = (float)5;
		mStartY = 60;
		ROW_HEIGHT = 35;
		mWordPadding = X_GAP / 2;
		mScreenWidth = 295;
		mScreenHeight = 270;
		mPopupWindow = new PopupWindow(mExtendLayout, mScreenWidth, mScreenHeight);
	}
	
	/**
	 * 關閉放大鏡
	 */
	public void dismissExtendView(){
    	clean();
		mPopupWindow.dismiss();
	}
	
	private void onSelected(int selectedIndex) {
		if (onSelectedEventListener != null) {
			if (selectedIndex < 0) {
				if (selectedIndex == mCapsIndex && mCurrentMode == MODE_SYMBOL)
					isNarrowSymbol = !isNarrowSymbol;
				onSelectedEventListener.onSelectedEvent(selectedIndex, null);
			} else
				onSelectedEventListener.onSelectedEvent(selectedIndex, mSuggestions.get(selectedIndex));
		}
	}
	
	public void setOnSelectedEventListener(OnSelectedEventListener listener) {
		onSelectedEventListener = listener;
	}
	
	public interface OnSelectedEventListener extends EventListener {
		public abstract void onSelectedEvent(int selectedIndex, String selectedWord);
	}
}
