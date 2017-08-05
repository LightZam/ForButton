package com.forbutton;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class KeyboardControl {
	public final static String TAG = "KEYBOARDCONTROL"; 
	
	/* Button Id */
	//1 2 3 4 四顆按鈕   左上順時針到左下
	public final static int ID_LEFTUP = 1;
	public final static int ID_RIGHTUP = 2;
	public final static int ID_LEFTDOWN = 3;
	public final static int ID_RIGHTDOWN = 4;
	public final static int ID_CENTER = 5;

	public final static int ENGLISH_BIG_MODE = 0;
	public final static int ENGLISH_MODE = 1;
	public final static int TAIWAN_MODE = 2;
	public final static int NUMBER_MODE = 3;
	public final static int SYMBOL_NARROW_MODE = 4;
	public final static int SYMBOL_WIDE_MODE = 5;
	private int currentMode;
	
	public KeyboardControl() {
		currentMode = TAIWAN_MODE;
	}
	
	/**
	 * 設定目前為哪一種鍵盤
	 * @author Zam
	 * @param mode
	 * @return 
	 */
	public void setMode(int mode) {
		switch(mode) {
		case ENGLISH_BIG_MODE:
			currentMode = ENGLISH_BIG_MODE;
			break;
			
		case ENGLISH_MODE:
			currentMode = ENGLISH_MODE;
			break;
			
		case TAIWAN_MODE:
			currentMode = TAIWAN_MODE;
			break;
			
		case NUMBER_MODE:
			currentMode = NUMBER_MODE;
			break;
			
		case SYMBOL_NARROW_MODE:
			currentMode = SYMBOL_NARROW_MODE;
			break;
			
		case SYMBOL_WIDE_MODE:
			currentMode = SYMBOL_WIDE_MODE;
			break;
		}
	}
	
	public int getMode() {
		return currentMode;
	}
	
	/**
	 * 回傳與該按鍵相對應的候選字
	 * @author Amobe
	 * @param bid
	 * @return ArrayList<String>
	 */
	public List<String> getCandidate(int bid) {
		switch(currentMode) {
		case ENGLISH_BIG_MODE:
			return ascii2list(getEnglishBigQWERTKeyboard());
		case ENGLISH_MODE:
			return ascii2list(getEnglishQWERTKeyboard());
		case TAIWAN_MODE:
			return getTaiwanContext(bid);
		case NUMBER_MODE:
			return ascii2list(getNumberKeyboard());
		case SYMBOL_NARROW_MODE:
			return ascii2list(getSymbolNarrowKeyboard());
		case SYMBOL_WIDE_MODE:
			return narrow2wide(getSymbolNarrowKeyboard());
		}
		return null;
	}
	
	/**
	 * 回傳與該按鍵相對應的英文字
	 * @author Amobe
	 * @param bid
	 * @return ArrayList<String>
	 * @last_edit_time 2011-8-2 03:59
	 */
	public List<String> getEnglishContext(int bid) {
		String s = null;
		
		switch(bid) {
		case ID_LEFTUP:
			
		case ID_RIGHTUP:			
		case ID_LEFTDOWN:
		case ID_RIGHTDOWN:
			//取得鍵盤
			s = getEnglishQWERTKeyboard();
			return ascii2list(s);
		}
		return null;
	}
	
	public List<String> getNumberContext(int bid) {
		String s = null;
		
		switch(bid) {
		case ID_LEFTUP:
			// 0~9
			s = "48 49 50 51 52 53 54 55 56 57";
			return ascii2list(s);
		case ID_RIGHTUP:
		case ID_LEFTDOWN:
		case ID_RIGHTDOWN:
			break;
		}
		return null;
	}
	
	/**
	 * 回傳與該按鍵相對應的中文字
	 * @author Amobe
	 * @param bid
	 * @return ArrayList<String>
	 * @last_edit_time 2011/09/19 13:45
	 */
	public List<String> getTaiwanContext(int bid) {
		String s = null;
		switch(bid) { 
		case ID_CENTER:
			// , . _ ! ( ) < >
			s = "44 46 32 33 729 714 711 715 ";
			return string2array(s);
		case ID_LEFTUP:
			// ㄅ ㄆ ㄇ ㄈ ㄉ ㄊ ㄋ ㄌ ㄍ ㄎ
			s = "12549 12550 12551 12552 12553 12554 12555 12556 12557 12558 ";
			return string2array(s);
		case ID_RIGHTUP:
			// ㄙ ㄧ ㄨ ㄩ ㄚ ㄛ ㄜ ㄝ ㄞ
			s = "12569 12583 12584 12585 12570 12571 12572 12573 12574 ";
			return string2array(s);
		case ID_LEFTDOWN:
			// ㄏ ㄐ ㄑ ㄒ ㄓ ㄔ ㄕ ㄖ ㄗ ㄘ
			s = "12559 12560 12561 12562 12563 12564 12565 12566 12567 12568 ";
			return string2array(s);
		case ID_RIGHTDOWN:
			// ㄟ ㄠ ㄡ ㄢ ㄣ ㄤ ㄥ ㄦ 
			s = "12575 12576 12577 12578 12579 12580 12581 12582 ";
			return string2array(s);
		}
		return null;
	}
	
	public String getNumberKeyboard() {
		String s = null;
		// 7 8 9 4 5 6 1 2 3 * 0 #
		s = "55 56 57 52 53 54 49 50 51 42 48 35 ";
		return s;
	}
	
	/**
	 * 回傳a~z測試用鍵盤
	 * @author LexLu
	 * @return String
	 * @last_edit_time 2011-8-2 03:17
	 */
	public String getEnglishNormalKeyboard() {
		String s = null;
		// a b c d e f g 
		s = "97 98 99 100 101 102 103 ";
		// h i j k l m n
		s += "104 105 106 107 108 109 110 ";
		// o p q r s t 
		s += "111 112 113 114 115 116 ";
		// u v w x y z
		s += "117 118 119 120 121 122";
		return s;
	}
	
	/**
	 * 回傳qwert用鍵盤
	 * @author LexLu
	 * @return String
	 * @last_edit_time 2011-8-2 03:28
	 */
	public String getEnglishQWERTKeyboard() {
		String s = null;
		// q w e r t y u i o
		s = "113 119 101 114 116 121 117 105 111 ";
		// a s d f g h j k p
		s += "97 115 100 102 103 104 106 107 112 ";
		// z x c v b n m l
		s += "122 120 99 118 98 110 109 108 ";
		// , 1 2 3 space ? ! .
		s += "44 48 49 50 32 63 33 46 ";
		// 4 5 6 7 8 9 0
		s += "51 52 53 54 55 56 57 ";
		return s;
	}
	
	/**
	 * 回傳QWERT用鍵盤
	 * @author LexLu
	 * @return String
	 */
	public String getEnglishBigQWERTKeyboard() {
		String s = null;
		// Q W E R T Y U I O
		s = "81 87 69 82 84 89 85 73 79 ";
		// A S D F G H J K P
		s += "65 83 68 70 71 72 74 75 80 ";
		// Z X C V B N M L
		s += "90 88 67 86 66 78 77 76 ";
		// , 1 2 3 space ? ! .
		s += "44 48 49 50 32 63 33 46 ";
		// 4 5 6 7 8 9 0
		s += "51 52 53 54 55 56 57 ";
		return s;
	}

	public String getSymbolNarrowKeyboard() {
		String s = null;
		// ~ @ # $ % ^ & ( )
		s = "126 64 35 36 37 94 38 40 41 ";
		// + - * / = \ | [ ]
		s += "43 45 42 47 61 92 124 91 93 ";
		// ` ' " , . _ ? { }
		s += "96 39 34 44 46 95 63 123 125 ";//63
		// : ; space ! < >
		s += "58 59 32 33 60 62 ";
		return s;
	}

	public String getSymbolWideKeyboard() {
		String s = null;
		// ! @ # $ % ^ & * ( )
		s = "33 64 35 36 37 94 38 42 40 41 ";
		// ~ ` - = \ _ + | { }
		s += "126 96 45 61 92 95 43 124 123 125 ";
		// " ' , . / : ; ? < >
		s += "34 39 44 46 47 58 59 63 60 62 ";//63
		// space [ ]
		s += "32 91 93 ";
		return s;
	}
	
	/**
	 * 回傳注音測試用鍵盤
	 * @last_edit_time 2011-8-17 
	 */
	public String getForbuttonKeyboard() {
		String s = null;
		// ㄅ ㄆ ㄇ ㄈ ㄉ ㄊ ㄋ ㄌ ㄍ ㄎ
		s = "12549 12570 12550 12551 12552 12553 12554 12555 12556 12557 12558 ";
		// ㄏ ㄐ ㄑㄒ ㄓ ㄔ ㄕ ㄖ ㄗ ㄘ
		s += "12559 12560 12561 12562 12563 12564 12565 12566 12567 12568 ";
		// ㄙ ㄚ ㄛ ㄜ ㄝ ㄞ ㄟ ㄠ ㄡ ㄢ
		s += "12569 12570 12571 12572 12573 12574 12575 12576 12577 12578 ";
		// ㄣ ㄤ ㄥ ㄦ ㄧ ㄨ ㄩ
		s += "12579 12580 12581 12582 12583 12584 12585 ";
		return s;
	}
	
	/**
	 * 將傳入的字串以空白為分割轉為字串陣列
	 * @author Amobe
	 * @param  String s
	 * @return String[]
	 */
	public List<String> string2array(String s) {
		List<String> list = new ArrayList<String>();
		String[] chars = null;
		chars = s.split(" ");
		for (int i = 0; i < chars.length; i++)
			list.add(chars[i]);
		return list;
	}
	
	/**
	 * 將傳入的ASCII字串轉為字串陣列
	 * @author Amobe
	 * @param  String s
	 * @return ArrayList<String>
	 */
	public List<String> ascii2list(String s) {
		List<String> list = new ArrayList<String>();
		String[] chars = null;
		chars = s.split(" ");
		for (int i = 0; i < chars.length; i++) {
			char c = (char)Integer.parseInt(chars[i]);
			list.add(Character.toString(c));
		}
		return list;
	}
	
	/**
	 * 將半形的字串轉為全形後傳出list
	 * @param s
	 * @return
	 */
	public List<String> narrow2wide(String s) {
		List<String> list = new ArrayList<String>();
		String[] chars = null;
		chars = s.split(" ");
		for (int i = 0; i < chars.length; i++) {
			char c = (char)(Integer.parseInt(chars[i])+65248);
			if (Integer.parseInt(chars[i]) == 32){
				c = (char)(Integer.parseInt(chars[i])+12256);
			}
			list.add(Character.toString(c));
		}
		return list;
	}
}
