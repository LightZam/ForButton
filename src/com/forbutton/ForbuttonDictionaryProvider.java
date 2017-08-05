package com.forbutton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import android.R.integer;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class ForbuttonDictionaryProvider extends ContentProvider {
	
	private static final String TAG = "ForbuttonDictionaryProvider";
	private static final String DBWORDS_NAME = "ForButtonWords.db";
	private static final String DBPHRASES_NAME = "ForButtonPhrases.db";
	private static final int DATABASE_VERSION = 2011081104;
	private static final Integer INPUT_DB_FILES = 10; // According to Forbutton.dbx in assets
	private static final boolean DEBUG = true;
	private static int wordCount = 0;
	private static HashMap<String, String> codeMap;
	private static String mSearchCode = "";
	private static List<String> mSmartSearchCode;
	private static List<String> mCloseSmartCode = new ArrayList<String>();
	private static boolean isGetPhrase = false;
	private Context context;
	private SQLiteDatabase dbWords;
	private SQLiteDatabase dbPhrases;

	/**,.
	 * This class helps open, create, and upgrade the database file.
	 */
	private class DatabaseHelper extends SQLiteOpenHelper {

		private String DB_PATH = "/data/data/com.forbutton/databases/";
		private final Context myContext;
		private String dbName;

		DatabaseHelper(Context context, String DBName) {
			super(context, DBName, null, DATABASE_VERSION);
			this.dbName = DBName;
			this.myContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db){

			// Nothing to do here, since we already have data!!
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (DEBUG) Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			try {
				copyDataBase();
			} catch (IOException e) {
				throw new Error("Error copying database");
			}
		}

		/**
		 * Creates a empty database on the system and rewrites it with your own
		 * database.
		 * */
		public void createDataBase() throws IOException {

			boolean dbExist = checkDataBase();

			if (dbExist) {
				// do nothing - database already exist
			} else {
				// By calling this method and empty database will be created
				// into the default system path
				// of your application so we are gonna be able to overwrite that
				// database with our database.
				this.getReadableDatabase();
				this.close();

				try {
					copyDataBase();

				} catch (IOException e) {

					throw new Error("Error copying database");

				}
			}

		}

		/**
		 * Check if the database already exist to avoid re-copying the file each
		 * time you open the application.
		 * 
		 * @return true if it exists, false if it doesn't
		 */
		private boolean checkDataBase() {

			SQLiteDatabase checkDB = null;

			try {
				String myPath = DB_PATH + this.dbName;
				Boolean exists = (new File(myPath)).exists();
				if(exists) {
					checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
				}

			} catch (SQLiteException e) {
				//Log.i(TAG, "database does't exist yet");
			}

			if (checkDB != null) {
				checkDB.close();
			}

			return checkDB != null ? true : false;
		}
		
		/**
		 * Copies your database from your local assets-folder to the just
		 * created empty database in the system folder, from where it can be
		 * accessed and handled. This is done by transfering bytestream.
		 * */
		private void copyDataBase() throws IOException {

			//Log.i(TAG, "start to copy database");

			InputStream myInput;
			String outFileName;
			OutputStream myOutput;
			Integer fIdx;
			
			// Path to the just created empty db
			outFileName = DB_PATH + this.dbName;
			// Open the empty db as the output stream
			myOutput = new FileOutputStream(outFileName);

			for(fIdx = 0; fIdx < INPUT_DB_FILES; fIdx++) {

				// Open your local db as the input stream
				try {
					myInput = myContext.getAssets().open(this.dbName + fIdx.toString());
				} catch (IOException e)
				{
					// No such file
					System.out.println("Unable to open input file " + this.dbName + fIdx.toString() + "!!");
					break;
				}			

				// transfer bytes from the inputfile to the outputfile
				byte[] buffer = new byte[1024];
				int length;
				while ((length = myInput.read(buffer)) > 0) {
					myOutput.write(buffer, 0, length);
				}

				myInput.close();
			}
				
			//Log.i(TAG, "copy database done");

			// Close the streams
			myOutput.flush();
			myOutput.close();			
		}	
	}

	private void forbuttonDictionaryCodeMap() {

		codeMap = new HashMap<String, String>();
		//準備轉碼所需的對應表
		codeMap.put("12549", "10"); //ㄅ
		codeMap.put("12550", "11"); //ㄆ
		codeMap.put("12551", "12"); //ㄇ
		codeMap.put("12552", "13"); //ㄈ
		codeMap.put("12553", "14"); //ㄉ
		codeMap.put("12554", "15"); //ㄊ
		codeMap.put("12555", "16"); //ㄋ
		codeMap.put("12556", "17"); //ㄌ
		codeMap.put("12557", "18"); //ㄍ
		codeMap.put("12558", "19"); //ㄎ
		codeMap.put("12559", "1A"); //ㄏ
		codeMap.put("12560", "1B"); //ㄐ
		codeMap.put("12561", "1C"); //ㄑ
		codeMap.put("12562", "1D"); //ㄒ
		codeMap.put("12563", "1E"); //ㄓ
		codeMap.put("12564", "1F"); //ㄔ
		codeMap.put("12565", "1G"); //ㄕ
		codeMap.put("12566", "1H"); //ㄖ
		codeMap.put("12567", "1I"); //ㄗ
		codeMap.put("12568", "1J"); //ㄘ
		codeMap.put("12569", "1K"); //ㄙ
		codeMap.put("12570", "20"); //ㄚ
		codeMap.put("12571", "21"); //ㄛ
		codeMap.put("12572", "22"); //ㄜ
		codeMap.put("12573", "23"); //ㄝ
		codeMap.put("12574", "24"); //ㄞ
		codeMap.put("12575", "25"); //ㄟ
		codeMap.put("12576", "26"); //ㄠ
		codeMap.put("12577", "27"); //ㄡ
		codeMap.put("12578", "28"); //ㄢ
		codeMap.put("12579", "29"); //ㄣ
		codeMap.put("12580", "2A"); //ㄤ
		codeMap.put("12581", "2B"); //ㄥ
		codeMap.put("12582", "2C"); //ㄦ
		codeMap.put("12583", "30"); //ㄧ
		codeMap.put("12584", "31"); //ㄨ
		codeMap.put("12585", "32"); //ㄩ
		codeMap.put("729",  "40"); //˙
		codeMap.put("714",  "41"); //ˊ	
		codeMap.put("711",  "42"); //ˇ
		codeMap.put("715",  "43"); //ˋ
	}
	/**
	 * 此為轉碼
	 * 要配合forbuttonDictionaryCodeMap看
	 * @param code
	 * @return
	 */
	private String transDBCode(String code) {
		String subCode;
		String transCode = "";
		//持續迴圈，當還沒有將code全部取出時
		//直到code的裡面的注音都被取出則結束回圈
		while(code.length() > 0)
		{
			//因為在forbuttonDictionaryCodeMap設置時，7的只有三位數，所以裁切三個
			//其他都為五位，取五個
			if(code.substring(0, 1).matches("7"))
				subCode = code.substring(0, 3);
			else
				subCode = code.substring(0, 5);
			//將已取出的code清除
			code = code.replaceFirst(subCode, "");
			//將code轉碼
			transCode += codeMap.get(subCode);
		}
		return transCode;
	}
	
		
	private DatabaseHelper mOpenHelperWords;
	private DatabaseHelper mOpenHelperPhrases;
	/**
	 * 初始
	 * @param ctx
	 */
	public ForbuttonDictionaryProvider(Context ctx) {
		this.context = ctx;
		mOpenHelperWords = new DatabaseHelper(this.context, DBWORDS_NAME);
		mOpenHelperPhrases = new DatabaseHelper(this.context, DBPHRASES_NAME);
		//如果不能成功創db則會有exception
		try {
			mOpenHelperWords.createDataBase();
			mOpenHelperPhrases.createDataBase();
		} catch (IOException ioe) {
			// throw new Error("Unable to create database");
			Toast.makeText(ctx, R.string.db_create_error, Toast.LENGTH_SHORT).show();
		}
		//初始轉換規則
		forbuttonDictionaryCodeMap();
		//負責放三種不同的smartSearchCode
	}

	@Override 
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreate() {
		//Log.i(TAG, "CREATE TABLE");
		mOpenHelperWords = new DatabaseHelper(getContext(), DBWORDS_NAME);
		mOpenHelperPhrases = new DatabaseHelper(getContext(), DBPHRASES_NAME);
		return true;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	public ForbuttonDictionaryProvider open() {
		dbWords = mOpenHelperWords.getWritableDatabase();
		dbPhrases = mOpenHelperPhrases.getWritableDatabase();
		return this;
	}

	public void close() {
		mOpenHelperWords.close();
		mOpenHelperPhrases.close();
	}

	/**
	 * 設定DB要選字所需的CODE
	 * @param code 要設定的code
	 * @param isTransDBcode true轉碼後設定，false不轉碼直接設定 
	 * @author Zam
	 * @Last_Edit_Time 2011/9/28
	 **/
	public void setSearchCode(String code, Boolean isTransDBcode) {
		if (isTransDBcode){
			mSearchCode = transDBCode(code);
		} else {
			mSearchCode = code;
		}
	}
	
//	/**
//	 * 此處是用來轉換智慧選字的code，轉換為DB裡要用的code
//	 * @param code 要轉換的code
//	 * @author Zam
//	 * @Last_Edit_Time 2011/9/23
//	 **/
//	public void transCodeAndSetSearchCode(String code){
//		String buffer = "";
//		//加上%幫助進行判斷
//		String tempCode = code + "%";
//		//Log.d("Test", "code.indexof " + tempCode.indexOf("%"));
//		//有%代表有兩個或以上的字，每個%會區隔左跟右的字    EX : A%B
//		//判斷第一個正確的字 EX: A%B 的 A
//		if (tempCode.indexOf("%") != -1){
//			int index = tempCode.indexOf("%");
//			buffer += transDBCode(tempCode.substring(0, index));
//			tempCode = tempCode.substring(index + 1);
//		}
//		//取出剩下的字
//		//EX: A%B%C%.... 的 B,C,...
//		while (tempCode.indexOf("%") != -1){
//			int index = tempCode.indexOf("%");
//			buffer += "%";
//			Log.d("1", "buffer 1 " + buffer);
//			buffer += transDBCode(tempCode.substring(0, index));
//			Log.d("1", "buffer 2 " + buffer);
//			Log.d("1", "code 1 " + tempCode);
//			tempCode = tempCode.substring(index + 1);
//			Log.d("1", "code 2 " + tempCode);
//		}
//		mSearchCode = buffer;
//	}
	
	/**
	* 將使用字或複合字的使用率增加
	* @author Zam
	* @Last_Edit_Time 2011/9/27
	*/
	public void useWords(String word) {
		if (DEBUG){
			Log.d(TAG, "useWords: word " + word);
			Log.d(TAG, "useWords: mSearchCode" + mSearchCode);
		}
		String code = mSearchCode;
		updateUseInSQL(word,code);
		mCloseSmartCode.clear();
	}	
	
	public void insertPhraseToSQL(String word, String code){
		dbPhrases.execSQL("insert into phrases_" + code.substring(0, 2) + "(code, word, frequency, use) values('" + code + "','" + word + "',1,10)");
	}
	
	/**
	* 執行使用率增加的sql語法
	* @author Zam
	* @Last_Edit_Time 2011/9/27
	*/
	private void updateUseInSQL(String word, String code){
		//使用複合字就  db裡的use欄位就+1  
		if(word.length() > 1)
			dbPhrases.execSQL("update phrases_" + code.substring(0, 2) + " set use=use+1 where word='" + word + "'");
		//使用字就  db裡的use欄位就+1  
		else	
			dbWords.execSQL("update words_" + code.substring(0, 2) + " set use=use+1 where word='" + word + "'");
	}
	
	/**
	*	取得PAHASES裡的資料
	*	複合字   LIKE 大家
	* @author Zam
	* @Last_Edit_Time 2011/9/23
	*/
	public Cursor getPhrases(Integer limit, String newCode) {
		String code = "";
		code = transDBCode(newCode);
		if (DEBUG) Log.d(TAG,"getPhrases code : " + code);
		Cursor mCursor = dbPhrases.query(true, "phrases_" + code.substring(0, 2), new String[] { "word","code" }, "code LIKE '" + code + "%' group by word", null, null, null, "use DESC, frequency DESC", limit.toString());
		if (mCursor.moveToFirst()){
			mSearchCode = code;
		}
		return mCursor;
	}
	
	/**
	 *	取得精確的字   CODE會被轉換成兩位數 EX: 12
	 *	此方法就是取得資料欄位CODE裡的值為12的WORD ㄇ
	 *	精確字 也包括  ㄇㄟˊ 已把注音符號也打出來的字
	 * @author Zam
	 * @param limit
	 * @return
	 * @Last_Edit_Time 2011/9/23
	 */
	public Cursor getWordsExactly(Integer limit, String newCode) {
		String code = transDBCode(newCode);
		Cursor mCursor = dbWords.query(true, "words_" + code.substring(0, 2), new String[] { "word","code" }, "code='" + code + "' group by word", null, null, null, "use DESC, frequency DESC", limit.toString());
		if (mCursor.moveToFirst()){
			mSearchCode = code;
		}
		return mCursor;
	}
	
	/**
	 *	取得不精確的字 ex: 1225 ㄇㄟ    沒
	 *	取得資料欄位code裡值為12XXXXX...的word但是不取 12
	 * @author Zam
	 * @param limit 字數限制
	 * @return
	 * @Last_Edit_Time 2011/9/25
	 */
	public Cursor getWordsRough(Integer limit, String newCode) {
		String code = transDBCode(newCode);
		if (DEBUG) Log.d(TAG,"getWordsRough code : " + code);
		Cursor mCursor = dbWords.query(true, "words_" + code.substring(0, 2), new String[] { "word","code" }, "code like '" + code + "%" + "' group by word", null, null, null, "use DESC, frequency DESC", limit.toString());
		if (mCursor.moveToFirst()){
			mSearchCode = code;
		}
		return mCursor;
	}
	
	/**
	 *	取得不精確的字 ex: 1225 ㄇㄟ    沒
	 *	取得資料欄位code裡值為12XXXXX...的word但是不取 12
	 * @author Zam
	 * @param limit 字數限制, word 收尋中不出現ㄅ,ㄆ,ㄇ,ㄈ,etc..
	 * @return
	 * @Last_Edit_Time 2011/9/25
	 */
	public Cursor getFirstWordsRough(Integer limit,String word) {
		String code = mSearchCode;
		Cursor mCursor = dbWords.query(true, "words_" + code.substring(0, 2), new String[] { "word","code" }, "code like '" + code + "%' and word!='" + word + "' group by word", null, null, null, "use DESC, frequency DESC", limit.toString());
		if (mCursor.moveToFirst()){
			setSmartSearchCode();
		}
		return mCursor;
	}
	
	/**
	 * 取得word之後的關聯字
	 * @author Zam
	 * @param limit
	 * @param word
	 * @return
	 * @Last_Edit_Time 2011/9/23
	 */
	public Cursor getAssociationWord(Integer limit,String word) {
		String code = mSearchCode;
		Cursor mCursor = dbPhrases.query(true, "phrases_" + code.substring(0, 2) , new String[] { "word","code" }, "word like '" + word + "%'" + " group by word", null, null, null, "use DESC, frequency DESC", limit.toString());
		return mCursor;
	}
	
	/********************
	 ****開發中程式碼*****
	 ********************/
	
	public Cursor getSmartPhrase(Integer limit, String code, int count, int[] countOfPhonetic){
		final String TABLE = "phrases_" + mSearchCode.substring(0, 2);
		final String[] COLUMNS = new String[] { "word","code" };
		final String[] UNDERSCORE = new String[] {"","__","____","______"};
		final String ORDERBY = "use DESC, frequency DESC";
		List<String> tempSmartSearchCodeList = new ArrayList<String>();		
		String selection = "";		//存放要收尋資料庫的選擇條件
		String tempCode = "";		//存放暫時編輯的程式碼
		Cursor cursor = null;
		//把所有會找的字的選擇條件存起來
		for (int i = 0; i < mSmartSearchCode.size(); i++){
			for (int j = 0; j < 4; j++){
				tempCode = mSmartSearchCode.get(i) + UNDERSCORE[j] + transDBCode(code);
				selection = "code like '" + tempCode + "%' group by word";
				cursor = dbPhrases.query(true, TABLE, COLUMNS, selection, null, null, null, ORDERBY, limit.toString());
				if (cursor.moveToFirst()){
					tempSmartSearchCodeList.add(tempCode);
					if (cursor.getString(0).length() == 2){
						wordCount = count;
						mCloseSmartCode.add(tempCode);
						isGetPhrase = false;
					} else if (cursor.getString(0).length() >= 3) {
						isGetPhrase = true;
					}
					if (DEBUG){
						Log.d(TAG, "tempCode " + tempCode);
						while(!cursor.isAfterLast()){
							Log.d(TAG, "cursor1 " + cursor.getString(0));
							cursor.moveToNext();
						}
					}
				}					
			}
		}
		if (cursor != null)
			cursor.close();
		if (tempSmartSearchCodeList.size() > 0){
			selection = "";
			mSmartSearchCode = tempSmartSearchCodeList;
			if (tempSmartSearchCodeList.size() == 1){
				selection += "code like '" + tempSmartSearchCodeList.get(0) + "' or ";
				selection += "code like '" + tempSmartSearchCodeList.get(0) + "__' or ";
				selection += "code like '" + tempSmartSearchCodeList.get(0) + "____' or ";
				selection += "code like '" + tempSmartSearchCodeList.get(0) + "______' group by word";
			} else{
				for (int i = 0; i < tempSmartSearchCodeList.size(); i++){
					if (i != tempSmartSearchCodeList.size() - 1){
						selection += "code like '" + tempSmartSearchCodeList.get(i) + "' or ";
						selection += "code like '" + tempSmartSearchCodeList.get(i) + "__'  or "; 
						selection += "code like '" + tempSmartSearchCodeList.get(i) + "____' or ";
						selection += "code like '" + tempSmartSearchCodeList.get(i) + "______' or ";
					} else{
						selection += "code like '" + tempSmartSearchCodeList.get(i) + "' or ";
						selection += "code like '" + tempSmartSearchCodeList.get(i) + "__' or ";
						selection += "code like '" + tempSmartSearchCodeList.get(i) + "____' or ";
						selection += "code like '" + tempSmartSearchCodeList.get(i) + "______' group by word";
					}
				}
			}
			String[] smartCode = tempSmartSearchCodeList.get(0).split("_");
			for (int i = 0; i < smartCode.length; i++) {
				if (!smartCode[i].contentEquals("")){
					countOfPhonetic[0] += smartCode[i].length()/2;
				}
			}
			cursor = dbPhrases.query(true, TABLE, COLUMNS, selection, null, null, null, ORDERBY, limit.toString());
		}
		return cursor;	
	}
	
	public void setSmartSearchCode(){
		mSmartSearchCode = new ArrayList<String>();
		mSmartSearchCode.add(mSearchCode);
	}
	
	public void clear(){
		mCloseSmartCode.clear();
		mSmartSearchCode.clear();
		mSearchCode = "";
		wordCount = 0;
	}
	
	public void delete(int length) {
		for(int i = 0; i < 2; i++){
			if (wordCount <= length) {
				String smartCode = "";
				List<String> tempSmartSearchCodeList = new ArrayList<String>();	
				while(mSmartSearchCode.size() > 0){
					smartCode = mSmartSearchCode.get(0);
					smartCode = smartCode.substring(smartCode.length() - 1, smartCode.length());
					while (smartCode.lastIndexOf("_") == length) {
						smartCode = smartCode.substring(smartCode.length() - 1, smartCode.length());
					}
					tempSmartSearchCodeList.add(smartCode);
					mSmartSearchCode.remove(0);
					if (mCloseSmartCode.size() > 0)
						mCloseSmartCode.remove(0);
				}
				if (wordCount == mSearchCode.length()/2){
					if (DEBUG) Log.d(TAG,"delete mSearchCode " + mSearchCode);
					mSearchCode.substring(mSearchCode.length()-2, mSearchCode.length());
				}
				mSmartSearchCode = tempSmartSearchCodeList;
				mCloseSmartCode = tempSmartSearchCodeList;
			} else {
				String smartCode = "";
				List<String> tempSmartSearchCodeList = new ArrayList<String>();	
				while(mSmartSearchCode.size() > 0){
					smartCode = mSmartSearchCode.get(0);
					smartCode = smartCode.substring(smartCode.length() - 1, smartCode.length());
					while (smartCode.lastIndexOf("_") == length) {
						smartCode = smartCode.substring(smartCode.length() - 1, smartCode.length());
					}
					tempSmartSearchCodeList.add(smartCode);
					mSmartSearchCode.remove(0);
				}
				if (wordCount == mSearchCode.length()/2){
					if (DEBUG) Log.d(TAG,"delete mSearchCode " + mSearchCode);
					mSearchCode.substring(mSearchCode.length()-2, mSearchCode.length());
				}
				mSmartSearchCode = tempSmartSearchCodeList;
			}
			length--;
		}
	}
	
	public Cursor getCloseWords(Integer limit, int[] countOfPhonetic) {
		Cursor cursor = null;
		final String TABLE = "words_" + mSearchCode.substring(0, 2);
		final String[] COLUMNS = new String[] { "word","code" };
		final String ORDERBY = "use DESC, frequency DESC";
		String selection = "";
		selection =  "code like '" + mSearchCode + "%" + "' group by word";
		cursor = dbWords.query(true, TABLE, COLUMNS, selection, null, null, null, ORDERBY, limit.toString());
		//計算總共有幾個注音
		countOfPhonetic[0] = mSearchCode.length()/2;
		if (DEBUG){
			if (cursor != null){
				Log.d(TAG, "cursor.moveToFirst() " + cursor.moveToFirst());
				//Log.d(TAG, "cursor.getString(0) " + cursor.getString(0));
				Log.d(TAG, "mSearchCode " + mSearchCode);
			}
		}
		return cursor;
	}
	
	public Cursor getClosePhrases(Integer limit, int[] countOfPhonetic, ArrayList<String> result) {
		Cursor cursor = null;
		final String TABLE = "phrases_" + mSearchCode.substring(0, 2);
		final String[] COLUMNS = new String[] { "word","code" };
		final String ORDERBY = "use DESC, frequency DESC";
		String selection = "";
		if (mCloseSmartCode.size() > 0){
			if (mCloseSmartCode.size() == 1){
				selection += "(code like '" + mCloseSmartCode.get(0) + "%') ";
				for(int i = 0; i < result.size(); i++) {
					selection += "and word !='" + result.get(i) + "' ";
				}
				selection += "group by word";
			} else{
				for (int i = 0; i < mCloseSmartCode.size(); i++){
					if (i != mCloseSmartCode.size() - 1){
						selection += "code like '" + mCloseSmartCode.get(i) + "%' or ";
					} else{
						selection += "(code like '" + mCloseSmartCode.get(i) + "%') ";
						for(int j = 0; j < result.size(); j++) {
							selection += "and word !='" + result.get(j) + "' ";
						}
						selection += "group by word";
					}
					if (DEBUG) Log.d(TAG, "mCloseSmartCode " + mCloseSmartCode.get(i));
				}
			}
			String[] smartCode = mCloseSmartCode.get(0).split("_");
			for (int i = 0; i < smartCode.length; i++) {
				if (!smartCode[i].contentEquals("")){
					countOfPhonetic[0] += smartCode[i].length()/2;
				}
			}
			cursor = dbPhrases.query(true, TABLE, COLUMNS, selection, null, null, null, ORDERBY, limit.toString());
		}
		return cursor;
	}
	
	public Cursor getSmartRoughPhrase(Integer limit, ArrayList<String> result, int[] countOfPhonetic) {
		final String TABLE = "phrases_" + mSearchCode.substring(0, 2);
		final String[] COLUMNS = new String[] { "word","code" };
		final String ORDERBY = "use DESC, frequency DESC";
		String selection = "";		//存放要收尋資料庫的選擇條件
		Cursor cursor = null;
		//用上面找出的選擇條件來撈出想要的資料組出table
		if (mSmartSearchCode.size() > 0){
			selection = "";
			if (mSmartSearchCode.size() == 1){
				selection += "(code like '" + mSmartSearchCode.get(0) + "%') ";
				for(int i = 0; i < result.size(); i++) {
					selection += "and word !='" + result.get(i) + "' ";
				}
				selection += "group by word";
			} else{
				for (int i = 0; i < mSmartSearchCode.size(); i++){
					if (i != mSmartSearchCode.size() - 1){
						selection += "code like '" + mSmartSearchCode.get(i) + "%' or ";
					} else{
						selection += "(code like '" + mSmartSearchCode.get(i) + "%') ";
						for(int j = 0; j < result.size(); j++) {
							selection += "and word !='" + result.get(j) + "' ";
						}
						selection += "group by word";
					}
				}
			}
			String[] smartCode = mSmartSearchCode.get(0).split("_");
			for (int i = 0; i < smartCode.length; i++) {
				if (!smartCode[i].contentEquals("")){
					countOfPhonetic[0] += smartCode[i].length()/2;
				}
			}
			cursor = dbPhrases.query(true, TABLE, COLUMNS, selection, null, null, null, ORDERBY, limit.toString());
		}
		return cursor;	
	}
	
	public void setSearchCodeInSmartLearning(String code) {
		Cursor cursor = null;
		String tansCode = transDBCode(code);
		int i = 4;
		if (tansCode.length()/2 < i)
			i = tansCode.length()/2;
		for (; i > 0; i--) {
			String newCode = tansCode.substring(0, i * 2);
			if (DEBUG) Log.d(TAG, "newCode " + newCode);
			cursor = dbWords.query(true, "words_" + newCode.substring(0, 2), new String[] { "word","code" }, "code like '" + newCode + "%" + "' group by word", null, null, null, "use DESC, frequency DESC", "2");
			if (DEBUG) Log.d(TAG, "cursor " + cursor.moveToFirst());
			if (cursor.moveToFirst()) {
				mSearchCode = newCode;
				if (DEBUG) Log.d(TAG, "mSearchCode " + mSearchCode);
				break;
			}
		}
	}
}
