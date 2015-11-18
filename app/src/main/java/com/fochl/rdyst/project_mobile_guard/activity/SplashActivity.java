package com.fochl.rdyst.project_mobile_guard.activity;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import com.fochl.rdyst.project_mobile_guard.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SplashActivity extends Activity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        textView = (TextView) findViewById(R.id.tv_version);
        textView.setText("版本号：" + getVersionName());

    }

    /**
     * 获取版本名
     * @return
     */
    private String getVersionName(){
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            int versionCode = packageInfo.versionCode;
            String versionName = packageInfo.versionName;
            System.out.println("versionCode = "+versionCode+",versionName = "+versionName);
            return versionName;
        } catch (PackageManager.NameNotFoundException e) {
            //没有找到包名
            e.printStackTrace();
        }
        return "";
    }


    /**
     * 从服务器获取版本信息进行校验
     */
    private void checkVersion(){
        new Thread(){
            @Override
            public void run() {
                try {
                    URL url = new URL("http://10.0.2.2:8888/update.json");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");//设置请求方法
                    conn.setConnectTimeout(5000);//设置连接超时
                    conn.setReadTimeout(5000);//设置响应超时，连接上了，但服务器迟迟不给响应
                    conn.connect();//连接服务器
                    int responseCode = conn.getResponseCode();
                    if(responseCode == 200){
                        InputStream is = conn.getInputStream();
                    }
                } catch (MalformedURLException e) {
                    //URl错误异常
                    e.printStackTrace();
                } catch (IOException e){
                    //网络异常
                    e.printStackTrace();
                }
            }
        }.start();

    }

}
