/* Copyright 2014 Michael Tsang, Garry Ng, Ken Leung
 *
 * This file is part of Four Corner Input Method.
 *
 * Four Corner Input Method is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Four Corner Input Method is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Four Corner Input Method.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.miklcct.csproject.fourcornerinputmethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.Build;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

public class FourCornerMethodService extends InputMethodService implements
		OnKeyboardActionListener, OnClickListener {

	private static boolean isInputType(int inputType, int constant) {
		return (inputType & constant) == constant;
	}

	private static boolean match(String pattern, String code) {
		if (pattern.length() < 4) {
			return pattern.equals(code);
		}
		for (int i = 0; i < pattern.length(); ++i) {
			if (code.length() < pattern.length() || pattern.charAt(i) != '*'
					&& pattern.charAt(i) != code.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	@Override
	@TargetApi(9)
	public void onClick(View v) {
		String committedText = ((Button) v).getText().toString();
		getCurrentInputConnection().commitText(committedText, 1);
		if (!isInputType(getCurrentInputEditorInfo().inputType,
				InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
			countsEditor.putInt(committedText,
					counts.getInt(committedText, 0) + 1);
			if (Build.VERSION.SDK_INT >= 9) {
				countsEditor.apply();
			} else {
				countsEditor.commit();
			}
		}
		setComposingText("");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		table = new TreeMap<String, List<String>>();
		counts = getSharedPreferences("counts", MODE_PRIVATE);
		countsEditor = counts.edit();
		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(
					getAssets().open("table.txt")));
			String line;
			while ((line = input.readLine()) != null) {
				String[] tokens = line.split(" ");
				String key = tokens[0];
				if (!table.containsKey(key)) {
					table.put(key, new ArrayList<String>());
				}
				for (int i = 1; i < tokens.length; ++i) {
					table.get(key).add(tokens[i]);
				}
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public View onCreateCandidatesView() {
		candidatesView = new CandidatesView(this, numCandidates);
		return candidatesView;
	}

	@Override
	public KeyboardView onCreateInputView() {
		keyboardView = new KeyboardView(this, null) {
			@Override
			protected boolean onLongPress(Keyboard.Key key) {
				if (key.codes[0] == Keyboard.KEYCODE_MODE_CHANGE) {
					((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
							.showInputMethodPicker();
					return true;
				} else {
					return super.onLongPress(key);
				}
			}
		};
		keyboardView.setOnKeyboardActionListener(this);
		return keyboardView;
	}

	@Override
	@TargetApi(9)
	public void onDestroy() {
		if (Build.VERSION.SDK_INT >= 9) {
			countsEditor.apply();
		} else {
			countsEditor.commit();
		}
		super.onDestroy();
	}

	@Override
	public void onKey(int primaryCode, int[] keyCodes) {
		if (primaryCode == KEYCODE_POEM) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.poem_title);
			builder.setMessage(R.string.poem_content);
			AlertDialog alert = builder.create();
			Window window = alert.getWindow();
			WindowManager.LayoutParams lp = window.getAttributes();
			lp.token = keyboardView.getWindowToken();
			lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
			window.setAttributes(lp);
			window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
			alert.show();
		} else if (primaryCode == Keyboard.KEYCODE_DELETE) {
			if (composingText.length() > 0) {
				setComposingText(composingText.substring(0,
						composingText.length() - 1));
			} else {
				int size;
				CharSequence prevCharSeq = getCurrentInputConnection()
						.getTextBeforeCursor(1, 0);
				if (prevCharSeq.length() == 0) {
					size = 0;
				} else {
					final char c = prevCharSeq.charAt(0);
					size = new Object() {
						@TargetApi(19)
						int getSize() {
							if (Build.VERSION.SDK_INT >= 19) {
								if (Character.isSurrogate(c)) {
									return 2;
								} else {
									return 1;
								}
							} else {
								if (c >= 0xd800 && c <= 0xdfff) {
									return 2;
								} else {
									return 1;
								}
							}
						}
					}.getSize();
				}
				getCurrentInputConnection().deleteSurroundingText(size, 0);
			}
		} else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
			switch (mode) {
			case MODE_NUMBER:
				setMode(MODE_CHINESE);
				break;
			case MODE_CHINESE:
				setMode(MODE_SYMBOLS);
				break;
			case MODE_SYMBOLS:
				setMode(MODE_NUMBER);
			}
		} else if (primaryCode == '\n') {
			if (composingText.length() > 0) {
				getCurrentInputConnection().finishComposingText();
				setComposingText("");
			} else if ((getCurrentInputEditorInfo().inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0) {
				getCurrentInputConnection().performEditorAction(
						getCurrentInputEditorInfo().actionId);
			} else {
				getCurrentInputConnection().commitText("\n", 1);
			}
		} else if (mode == MODE_CHINESE
				&& (primaryCode >= '0' && primaryCode <= '9' || primaryCode == '*')) {
			if (composingText.length() < 5) {
				setComposingText(composingText + (char) primaryCode);
			}
		} else if (primaryCode > 0) {
			getCurrentInputConnection().finishComposingText();
			setComposingText("");
			getCurrentInputConnection().commitText(
					String.valueOf((char) primaryCode), 1); // no non-BMP
															// characters here
			if (primaryCode == '…' || primaryCode == '—') {
				getCurrentInputConnection().commitText(
						String.valueOf((char) primaryCode), 1);
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		int code = 0;
		if (keyCode == KeyEvent.KEYCODE_DEL) {
			code = Keyboard.KEYCODE_DELETE;
		} else {
			int c = event.getUnicodeChar();
			if ((c >= '0' && c <= '9') || c == '*') {
				code = c;
			}
		}
		if (code != 0) {
			int[] alternatives = { code };
			onKey(code, alternatives);
			return true;
		}
		if (getCurrentInputConnection() != null) {
			getCurrentInputConnection().finishComposingText();
			setComposingText("");
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
			int[] alternatives = { '\n' };
			onKey(alternatives[0], alternatives);
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onPress(int primaryCode) {
	}

	@Override
	public void onRelease(int primaryCode) {
	}

	@Override
	public void onStartInputView(EditorInfo info, boolean restarting) {
		if (isInputType(info.inputType, InputType.TYPE_CLASS_PHONE)) {
			setMode(MODE_PHONE);
		} else if (isInputType(info.inputType, InputType.TYPE_CLASS_NUMBER)
				|| isInputType(info.inputType, InputType.TYPE_CLASS_DATETIME)) {
			setMode(MODE_NUMBER);
		} else {
			setMode(MODE_CHINESE);
		}
	}

	@Override
	public void onText(CharSequence text) {
	}

	@Override
	public void swipeDown() {
	}

	@Override
	public void swipeLeft() {
	}

	@Override
	public void swipeRight() {
	}

	@Override
	public void swipeUp() {
	}

	private Keyboard getKeyboard() {
		switch (mode) {
		case MODE_NUMBER:
			return new Keyboard(this, R.xml.numbers);
		case MODE_CHINESE:
			return new Keyboard(this, R.xml.chinese);
		case MODE_PHONE:
			return new Keyboard(this, R.xml.phone);
		case MODE_SYMBOLS:
			return new Keyboard(this, R.xml.symbols);
		default:
			return null;
		}
	}

	private void setComposingText(String string) {
		composingText = string;
		if (getCurrentInputConnection() != null) {
			getCurrentInputConnection().setComposingText(composingText, 1);
		}
		boolean shown = composingText.length() > 0;
		if (shown) {
			List<String> candidatesList = new ArrayList<String>();
			for (Map.Entry<String, List<String>> entry : table.entrySet()) {
				if (match(composingText.toString(), entry.getKey())) {
					candidatesList.addAll(entry.getValue());
				}
			}
			if (composingText.length() >= 4) {
				Collections.sort(candidatesList, new Comparator<String>() {
					@Override
					public int compare(String lhs, String rhs) {
						int lhsCount = counts.getInt(lhs, 0);
						int rhsCount = counts.getInt(rhs, 0);
						if (lhsCount < rhsCount)
							return 1;
						if (lhsCount > rhsCount)
							return -1;
						return 0;
					}
				});
			}
			candidatesView.setCandidates(candidatesList
					.toArray(new String[candidatesList.size()]));
		}
		setCandidatesViewShown(shown);
	}

	private void setMode(int newMode) {
		mode = newMode;
		keyboardView.setKeyboard(getKeyboard());
		if (getCurrentInputConnection() != null) {
			getCurrentInputConnection().finishComposingText();
			setComposingText("");
		}
	}

	public static final int MODE_CHINESE = 1;
	public static final int MODE_NUMBER = 0;
	public static final int MODE_PHONE = 2;
	public static final int MODE_SYMBOLS = 3;
	private static final int KEYCODE_POEM = -100;
	private CandidatesView candidatesView;

	private String composingText = "";

	private SharedPreferences counts;
	private SharedPreferences.Editor countsEditor;
	private KeyboardView keyboardView;
	private int mode = MODE_CHINESE;
	private int numCandidates = 5;
	private TreeMap<String, List<String>> table;
}
