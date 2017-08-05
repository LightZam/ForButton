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
	private List<String> mWordCode;					//�s��C���q��Ʈw���X��table���Acode��줤��code
	private static List<int[]> countOfPhonetic = new ArrayList<int[]>();//�s��C�Ӧr�βզX�r�O�δX�Ӫ`���զX�X�Ӫ�;
	private String continuousCode = "";				//�s����X�L�å[�W_��%��SQL�y�k��CODE
	private String remainingWord = "";
	private String currentValidCode = "";			//�s��Ĥ@�ӥi�H�b��Ʈw��쪺CODE
	private String learningCode = "";				//�s��ǲߤ��զX�r��CODE
	private String learningWord = "";				//�s��ǲߤ��զX�r���r
	private String originalCode = "";				//�s��ϥΪ̭���ҥ���CODE�A��������ץ�����
	private String originalcontinuousCode = "";
	public Boolean isSmartSearching = false;
	public Boolean isSmartLearning = false;
	public Boolean isPicked = false;
	private Context mContext;						//�s��ForButton��J�kSERVICE
	public boolean spacePressed = false;

	public ForbuttonDictionary(Context context) {
		mContext = context;
		MAXWORDS = CandidateView.getMaxSuggest();
	}

	/**
	 * �qdb�̭����o�������r
	 * @author Zam
	 * @Last_Edit_Time 2011/8/19
	 * */
	@Override
	public void getWords(WordComposer composer, WordCallback callback) {
		String code = "";
		// composer.getTypedWord().length() ���o�ϥΪ̥ثe�����r��(���e�X)
		// �N����code ��b�@�_ �C��code ��������� ex 12345 �Ÿ���3���
		for (int i = 0; i < composer.getTypedWord().length(); i++) {
			code += String.valueOf(composer.getCodesAt(i)[0]);
		}
		// ���o��ĳ���r�X��
		// �b�Կ�C�|�X�{�����Ǧr
		//String[] result = this.loadWordDB(code);
		String[] result = this.loadWordFromDB(code);
		// �N�r��Jcallback
		for (String s : result) {
			// �নchar array
			char[] word = s.toCharArray();
			// ��J
			// addWord(char[] word, int wordOffset, int wordLength, int
			// frequency)
			// word �r���}�C�̭���r
			// wordOffset �b�}�C�̭��r���������q
			// wordLength �b�}�C�̭����Ī��r��������
			// frequency �o�ͪ��W�v�A�q�`��1~255���O�i�H�W�L�o�ǭ���
			callback.addWord(word, 0, word.length, 10);

			// Log.d("Test", "callback addWord["+ i + "]" + word[i] );
		}
	}

	/**
	 * �qdb�̭����o���p���r
	 * @author Zam
	 * @Last_Edit_Time 2011/8/25
	 * */
	public void getWordAssociation(WordComposer composer,
			WordCallback callback, String selectedWord, int index) {
		String code = "";
		// composer.getTypedWord().length() ���o�ϥΪ̥ثe�����r��(���e�X)
		// �N����code ��b�@�_ �C��code ��������� ex 12345 �Ÿ���3���
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
		
		// ���o��ĳ���r�X��
		// �b�Կ�C�|�X�{�����Ǧr
		String[] result = this.loadWordAssociation(code, selectedWord, index);

		// �N�r��Jcallback
		for (String s : result) {
			// �নchar array
			char[] word = s.toCharArray();
			// ��J
			// addWord(char[] word, int wordOffset, int wordLength, int
			// frequency)
			// word �r���}�C�̭���r
			// wordOffset �b�}�C�̭��r���������q
			// wordLength �b�}�C�̭����Ī��r��������
			// frequency �o�ͪ��W�v�A�q�`��1~255���O�i�H�W�L�o�ǭ���
			callback.addWord(word, 0, word.length, 10);
		}
	}

	@Override
	public boolean isValidWord(CharSequence word) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * �N�ϥΪ��r�W�[�ϥΦ��ơA�H�K�u����r
	 * @author Zam
	 * @Last_Edit_Time 2011/9/23
	 * */
	public void useWordDB(String word) {
		ForbuttonDictionaryProvider zdb = new ForbuttonDictionaryProvider(
				mContext);
		zdb.open();
		// �ϥΦr�̪�db use ++
		zdb.useWords(word);
		zdb.close();
	}
	
//	/**
//	 * �N�ϥΪ��r�W�[�ϥΦ��ơA�H�K�u����r
//	 * @author Zam
//	 * @Last_Edit_Time 2011/9/23
//	 * */
//	public void useAssociationDB(String word) {
//		ForbuttonDictionaryProvider zdb = new ForbuttonDictionaryProvider(
//				mContext);
//		zdb.open();
//		// �ϥΦr�̪�db use ++
//		zdb.useAssociation(word);
//		zdb.close();
//	}

	/**
	 * �N���p�r���x�s�Ŷ����M�Ū��ʧ@..
	 * @author Zam
	 * @Last_Edit_Time 2011/9/25
	 * */
	public void clearAssciationWord() {
		//reset�`�Φr�����p�r
		//remainingWord�O�զX�ϥΪ̫��U�h�����p�r�A�M��bdb�@�ϥ�++���ʧ@�A�i�H���`�Φr�]��e��
		remainingWord = "";
	}
	
	/** TODO �ץ�����  ���˴�
	 * �R���r
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
	 * �p��Code�̭��`�@���X�Ӧr
	 * @author Zam
	 * @Last_Edit_Time 2011/9/23
	 * */
	private int wordCounter(String code){
		int count = 0;
		String subCode;
		//����j��A���٨S���Ncode�������X��
		//����code���̭����`�����Q���X�h�����^��
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
	 * �����̫�@�Ӧr��code�X��
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
	 * �����Ĥ@�Ӧr��code�X��
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
//	 * ����������code�X��
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
//	 * �����Ĥ@�զr���T��code�X��
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
//	 * ���o��T�r ex �t �u �v �w �x �y
//	 * @author Zam
//	 * @Last_Edit_Time 2011/9/28
//	 * */
//	private void getWordExactly(Cursor cursor, ForbuttonDictionaryProvider zdb, ArrayList<String> result
//											, int selectionMode, String code, int leftWords){
//		//�h��Ʈw�����
//		cursor = zdb.getWordsExactly(leftWords);
//		//�p�G����Ƥ~�|����
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
//	 * ���o�j���r ex �S �� �� ....
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
//	 * ���o�ƦX�r ex �S�� �S���D �S���� ...
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
//					//����0~middle + % + ����middle~end  ex : A%B  A�O�����զn���r,B�O�̫�@�ӿ�J���r
//					continuousCode = continuousCode.substring(0,middle)
//							+ "%"
//							+ continuousCode.substring(middle);
//					Log.d(TAG, "continuousCode 33 " + continuousCode);
//					originalcontinuousCode = continuousCode;
//					
//					//�ഫ�r�X
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
//	 * �qdb���J�r�w�A�@���r�P���z��r
//	 * @author Zam
//	 * @Last_Edit_Time 2011/9/25
//	 * */
//	public String[] loadWordDB(String code) {
//		// �bPreference�W�W�w�ҭn������r�ƭ���
//		//�ثe����hardcode�ȩw
//		Integer leftWords = 50;
//		Cursor cursor = null;
//		ForbuttonDictionaryProvider zdb = new ForbuttonDictionaryProvider(mContext);
//		zdb.open();
//		mWordCode = new ArrayList<String>();
//		ArrayList<String> result = new ArrayList<String>();
//		
//		//���z��r��l��
//		int selectionMode = 3;
//		isSmartWordMode = false;
//		originalCode = code;
//		//�qcode��X�̭����X�Ӫ`���Ϊ`���Ÿ�
//		int numOfWord = wordCounter(code);
//		//�]�w�n���M��code
//		zdb.setSearchCode(code,true);
//		if (!isSmartLearning){
//			//�u���Ĥ@�Ӫ`���Ÿ��|�i�J
//			if (numOfWord == 1) {
//				getWordExactly(cursor, zdb, result, selectionMode, code, leftWords);
//			}
//			
//			// ���o�j���r ex �S �� �� ....
//			getWordsRough(cursor, zdb, result, selectionMode, code, leftWords, numOfWord);
//		}
//		// ���o�ƦX�r ex �S�� �S���D �S���� ...
//		getPhrases(cursor, zdb, result, selectionMode, code, leftWords, numOfWord);
//		zdb.close();
//			
//		return result.toArray(new String[0]);
//	}
	
	/**
	 * �N�ϥΪ̩ҫ��쪺association���r�A�bdb�Wuse++�A���ϥΪ̱`�Φr�i�H�Ʀb�e��
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
	 * ���R��A�qdb�����X�Pword�����p���r
	 * @author Zam
	 * @param code �w��X�Lcode
	 * @param selectedWord �Q�諸�r
	 * @param index �Q�諸�r�bsuggestion�̭�����m
	 * @Last_Edit_Time 2011/9/25
	 * */
	public String[] loadWordAssociation(String code, String selectedWord,
			int index) {
		
		updateAssociation(selectedWord);
		// �bPreference�W�W�w�ҭn������r�ƭ���
		//�ثe����hardcode�ȩw
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
		
		// ���o�ƦX�r ex �S�� �S���D �S���� ...
		// ���ؽƦX�r�~�|�i�J�H�U�j��
		if (leftWords > 0) {
			cursor = zdb.getAssociationWord(leftWords, selectedWord);
			if (cursor.moveToFirst()) {
				while (!cursor.isAfterLast()) {
					// ���X�ƦX�r��code
					String phrasesCode = cursor.getString(1);
					
					if (!phrasesCode.contentEquals(phrasesCode.replaceFirst(code, ""))
							&& !phrasesCode.replaceFirst(code, "").contentEquals("")){
						phrasesCode = phrasesCode.replaceFirst(code, "");
						mWordCode.add(phrasesCode);
						// ���X�ƦX�r
						String aword = cursor.getString(0);
						// ��Ĥ@�Ӧr
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
	 ***** �}�o���ݴ��X******
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
	 ***** �}�o���{���X******
	 ***********************/
	
	/** TODO �ץ�����  ���˴�
	 * �R���r
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
	 * �N���z��r���x�s�Ŷ����M�Ū��ʧ@
	 * @author Zam
	 * @Last_Edit_Time 2011/9/23
	 * */
	public void clear() {
		isSmartLearning = false;
		isSmartSearching = false;
		countOfPhonetic = new ArrayList<int[]>();//�s��C�Ӧr�βզX�r�O�δX�Ӫ`���զX�X�Ӫ�;
		continuousCode = "";				//�s����X�L�å[�W_��%��SQL�y�k��CODE
		remainingWord = "";
		learningCode = "";				//�s��ǲߤ��զX�r��CODE
		learningWord = "";				//�s��ǲߤ��զX�r���r
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
		int numOfWord = wordCounter(code);			//�qcode��X�̭����X�Ӫ`���Ϊ`���Ÿ�
		if (isSmartLearning && isPicked){
			zdb.setSearchCodeInSmartLearning(code);			//�]�w�n���M��code
		}
		//�Ĥ@����J�~������
		if (numOfWord == 1){
			zdb.setSearchCode(code,true);			//�]�w�n���M��code
			getPhonetic(cursor, zdb, result, code);   	//���`��
			getFirstRoughWord(cursor, zdb, result);	//���r
		} else {
			getSingleWord(cursor, zdb, result, code);		//���r
			if (result.size() != 0){
				getPhrases(cursor, zdb, result, code);	//���r�᭱���զX�r
			}
			//�n��X��J���z�r�����G�A�����n�W����Ө��r���S���b�i�h��
			if (result.size() == 0){
				if (!isSmartSearching){
					isSmartSearching = true;
					zdb.setSmartSearchCode();
				}
//				if (!isSmartLearning){
					getSmartPhrases(cursor, zdb, result, code);	//���X��J���z�r�����G   ex: ��J���t��  > �X�{��B���ﶵ
					//�S������J���z�r�����G�A�N���Ʈw�S���o�Ӧr�A�i�J�ǲ߼Ҧ�
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
