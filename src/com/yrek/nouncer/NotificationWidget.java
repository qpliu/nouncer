package com.yrek.nouncer;

import android.view.View;
import android.widget.TextView;

class NotificationWidget extends Widget {
    private final TextView notificationText;

    NotificationWidget(final Main activity, int id) {
        super(activity, id);
        notificationText = (TextView) view;
        view.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                hide();
            }
        });
    }

    void show(String notification) {
        StringBuilder sb = new StringBuilder();
        sb.append(notificationText.getText());
        if (sb.length() > 0) {
            sb.append("\n");
        }
        sb.append(notification);
        notificationText.setText(sb);
        show();
    }

    @Override
    public void onHide() {
        notificationText.setText("");
    }
}
