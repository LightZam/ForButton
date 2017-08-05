package com.forbutton;

import java.util.ArrayList;
import java.util.List;

import com.forbutton.R.color;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Path.FillType;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

public class Magnifier{
	private final String TAG = "MAGNIFIER";
	private final boolean DEBUG = true;
	private final boolean DEBUG_SELECTION = true;
	private final int BUTTONSIZE = 200;
	private final int TEXTSIZE = 15;
	
	private boolean ENTER_MODE = false;
	private String mWordEnter = "Enter";

    private ForButton mService = null;
	private MagnifierLayout mMagnifier = null;
	private PopupWindow mPopupWindow = null;
	private WindowManager mWindowManager=null;

	private List<String> mSuggestions = new ArrayList<String>();
	private int mSelectedIndex = -1;
	private double mAngle = 0; 
	private boolean mDirection = true;
	
	private RelativeLayout.LayoutParams params = null;
	private Paint paint = new Paint();
	
	private int mScreenWidth, mScreenHeight;
	
	public Magnifier(Context context, ForButton service) {
        mService = service;
        
		Object obj = mService.getSystemService(Context.WINDOW_SERVICE);
		mWindowManager = (WindowManager)obj;
		mScreenWidth = mWindowManager.getDefaultDisplay().getWidth();
		mScreenHeight = mWindowManager.getDefaultDisplay().getHeight();
		if (DEBUG) Log.d(TAG, "mScreenWidth = " + mScreenWidth + ", mScreenHeight = " + mScreenHeight);
		
		mMagnifier = new MagnifierLayout(mService.getApplicationContext());
		
		params = new RelativeLayout.LayoutParams(BUTTONSIZE, BUTTONSIZE);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		
		mPopupWindow = new PopupWindow(mMagnifier, mScreenWidth, mScreenHeight);
		mPopupWindow.setBackgroundDrawable(null);
		mPopupWindow.setOutsideTouchable(false);
		mPopupWindow.setClippingEnabled(false);
	}
	
	private class MagnifierLayout extends RelativeLayout {
		private Path insideCircle;
		private int INSIDE_ARC_SIZE = 60;
		private int OUTSIDE_ARC_SIZE = 90;
		private int CIRCLE_SIZE = 75;
		
		public MagnifierLayout(Context context) {
			super(context);
	        setWillNotDraw(false);
	        insideCircle = new Path();
	        insideCircle.addCircle(mScreenWidth/2, mScreenHeight/2, CIRCLE_SIZE, Direction.CW);
		}
		
