package com.program.rwpc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements Handler.Callback {
    
    ScrollView logViewContainer;
    TextView logView;
    StringBuilder Logtemp;

    Handler handler;
    NetManager net;

    void popup(String s){
        Toast.makeText(getApplicationContext(),
                s, Toast.LENGTH_SHORT).show();
    }
    //时间分发方法重写

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //如果是点击事件，获取点击的view，并判断是否要收起键盘
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            //获取目前得到焦点的view
            View v = getCurrentFocus();
            //判断是否要收起并进行处理
            if (isShouldHideKeyboard(v, ev)) {
                hideKeyboard(v.getWindowToken());
            }
        }
        //这个是activity的事件分发，一定要有，不然就不会有任何的点击事件了
        return super.dispatchTouchEvent(ev);
    }

    private boolean isShouldHideKeyboard(View v, MotionEvent event) {
        if ((v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0],
                    top = l[1],
                    bottom = top + v.getHeight(),
                    right = left + v.getWidth();
            return !(event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom);
        }
        return false;
    }

    private void hideKeyboard(IBinder token) {
        if (token != null) {
            InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }


    private void log(String ...info)
    {
        for(String s : info) {
            Logtemp.append(s);
            Logtemp.append('\n');
        }
        
        logView.setText(Logtemp.toString());
        logViewContainer.post(new Runnable() {
            public void run() {
                logViewContainer.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void log(StringBuilder sb)
    {
        Logtemp.append(sb);
        logView.setText(Logtemp.toString());
        logViewContainer.post(new Runnable() {
            public void run() {
                logViewContainer.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
    
    private void bindEvent()
    {
        // log
        {
            logView = findViewById(R.id.log_text);
            Logtemp = new StringBuilder();
            logViewContainer = findViewById(R.id.scrollView2);
        }

        {
            TextInputEditText HostName = findViewById(R.id.HostName);
            
            HostName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(!hasFocus) {
                        TextView view1 = (TextView) v;

                        net.popupAddress(view1.getText().toString());
                    }
                }
            });
            
        }

        // save
        {
            Button save = findViewById(R.id.save_button);
            save.setOnClickListener(v -> {
                TextInputEditText HostName = findViewById(R.id.HostName);
                TextInputEditText port = findViewById(R.id.Port);
                TextInputEditText cmd = findViewById(R.id.Command);

                String hostnameStr = HostName.getEditableText().toString();
                String portStr = port.getEditableText().toString();
                String cmdStr = cmd.getEditableText().toString();

                SharedPreferences.Editor sharedata = getSharedPreferences("data", 0).edit();

                sharedata.putString("hostnameStr", hostnameStr);
                sharedata.putString("portStr", portStr);
                sharedata.putString("cmdStr", cmdStr);

                sharedata.apply();
            });
        }
        
        // send
        {
            Button send = findViewById(R.id.send_button);
            send.setOnClickListener(v -> {
                TextInputEditText HostName = findViewById(R.id.HostName);
                String hostNameStr = HostName.getEditableText().toString();
                TextInputEditText port = findViewById(R.id.Port);
                String portStr = port.getEditableText().toString();
                TextInputEditText cmd = findViewById(R.id.Command);
                String cmdStr = cmd.getEditableText().toString();

                try {
                    net.send(hostNameStr, Integer.parseInt(portStr), cmdStr);
                }catch (Exception e) {
                    log(e.getMessage());
                }
            });
        }
    }
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        handler = new Handler(this);
        
        net = new NetManager(handler);
        
        net.beginCheckSocketData();
        
        bindEvent();

        initText();
    }

    private void initText() {
        logView.setText("  ");
        TextInputEditText HostName = findViewById(R.id.HostName);
        TextInputEditText port = findViewById(R.id.Port);
        TextInputEditText cmd = findViewById(R.id.Command);

        SharedPreferences sharedata = getSharedPreferences("data", 0);
        
        HostName.setText(sharedata.getString("hostnameStr", null));
        port.setText(sharedata.getString("portStr", null));
        cmd.setText(sharedata.getString("cmdStr", null));
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if(msg.obj instanceof DatagramPacket) {
            DatagramPacket packet = (DatagramPacket) msg.obj;

            log(new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8));
        }else if(msg.obj instanceof String) {
            if(msg.arg1 == 0) {
                popup((String) msg.obj);
            }else {
                log((String)msg.obj);
            }
        }else if(msg.obj instanceof StringBuilder) {
            log((StringBuilder)msg.obj);
        }else {
            log("msg type error", msg.obj.toString());
        }

        return true;
    }
}