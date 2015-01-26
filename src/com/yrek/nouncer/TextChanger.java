package com.yrek.nouncer;

import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class TextChanger {
    public static void setup(final TextView textView, final EditText editText, final OnTextChanged onTextChanged) {
        textView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                editText.setText(textView.getText());
                textView.setVisibility(View.GONE);
                editText.setVisibility(View.VISIBLE);
                editText.requestFocus();
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String text = editText.getText().toString();
                textView.setText(text);
                textView.setVisibility(View.VISIBLE);
                editText.setVisibility(View.GONE);
                onTextChanged.onTextChanged(text);
                return true;
            }
        });
    }

    public interface OnTextChanged {
        public void onTextChanged(String text);
    }
}