		@Override protected void onDraw(Canvas canvas) {
			//抗鋸齒
			canvas.setDrawFilter(new PaintFlagsDrawFilter(0,
					Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
			if (mSelectedIndex > 0)
				drawCursor(canvas);
			
			paint.setColor(Color.GRAY);
			paint.setStyle(Style.STROKE);
			paint.setStrokeWidth(30);
			canvas.drawPath(insideCircle, paint);
			
			drawSelectedArc(canvas);
			
			if (mSuggestions.size() != 0) {
				drawSuggestions(canvas);
			}
		}
		
		private void drawCursor(Canvas canvas) {
			if (DEBUG) Log.d(TAG, "drawCursor");
			int centerX = mScreenWidth/2;
			int centerY = mScreenHeight/2;

			double insidePoint = 85;
			double outsidePoint = 105;
			
			paint.setColor(Color.RED);
			paint.setStyle(Style.STROKE);
			paint.setStrokeWidth(2);
			//angle to radians
			double radians = mAngle * Math.PI / 180;
			double Lradians = (mAngle+4) * Math.PI / 180;
			double Rradians = (mAngle-4) * Math.PI / 180;
			
			double midX = Math.cos(radians) * outsidePoint;
			double midY = Math.sin(radians) * outsidePoint;
			double leftX = Math.cos(Lradians) * insidePoint;
			double leftY = Math.sin(Lradians) * insidePoint;
			double rightX = Math.cos(Rradians) * insidePoint;
			double rightY = Math.sin(Rradians) * insidePoint;			
			
			Path cursor = new Path();
			cursor.moveTo(centerX - (float)leftX , centerY - (float)leftY);
			cursor.lineTo(centerX - (float)midX  , centerY - (float)midY);
			cursor.lineTo(centerX - (float)rightX, centerY - (float)rightY);
			
			canvas.drawPath(cursor, paint);
		}
		
		private void drawSelectedArc(Canvas canvas) {
			Path selectedRect = new Path();
			float sweepAngle = 0;
			float startAngle = 0;
			
			paint.setColor(Color.BLACK);
			paint.setStyle(Style.STROKE);
			paint.setStrokeWidth(30);
			paint.setAlpha(100);
			
			RectF outsideArc = new RectF(mScreenWidth/2-OUTSIDE_ARC_SIZE, mScreenHeight/2-OUTSIDE_ARC_SIZE
					, mScreenWidth/2+OUTSIDE_ARC_SIZE, mScreenHeight/2+OUTSIDE_ARC_SIZE);
			
			RectF insideArc = new RectF(mScreenWidth/2-INSIDE_ARC_SIZE, mScreenHeight/2-INSIDE_ARC_SIZE
					, mScreenWidth/2+INSIDE_ARC_SIZE, mScreenHeight/2+INSIDE_ARC_SIZE);
			
			RectF circleArc = new RectF(mScreenWidth/2-CIRCLE_SIZE, mScreenHeight/2-CIRCLE_SIZE
					, mScreenWidth/2+CIRCLE_SIZE, mScreenHeight/2+CIRCLE_SIZE);
			//canvas.drawArc(outsideArc, 180, (float)mAngle, false, paint);
			
			if (mSelectedIndex == 0) {
				if (DEBUG_SELECTION) Log.d(TAG, "mSelectedIndex = " + mSelectedIndex);
				Paint _circlePaint = new Paint();
				_circlePaint.setColor(Color.GRAY);
				_circlePaint.setStyle(Style.FILL);
				_circlePaint.setAlpha(200);
				canvas.drawCircle(mScreenWidth/2, mScreenHeight/2, 50, _circlePaint);
				_circlePaint.setColor(Color.BLACK);
				_circlePaint.setStyle(Style.STROKE);
				_circlePaint.setStrokeWidth(5);
				_circlePaint.setAlpha(150);
				canvas.drawCircle(mScreenWidth/2, mScreenHeight/2, 50-_circlePaint.getStrokeWidth()/2, _circlePaint);
				if (ENTER_MODE) {
					paint.setColor(Color.RED);
					paint.setAntiAlias(true);
					paint.setStrokeWidth(0);
					paint.setTextSize(TEXTSIZE+10);
					float textWidth = paint.measureText(mWordEnter);
					canvas.drawText(mWordEnter, mScreenWidth/2 - textWidth/2, mScreenHeight/2 + TEXTSIZE/2, paint);
				}
				return;
			}
			
			switch (mSuggestions.size()) {
			case 8:
				sweepAngle = SelectedAngle.SWEEPANGLE_8;
				switch (mSelectedIndex) {
				case 1:
					startAngle = SelectedAngle.SELECTION_8_1;
					break;
				case 2:
					startAngle = SelectedAngle.SELECTION_8_2;
					break;
				case 3:
					startAngle = SelectedAngle.SELECTION_8_3;
					break;
				case 4:
					startAngle = SelectedAngle.SELECTION_8_4;
					break;
				case 5:
					startAngle = SelectedAngle.SELECTION_8_5;
					break;
				case 6:
					startAngle = SelectedAngle.SELECTION_8_6;
					break;
				case 7:
					startAngle = SelectedAngle.SELECTION_8_7;
					break;
				case 8:
					startAngle = SelectedAngle.SELECTION_8_8;
					break;
				}
				break;
			case 9:
				switch (mSelectedIndex) {
				case 1:
					sweepAngle = SelectedAngle.SWEEPANGLE_9_1;
					startAngle = SelectedAngle.SELECTION_9_1;
					break;
				case 2:
					sweepAngle = SelectedAngle.SWEEPANGLE_9_1;
					startAngle = SelectedAngle.SELECTION_9_2;
					break;
				case 3:
					sweepAngle = SelectedAngle.SWEEPANGLE_9_1;
					startAngle = SelectedAngle.SELECTION_9_3;
					break;
				case 4:
					sweepAngle = SelectedAngle.SWEEPANGLE_9_1;
					startAngle = SelectedAngle.SELECTION_9_4;
					break;
				case 5:
					sweepAngle = SelectedAngle.SWEEPANGLE_9_1;
					startAngle = SelectedAngle.SELECTION_9_5;
					break;
				case 6:
					sweepAngle = SelectedAngle.SWEEPANGLE_9_2;
					startAngle = SelectedAngle.SELECTION_9_6;
					break;
				case 7:
					sweepAngle = SelectedAngle.SWEEPANGLE_9_2;
					startAngle = SelectedAngle.SELECTION_9_7;
					break;
				case 8:
					sweepAngle = SelectedAngle.SWEEPANGLE_9_2;
					startAngle = SelectedAngle.SELECTION_9_8;
					break;
				case 9:
					sweepAngle = SelectedAngle.SWEEPANGLE_9_2;
					startAngle = SelectedAngle.SELECTION_9_9;
					break;
				}
				break;
			case 10:
				sweepAngle = SelectedAngle.SWEEPANGLE_10;
				switch (mSelectedIndex) {
				case 1:
					startAngle = SelectedAngle.SELECTION_10_1;
					break;
				case 2:
					startAngle = SelectedAngle.SELECTION_10_2;
					break;
				case 3:
					startAngle = SelectedAngle.SELECTION_10_3;
					break;
				case 4:
					startAngle = SelectedAngle.SELECTION_10_4;
					break;
				case 5:
					startAngle = SelectedAngle.SELECTION_10_5;
					break;
				case 6:
					startAngle = SelectedAngle.SELECTION_10_6;
					break;
				case 7:
					startAngle = SelectedAngle.SELECTION_10_7;
					break;
				case 8:
					startAngle = SelectedAngle.SELECTION_10_8;
					break;
				case 9:
					startAngle = SelectedAngle.SELECTION_10_9;
					break;
				case 10:
					startAngle = SelectedAngle.SELECTION_10_10;
					break;
				}
				break;
			}
			
			canvas.drawArc(circleArc, startAngle, sweepAngle, false, paint);
			selectedRect.addArc(insideArc, startAngle, sweepAngle);
			selectedRect.addArc(outsideArc, startAngle, sweepAngle);
			paint.setStrokeWidth(3);
			paint.setAlpha(255);
			canvas.drawPath(selectedRect, paint);
		}
	    
		private void drawSuggestions(Canvas canvas) {
			final float RATE = (float)3;
			final float CENTER_X = mScreenWidth /2 - TEXTSIZE/2;
			final float CENTER_Y = mScreenHeight/2 + TEXTSIZE/2 - 3;

			final float Y41 = -18 * RATE;
			final float Y42 = -6 * RATE;
			final float Y43 = Y42 * -1;
			final float Y44 = Y41 * -1;
			final float Y51 = -20 * RATE;
			final float Y52 = -11 * RATE;
			final float Y53 = 0;
			final float Y54 = Y52 * -1;
			final float Y55 = Y51 * -1;
			
			final float X4L1 = -18 * RATE;
			final float X4L2 = -24 * RATE;
			final float X4R1 = X4L1 * -1;
			final float X4R2 = X4L2 * -1;
			final float X5L1 = -16 * RATE;
			final float X5L2 = -23 * RATE;
			final float X5L3 = -25 * RATE;
			final float X5R1 = X5L1 * -1;
			final float X5R2 = X5L2 * -1;
			final float X5R3 = X5L3 * -1;
	        
			paint.setColor(Color.WHITE);
			paint.setAntiAlias(true);
			paint.setStrokeWidth(0);
			paint.setTextSize(TEXTSIZE);
			
			switch(mSuggestions.size()) {
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
	}

	/**
	 * 設定角度以及方向
	 * @param angle:目前的角度
	 * @param direction:true = 左, false = 右
	 */
	public void setAngle(double angle, boolean direction) {
		if (DEBUG) Log.d(TAG, "setAngle");
		mDirection = direction;
		if (direction) {
			mAngle = angle;
		} else {
			if (mAngle >= 0)
				mAngle = angle + 180;
			else
				mAngle = angle - 180;
		}
		mMagnifier.invalidate();
	}
	
	/**
	 * 設定所選取字的索引值
	 * @param index:Index in suggestions.
	 */
	public void setSelectedIndex(int index) {
		mSelectedIndex = index;
		mMagnifier.invalidate();
	}
	
	/**
	 * 將目前按鈕所擁有的字傳進來
	 * @param suggestions
	 */
	public void setSuggestions(List<String> suggestions) {
		if (suggestions != null) {
			mSuggestions = new ArrayList<String>(suggestions);
		}
		mMagnifier.invalidate();
	}
	
	public void setEnterMode(boolean mode) {
		ENTER_MODE = mode;
	}
	
	/**
	 * 顯示放大鏡
	 * @param v:要顯示放大鏡的 view
	 */
	public void showMagnifier(View v) {
		mPopupWindow.showAtLocation(v, Gravity.CENTER, 0, -150);
	}
	
	/**
	 * 關閉放大鏡
	 */
	public void dismissMagnifier(){
		mPopupWindow.dismiss();
		ENTER_MODE = false;
	}
	
	private void updateView(){
	}
}
