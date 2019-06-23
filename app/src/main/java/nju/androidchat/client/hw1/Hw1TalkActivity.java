package nju.androidchat.client.hw1;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.extern.java.Log;
import nju.androidchat.client.ClientMessage;
import nju.androidchat.client.R;
import nju.androidchat.client.Utils;
import nju.androidchat.client.component.ItemTextReceive;
import nju.androidchat.client.component.ItemTextSend;
import nju.androidchat.client.component.OnRecallMessageRequested;

@Log
public class Hw1TalkActivity extends AppCompatActivity implements Hw1Contract.View, TextView.OnEditorActionListener, OnRecallMessageRequested {

    private Hw1Contract.Presenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Hw1TalkModel hw1TalkModel = new Hw1TalkModel();

        // Create the presenter
        this.presenter = new Hw1TalkPresenter(hw1TalkModel, this, new ArrayList<>());
        hw1TalkModel.setIHw1TalkPresenter(this.presenter);
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.start();
    }

    @Override
    public void showMessageList(List<ClientMessage> messages) {
        runOnUiThread(() -> {
                LinearLayout content = findViewById(R.id.chat_content);

                // 删除所有已有的ItemText
                content.removeAllViews();

                new Thread(() ->{
                    // 增加ItemText
                    for (ClientMessage message : messages) {
                        String text = String.format("%s", message.getMessage());

//                        TextView textView_t = textView;

                        CharSequence charSequence = null;

                        // 图片转换
                        // Test: ![]({https://pic4.zhimg.com/80/v2-72693a3583c495d251866e933e3a132e_hd.jpg})
                        if (text.startsWith("![]({") && text.endsWith("})")) {

                            String url = text.substring(5, text.length()-2);
                            String imgText = "图片: "+ "<img src='" + url + "'>";

                            charSequence = Html.fromHtml(imgText, new Html.ImageGetter() {
                                @Override
                                public Drawable getDrawable(String source) {
                                    Drawable drawable = getOnlineImg(url);
                                    drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                                    return drawable;
                                }
                            }, null);

                        } else {
                            charSequence = text;
                        }

                        CharSequence itemText = charSequence;

                        runOnUiThread(() ->{
                            // 如果是自己发的，增加ItemTextSend
                            if (message.getSenderUsername().equals(this.presenter.getUsername())) {
                                ItemTextSend itemTextSend = new ItemTextSend(this, itemText, message.getMessageId(), this);
                                content.addView(itemTextSend);
                            } else {
                                ItemTextReceive itemTextReceive = new ItemTextReceive(this, itemText, message.getMessageId());
                                content.addView(itemTextReceive);
                            }
                        });
                    }

                }).start();

                Utils.scrollListToBottom(this);
            }
        );
    }

    @Override
    public void setPresenter(Hw1Contract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (null != this.getCurrentFocus()) {
            return hideKeyboard();
        }
        return super.onTouchEvent(event);
    }

    private boolean hideKeyboard() {
        return Utils.hideKeyboard(this);
    }


    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (Utils.send(actionId, event)) {
            hideKeyboard();
            // 异步地让Controller处理事件
            sendText();
        }
        return false;
    }

    private void sendText() {
        EditText text = findViewById(R.id.et_content);
        AsyncTask.execute(() -> {
            this.presenter.sendMessage(text.getText().toString());
        });
    }

    public void onBtnSendClicked(View v) {
        hideKeyboard();
        sendText();
    }

    private Drawable getOnlineImg(String urlStr) {
        Drawable drawable = null;

        try {
            URL url = new URL(urlStr);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.connect();

            InputStream is = conn.getInputStream();
            drawable = Drawable.createFromStream(is, "");
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return drawable;
    }

    // 当用户长按消息，并选择撤回消息时做什么，MVP-0不实现
    @Override
    public void onRecallMessageRequested(UUID messageId) {

    }
}
