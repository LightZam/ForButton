package com.forbutton;

import java.util.ArrayList;
import java.util.List;

import android.R.integer;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class ForbuttonDictionary extends Dictionary {
	private static final String TAG = "ForbuttonDictionary";
	private static final boolean DEBUG = true;
	private int MAXWORDS;
	private List<String> mWordCode;					//存放每次從資料庫取出的table表中，code欄位中的code
	private static List<int[]> countOfPhonetic = new ArrayList<int[]>();//存放每個字或組合字是用幾個注音組合出來的;
	private String continuousCode = "";				//存放轉碼過並加上_或%的SQL語法的CODE
	private String remainingWord = "";
	private String currentValidCode = "";			//存放第一個可以在資料庫找到的CODE
	private String learningCode = "";				//存放學習中組合字的CODE
	private String learningWord = "";				//存放學習中組合字的字
	private String originalCode = "";				//存放使用者原先所打的CODE，未做任何修正版本
	private String originalcontinuousCode = "";
	public Boolean isSmartSearching = false;
	public Boolean isSmartLearning = false;
	public Boolean isPicked = false;
	private Context mContext;						//存放ForButton輸入法SERVICE
	public boolean spacePressed = false;

	public ForbuttonDictionary(Context context) {
		mContext = context;
		MAXWORDS = CandidateView.getMaxSuggest();
	}

	/**
	 * 從db裡面取得對應的字
	 * @author Zam
	 * @Last_Edit_Time 2011/8/19
	 * */
	@Override
	public void getWords(WordComposer composer, WordCallback callback) {
		String code = "";
		// composer.getTypedWord().length() 取得使用者目前打的字數(未送出)
		// 將打的code 串在一起 每個code 都為五位數 ex 12345 符號為3位數
		for (int i = 0; i < composer.getTypedWord().length(); i++) {
			code += String.valueOf(composer.getCodesAt(i)[0]);
		}
		// 取得建議的字出來
		// 在候選列會出現的那些字
		//String[] result = this.loadWordDB(code);
		String[] result = this.loadWordFromDB(code);
		// 將字放入callback
		for (String s : result) {
			// 轉成char array
			char[] word = s.toCharArray();
			// 放入
			// addWord(char[] word, int wordOffset, int wordLength, int
			// frequency)
			// word 字元陣列裡面放字
			// wordOffset 在陣列裡面字元的偏移量
			// wordLength 在陣列裡面有效的字元的長度
			// frequency 發生的頻率，通常為1~255但是可以超過這些限制
			callback.addWord(word, 0, word.length, 10);

			// Log.d("Test", "callback addWord["+ i + "]" + word[i] );
		}
	}

	/**
	 * 從db裡面取得關聯的字
	 * @author Zam
	 * @Last_Edit_Time 2011/8/25
	 * */
	public void getWordAssociation(WordComposer composer,
			WordCallback callback, String selectedWord, int index) {
		String code = "";
		// composer.getTypedWord().length() 取得使用者目前打的字數(未送出)
		// 將打的code 串在一起 每個code 都為五位數 ex 12345 符號為3位數
		if (composer.size() <= 0) {
			if (mWordCode.size() > 0) {
				code = mWordCode.get(index);
			} else
				return;
		} else {
			for (int i = 0; i < composer.getTypedWord().length(); i++) {
				code += String.valueOf(composer.getCodesAt(i)[0]);
			}
		}
		
		// 取得建議的字出來
		// 在候選列會出現的那些字
		String[] result = this.loadWordAssociation(code, selectedWord, index);

		// 將字放入callback
		for (String s : result) {
			// 轉成char array
			char[] word = s.toCharArray();
			// 放入
			// addWord(char[] word, int wordOffset, int wordLength, int
			// frequency)
			// word 字元陣列裡面放字
			// wordOffset 在陣列裡面字元的偏移量
			// wordLength 在陣列裡面有效的字元的長度
			// frequency 發生的頻率，通常為1~255但是可以超過這些限制
			callback.addWord(word, 0, word.length, 10);
		}
	}

	@Override
	public boolean isValidWord(CharSequence word) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 將使用的字增加使用次數，以便優先選字
	 * @author Zam
	 * @Last_Edit_Time 2011/9/23
	 * */
	public void useWordDB(String word) {
		ForbuttonDictionaryProvider zdb = new ForbuttonDictionaryProvider(
				mContext);
		zdb.open();
		// 使用字裡的db use ++
		zdb.useWords(word);
		zdb.close();
	}
	
//	/**
//	 * 將使用的字增加使用次數，以便優先選字
//	 * @author Zam
//	 * @Last_Edit_Time 2011/9/23
//	 * */
//	public void useAssociationDB(String word) {
//		ForbuttonDictionaryProvider zdb = new ForbuttonDictionaryProvider(
//				mContext);
//		zdb.open();
//		// 使用字裡的db use ++
//		zdb.useAssociation(word);
//		zdb.close();
//	}

	/**
	 * 將關聯字的儲存空間做清空的動作..
	 * @author Zam
	 * @Last_Edit_Time 2011/9/25
	 * */
	public void clearAssciationWord() {
		//reset常用字的關聯字
		//remainingWord是組合使用者按下去的關聯字，然後在db作使用++的動作，可以讓常用字跑到前面
		remainingWord = "";
	}
	
	/** TODO 修正完成  待檢測
	 * 刪除字
	 * @author Zam
	 * @Last_Edit_Time 2011/9/26
	 * */
//	public void delete() {
//		if (continuousCode.length() != 0) {
//			Log.d(TAG, "continuousCode " + continuousCode);
//			int end = continuousCode.length(); 
//			if (continuousCode.substring(end - 1, end).matches("%"))
//				continuousCode = continuousCode.substring(0, end - 1);
//			Log.d(TAG, "continuousCode " + continuousCode);
//			end = continuousCode.length();
//			if (continuousCode.substring(end - 3, end).matches("7")){
//				continuousCode = continuousCode.substring(0, end - 3);
//			} else {
//				continuousCode = continuousCode.substring(0, end - 5);
//			}
//			Log.d(TAG, "continuousCode " + continuousCode);
//			originalcontinuousCode = continuousCode;
//		}
//	}
	
	/**
	 * 計數Code裡面總共有幾個字
	 * @author Zam
	 * @Last_Edit_Time 2011/9/23
	 * */
	private int wordCounter(String code){
		int count = 0;
		String subCode;
		//持續迴圈，當還沒有將code全部取出時
		//直到code的裡面的注音都被取出則結束回圈
		while(code.length() > 0)
		{
			if(code.substring(0, 1).matches("7")){
				subCode = code.substring(0, 3);
				count++;
			} else{
				subCode = code.substring(0, 5);
				count++;
			}
			code = code.replaceFirst(subCode, "");
		}
		return count;
	}
	
	/**
	 * 裁切最後一個字的code出來
	 * @author Zam
	 * @Last_Edit_Time 2011/9/23
	 * */
	private String trimLastCode(String code){
		if (code.length() > 0){
			int end = code.length();  
			if (code.substring(end - 3, end).matches("7")){
				code = code.substring(end - 3, end);
			} else {
				code = code.substring(end - 5, end);
			}
			return code;
		}
		return null;
	}
	
	/**
	 * 裁切第一個字的code出來
	 * @author Zam
	 * @Last_Edit_Time 2011/10/12
	 * */
	private String trimFirstCode(String code){
		if (code.length() > 0){
			if (code.substring(0, 1).matches("7")){
				code = code.substring(0, 3);
			} else {
				code = code.substring(0, 5);
			}
			return code;
		}
		return null;
	}
	
//	/**
//	 * 裁切全部的code出來
//	 * @author Zam
//	 * @Last_Edit_Time 2011/10/12
//	 * */
//	private String[] trimAllValidCode(String code){
//		if (code.length() > 0){
//			String[] firstWord = code.split("%");
//			if (firstWord != null)
//				return firstWord;			
//		}
//		return null;
//	}
//	
//	/**
//	 * 裁切第一組字正確的code出來
//	 * @author Zam
//	 * @Last_Edit_Time 2011/9/23
//	 * */
//	private String trimFirstValidCode(String code){
//		if (code.length() > 0){
//			String[] firstWord = code.split("%");
//			if (firstWord != null)
//				return firstWord[0];			
//		}
//		return null;
//	}
//	
//	/**
//	 * 取得精確字 ex ㄅ ㄆ ㄇ ㄈ ㄉ ㄊ
//	 * @author Zam
//	 * @Last_Edit_Time 2011/9/28
//	 * */
//	private void getWordExactly(Cursor cursor, ForbuttonDictionaryProvider zdb, ArrayList<String> result
//											, int selectionMode, String code, int leftWords){
//		//去資料庫撈資料
//		cursor = zdb.getWordsExactly(leftWords);
//		//如果有資料才會執行
//		if (cursor.moveToFirst()) {
//			Log.d(TAG, "continuousCode 11 " + continuousCode);
//			continuousCode = code;
//			Log.d(TAG, "continuousCode 12 " + continuousCode);
//			selectionMode = 1;
//			while (!cursor.isAfterLast()) {
//				String wordCode = cursor.getString(1);
//				String aword = cursor.getString(0);
//				mWordCode.add(wordCode);
//				result.add(aword);
//				cursor.moveToNext();
//				leftWords--;
//				Log.d(TAG, "leftWords " + leftWords);
//				Log.d(TAG, "aword " + aword);
//				break;
//			}
//		}
//		cursor.close();
//	}
//	
//	/**
//	 * 取得大概字 ex 沒 美 梅 ....
//	 * @author Zam
//	 * @Last_Edit_Time 2011/9/28
//	 * */
//	private void getWordsRough(Cursor cursor, ForbuttonDictionaryProvider zdb, ArrayList<String> result
//											, int selectionMode, String code, int leftWords, int numOfWord){
//		if (leftWords > 0) {
//			if (numOfWord == 1) {
//				cursor = zdb.getFirstWordsRough(leftWords,result.get(0));
//			} else {
//				cursor = zdb.getWordsRough(leftWords);
//			}
//			if (cursor.moveToFirst()) {
//				if (numOfWord >= 2) {
//					selectionMode = 2;
//					//String subCode = code.substring(code.length() - 5,code.length());
//					Log.d(TAG, "continuousCode 21 " + continuousCode);
//					continuousCode = code;
//					//continuousCode += subCode;
//					Log.d(TAG, "continuousCode 22 " + continuousCode);
//				}
//				while (!cursor.isAfterLast()) {
//					String wordCode = cursor.getString(1);
//					String aword = cursor.getString(0);
//					mWordCode.add(wordCode);
//					result.add(aword);
//					cursor.moveToNext();
//					leftWords--;
//					Log.d("2", "leftWords " + leftWords);
//					Log.d("2", "aword " + aword);
//				}
//			}
//			
//		}
//		cursor.close();
//	}
//	
//	/**
//	 * 取得複合字 ex 沒有 沒問題 沒什麼 ...
//	 * @author Zam
//	 * @Last_Edit_Time 2011/9/28
//	 * */
//	private void getPhrases(Cursor cursor, ForbuttonDictionaryProvider zdb, ArrayList<String> result
//										, int selectionMode, String code, int leftWords, int numOfWord){
//		if (leftWords > 0) {
//			if (!isSmartLearning){
//				cursor = zdb.getPhrases(leftWords);
//			}
//			if (!isSmartLearning && cursor.moveToFirst()) {
//				while (!cursor.isAfterLast()) {
//					String wordCode = cursor.getString(1);
//					String aword = cursor.getString(0);
//					mWordCode.add(wordCode);
//					result.add(aword);
//					cursor.moveToNext();
//					leftWords--;
//					Log.d("3", "leftWords " + leftWords);
//					Log.d("3", "aword " + aword);
//				}
//				
//			} else {
//				if (selectionMode == 3) {
//					isSmartWordMode = true;
//					String subCode = "";
//					if (continuousCode != "") {
//						subCode = trimLastCode(code);
//						Log.d(TAG, "continuousCode 31 " + continuousCode);
//						continuousCode += subCode;
//						Log.d(TAG, "continuousCode 32 " + continuousCode);
//					
//					int middle = continuousCode.length() - subCode.length();
//					
//					//裁切0~middle + % + 裁切middle~end  ex : A%B  A是全部組好的字,B是最後一個輸入的字
//					continuousCode = continuousCode.substring(0,middle)
//							+ "%"
//							+ continuousCode.substring(middle);
//					Log.d(TAG, "continuousCode 33 " + continuousCode);
//					originalcontinuousCode = continuousCode;
//					
//					//轉換字碼
//					zdb.transCodeAndSetSearchCode(continuousCode);
//					cursor = zdb.getPhrases(leftWords);
//					}
//					if (!isSmartLearning && cursor.moveToFirst()) {
//						while (!cursor.isAfterLast()) {
//							currentValidCode = continuousCode;
//							String wordCode = cursor.getString(1);
//							String aword = cursor.getString(0);
//							mWordCode.add(wordCode);
//							result.add(aword);
//							cursor.moveToNext();
//							leftWords--;
//							Log.d("3", "leftWords " + leftWords);
//							Log.d("3", "aword " + aword);
//						}
//					} else {
//						if (currentValidCode.contentEquals("")){
////							currentValidCode = trimFirstValidCode(continuousCode);
//							currentValidCode = trimFirstCode(code);
//							zdb.setSearchCode(currentValidCode,true);
////							getWordsRough(cursor, zdb, result, selectionMode, code, leftWords, numOfWord);
//							cursor = zdb.getFirstWordsRough(leftWords,currentValidCode);
//							if (cursor.moveToFirst()) {
//								while (!cursor.isAfterLast()) {
//									String wordCode = cursor.getString(1);
//									String aword = cursor.getString(0);
//									mWordCode.add(wordCode);
//									result.add(aword);
//									cursor.moveToNext();
//									leftWords--;
//								}
//							}
//						} else {
//							if (cursor != null)
//								cursor.close();
//							Log.d(TAG, "isSmartLearning " + isSmartLearning);
//							Log.d(TAG, "currentValidCode  " + currentValidCode);
//							zdb.transCodeAndSetSearchCode(currentValidCode);
//							cursor = zdb.getPhrases(leftWords);
//							if (cursor.moveToFirst()) {
//								while (!cursor.isAfterLast()) {
//									String wordCode = cursor.getString(1);
//									String aword = cursor.getString(0);
//									mWordCode.add(wordCode);
//									result.add(aword);
//									cursor.moveToNext();
//									leftWords--;
//									Log.d("3", "leftWords " + leftWords);
//									Log.d("3", "aword " + aword);
//								}
//							}	
//						}
//						isSmartLearning = true;
//					}	
//				}
//			}
//			if (cursor != null)
//				cursor.close();
//		}
//	}
//	
//	/**
//	 * 從db載入字庫，一般選字與智慧選字
//	 * @author Zam
//	 * @Last_Edit_Time 2011/9/25
//	 * */
//	public String[] loadWordDB(String code) {
//		// 在Preference上規定所要抓取的字數限制
//		//目前先用hardcode暫定
//		Integer leftWords = 50;
//		Cursor cursor = null;
//		ForbuttonDictionaryProvider zdb = new ForbuttonDictionaryProvider(mContext);
//		zdb.open();
//		mWordCode = new ArrayList<String>();
//		ArrayList<String> result = new ArrayList<String>();
//		
//		//智慧選字初始化
//		int selectionMode = 3;
//		isSmartWordMode = false;
//		originalCode = code;
//		//從code找出裡面有幾個注音或注音符號
//		int numOfWord = wordCounter(code);
//		//設定要收尋的code
//		zdb.setSearchCode(code,true);
//		if (!isSmartLearning){
//			//只有第一個注音符號會進入
//			if (numOfWord == 1) {
//				getWordExactly(cursor, zdb, result, selectionMode, code, leftWords);
//			}
//			
//			// 取得大概字 ex 沒 美 梅 ....
//			getWordsRough(cursor, zdb, result, selectionMode, code, leftWords, numOfWord);
//		}
//		// 取得複合字 ex 沒有 沒問題 沒什麼 ...
//		getPhrases(cursor, zdb, result, selectionMode, code, leftWords, numOfWord);
//		zdb.close();
//			
//		return result.toArray(new String[0]);
//	}
	
	/**
	 * 將使用者所按到的association的字，在db上use++，讓使用者常用字可以排在前面
	 * @author Zam
	 * @Last_Edit_Time 2011/9/26
	 * */
	private void updateAssociation(String selectedWord){
		if (remainingWord.contentEquals("")){
			remainingWord = selectedWord;
		} else{
			remainingWord += selectedWord;
			useWordDB(remainingWord);
			remainingWord = remainingWord.substring(1);
		}
	}
	
	/**
	 * 分析後，從db中拿出與word相關聯的字
	 * @author Zam
	 * @param code 已轉碼過code
	 * @param selectedWord 被選的字
	 * @param index 被選的字在suggestion裡面的位置
	 * @Last_Edit_Time 2011/9/25
	 * */
	public String[] loadWordAssociation(String code, String selectedWord,
			int index) {
		
		updateAssociation(selectedWord);
		// 在Preference上規定所要抓取的字數限制
		//目前先用hardcode暫定
		Integer leftWords = 50;
		Cursor cursor;
		ForbuttonDictionaryProvider zdb = new ForbuttonDictionaryProvider(
				mContext);
		zdb.open();
		ArrayList<String> result = new ArrayList<String>();
		code = mWordCode.get(index);
		//zdb.setSlectedCode(code);
		zdb.setSearchCode(code,false);
		mWordCode.clear();
		
		// 取得複合字 ex 沒有 沒問題 沒什麼 ...
		// 此種複合字才會進入以下迴圈
		if (leftWords > 0) {
			cursor = zdb.getAssociationWord(leftWords, selectedWord);
			if (cursor.moveToFirst()) {
				while (!cursor.isAfterLast()) {
					// 取出複合字的code
					String phrasesCode = cursor.getString(1);
					
					if (!phrasesCode.contentEquals(phrasesCode.replaceFirst(code, ""))
							&& !phrasesCode.replaceFirst(code, "").contentEquals("")){
						phrasesCode = phrasesCode.replaceFirst(code, "");
						mWordCode.add(phrasesCode);
						// 取出複合字
						String aword = cursor.getString(0);
						// 把第一個字
						aword = aword.replaceFirst(selectedWord, "");
						result.add(aword);
						cursor.moveToNext();
						leftWords--;
					} else {
						cursor.moveToNext();
					}
				}
			}
			cursor.close();
		}
		zdb.close();
		return result.toArray(new String[0]);
	}
	
	public void addNewPhraseToDB(){
		if (!isSmartLearning){
			ForbuttonDictionaryProvider zdb = new ForbuttonDictionaryProvider(
					mContext);
			zdb.open();
			if (DEBUG){
				Log.d(TAG, "addNewPhraseToDB: learningWord " + learningWord);
			}
			zdb.insertPhraseToSQL(learningWord, learningCode);
			zdb.close();
		}
	}

	public void learning(int index, String selectedWord) {
			learningCode += mWordCode.get(index);
			learningWord += selectedWord;
	}
	
	/***********************
	 ***** 開發完待測碼******
	 ***********************/
	private void getPhonetic(Cursor cursor, ForbuttonDictionaryProvider zdb,
			ArrayList<String> result, String code) {
		cursor = zdb.getWordsExactly(1, code);
		if (cursor.moveToFirst()){
			mWordCode.add(cursor.getString(1));
			result.add(cursor.getString(0));
		}
		cursor.close();
	}
	
	private void getFirstRoughWord(Cursor cursor, ForbuttonDictionaryProvider zdb, 
			ArrayList<String> result) {
		cursor = zdb.getFirstWordsRough(MAXWORDS, result.get(0));
		if (cursor.moveToFirst()){
			while (!cursor.isAfterLast()){
				mWordCode.add(cursor.getString(1));
				result.add(cursor.getString(0));
				cursor.moveToNext();
			}
		}
		cursor.close();
	}
	
	private void getSingleWord(Cursor cursor, ForbuttonDictionaryProvider zdb,
			ArrayList<String> result, String code) {
		if (spacePressed) {
			cursor = zdb.getWordsExactly(MAXWORDS, code);
			spacePressed = false;
			if (DEBUG) Log.d(TAG, "spacePressed " + spacePressed);
			if (DEBUG) Log.d(TAG, "code " + code);
		} else {
			cursor = zdb.getWordsRough(MAXWORDS, code);
		}
		if (cursor.moveToFirst()){
			isSmartLearning = false;
			while (!cursor.isAfterLast()){
				mWordCode.add(cursor.getString(1));
				result.add(cursor.getString(0));
				cursor.moveToNext();
			}
		}
		cursor.close();
	}
	
	private void getPhrases(Cursor cursor, ForbuttonDictionaryProvider zdb,
			ArrayList<String> result, String code) {
		if (result.size() < MAXWORDS){
			cursor = zdb.getPhrases(MAXWORDS - result.size(), code);
			if (cursor.moveToFirst()){
				isSmartLearning = false;
				while(!cursor.isAfterLast()){
					mWordCode.add(cursor.getString(1));
					result.add(cursor.getString(0));
					cursor.moveToNext();
				}
			}				
			cursor.close();
		}
	}
	
	private void getSmartPhrases(Cursor cursor,
			ForbuttonDictionaryProvider zdb, ArrayList<String> result,
			String code) {
		int[] countPhonetic = new int[1];
		cursor = zdb.getSmartPhrase(MAXWORDS, trimLastCode(code), wordCounter(code), countPhonetic);
		if (cursor.moveToFirst()){
			while (!cursor.isAfterLast()){
				mWordCode.add(cursor.getString(1));
				Log.d(TAG, cursor.getString(1) + cursor.getString(0));
				result.add(cursor.getString(0));
				cursor.moveToNext();
				countOfPhonetic.add(countPhonetic);
			}
		}
		cursor.close();
	}
	
	private void getSmartRoughPhrases(Cursor cursor,
			ForbuttonDictionaryProvider zdb, ArrayList<String> result) {
		int[] countPhonetic = new int[1];
		cursor = zdb.getSmartRoughPhrase(MAXWORDS, result, countPhonetic);
		if (cursor.moveToFirst()){
			while (!cursor.isAfterLast()){
				mWordCode.add(cursor.getString(1));
				result.add(cursor.getString(0));
				cursor.moveToNext();
				countOfPhonetic.add(countPhonetic);
			}
		}
		cursor.close();
	}	

	/***********************
	 ***** 開發中程式碼******
	 ***********************/
	
	/** TODO 修正完成  待檢測
	 * 刪除字
	 * @author Zam
	 * @Last_Edit_Time 2011/9/26
	 * */
	public void delete(int length) {
		ForbuttonDictionaryProvider zdb = new ForbuttonDictionaryProvider(mContext);
		if (isSmartLearning || isSmartSearching){
			if (length <= 2){
				zdb.clear();
				isSmartLearning = false;
			}else {
				zdb.delete(length);
			}	
		}
		
	}
	
	/**
	 * 將智慧選字的儲存空間做清空的動作
	 * @author Zam
	 * @Last_Edit_Time 2011/9/23
	 * */
	public void clear() {
		isSmartLearning = false;
		isSmartSearching = false;
		countOfPhonetic = new ArrayList<int[]>();//存放每個字或組合字是用幾個注音組合出來的;
		continuousCode = "";				//存放轉碼過並加上_或%的SQL語法的CODE
		remainingWord = "";
		learningCode = "";				//存放學習中組合字的CODE
		learningWord = "";				//存放學習中組合字的字
		isPicked = false;
	}
	
	public String[] loadSmartWordFromDB(String code){
		
		return null;
	}
	
	public String[] loadWordFromDB(String code){
		Cursor cursor = null;
		mWordCode = new ArrayList<String>();
		ArrayList<String> result = new ArrayList<String>();
		ForbuttonDictionaryProvider zdb = new ForbuttonDictionaryProvider(mContext);
		zdb.open();
		countOfPhonetic.clear();
		int numOfWord = wordCounter(code);			//從code找出裡面有幾個注音或注音符號
		if (isSmartLearning && isPicked){
			zdb.setSearchCodeInSmartLearning(code);			//設定要收尋的code
		}
		//第一次輸入才須執行
		if (numOfWord == 1){
			zdb.setSearchCode(code,true);			//設定要收尋的code
			getPhonetic(cursor, zdb, result, code);   	//取注音
			getFirstRoughWord(cursor, zdb, result);	//取字
		} else {
			getSingleWord(cursor, zdb, result, code);		//取字
			if (result.size() != 0){
				getPhrases(cursor, zdb, result, code);	//取字後面的組合字
			}
			//要找出輸入智慧字的結果，必須要上面兩個取字都沒找到在進去找
			if (result.size() == 0){
				if (!isSmartSearching){
					isSmartSearching = true;
					zdb.setSmartSearchCode();
				}
//				if (!isSmartLearning){
					getSmartPhrases(cursor, zdb, result, code);	//取出輸入智慧字的結果   ex: 輸入ㄐㄅㄙ  > 出現賈伯斯選項
					//沒有找到輸入智慧字的結果，代表資料庫沒有這個字，進入學習模式
					if (result.size() == 0){
						getSmartRoughPhrases(cursor, zdb, result);
//							isSmartSearching = false;
						isSmartLearning = true;
					}
					if (result.size() < MAXWORDS){
						isSmartLearning = true;
					}
					smartLearning(cursor, zdb, result);
//				} 
//				else {
//					smartLearning(cursor, zdb, result);
//				}
			} else {
				isSmartSearching = false;
				isSmartLearning = false;
			}
		}
		zdb.close();
		return result.toArray(new String[0]);
		
	}

	private void getCloseWords(Cursor cursor,
			ForbuttonDictionaryProvider zdb, ArrayList<String> result) {
		int[] countPhonetic = new int[1];
		cursor = zdb.getCloseWords(MAXWORDS - result.size(), countPhonetic);
		if (cursor != null && cursor.moveToFirst()){
			while(!cursor.isAfterLast()){
				mWordCode.add(cursor.getString(1));
				result.add(cursor.getString(0));
				cursor.moveToNext();
				countOfPhonetic.add(countPhonetic);
			}
			cursor.close();
		}	
		
	}
	
	private void getClosePhrases(Cursor cursor,
			ForbuttonDictionaryProvider zdb, ArrayList<String> result) {
		int[] countPhonetic = new int[1];
		cursor = zdb.getClosePhrases(MAXWORDS - result.size(), countPhonetic, result);
		if (cursor != null && cursor.moveToFirst()){
			while(!cursor.isAfterLast()){
				mWordCode.add(cursor.getString(1));
				result.add(cursor.getString(0));
				cursor.moveToNext();
				countOfPhonetic.add(countPhonetic);
			}
			cursor.close();
		}	
		
	}
	
	private void smartLearning(Cursor cursor, ForbuttonDictionaryProvider zdb,
			ArrayList<String> result) {
		getClosePhrases(cursor, zdb, result);
		getCloseWords(cursor, zdb, result);	
		if (!isSmartLearning){
			getSmartRoughPhrases(cursor, zdb, result);
		}
		if (DEBUG) {
			for (int i = 0; i < countOfPhonetic.size() ;i++)
				Log.d(TAG, "countOfPhonetic[" + i + "] " + countOfPhonetic.get(i)[0]);
		}
	}
	
	public int getNumOfPhoentic(int index){
		if (countOfPhonetic.size() > 0) {
			return countOfPhonetic.get(index)[0];
		} else {
			return 0;
		}
		
	}
}
