package com.fochl.rdyst.project_mobile_guard.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.fochl.rdyst.project_mobile_guard.R;
import com.fochl.rdyst.project_mobile_guard.utils.StreamUtils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SplashActivity extends Activity {

    private static final int CODE_UPDATE_DIALOG = 0;
    private static final int CODE_URL_ERROR = 1;
    private static final int CODE_NET_ERROR = 2;
    private static final int CODE_JSON_ERROR = 3;
    private static final int CODE_ENTER_HOME = 4;
    private TextView mTvVersion;
    private TextView mTvProgress;
    private String mVersionName;//服务器版本号名
    private int mVersionCode;//服务器版本号
    private String mDescription;//新版本描述

    private String mDownloadURL;//新版本下载链接
    private static MyHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mTvVersion = (TextView) findViewById(R.id.tv_version);
        mTvVersion.setText("版本号：" + getVersionName());
        mTvProgress = (TextView) findViewById(R.id.tv_progress);

        //创建一个MyHandler静态对象，以防内存泄漏
        mHandler = new MyHandler();
        checkVersion();

    }

    /**
     * 获取本地App的版本名
     *
     * @return String
     */
    private String getVersionName() {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            //没有找到包名
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取本地App的版本号
     *
     * @return int
     */
    private int getVersionCode() {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            //没有找到包名
            e.printStackTrace();
        }
        return -1;
    }


    /**
     * 从服务器获取版本信息进行校验
     */
    private void checkVersion() {

        final long startTime = System.currentTimeMillis();

        new Thread() {

            private HttpURLConnection conn = null;

            @Override
            public void run() {
                Message message = Message.obtain();
                try {
                    URL url = new URL("http://192.168.0.103:8888/update.json");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");//设置请求方法
                    conn.setConnectTimeout(5000);//设置连接超时
                    conn.setReadTimeout(5000);//设置响应超时，连接上了，但服务器迟迟不给响应
                    conn.connect();//连接服务器
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream is = conn.getInputStream();
                        String result = StreamUtils.readFromStream(is);
                        System.out.println("网络结果返回result" + result);
                        //解析json
                        JSONObject jsonObject = new JSONObject(result);
                        mVersionName = jsonObject.getString("versionName");
                        mVersionCode = jsonObject.getInt("versionCode");
                        mDescription = jsonObject.getString("description");
                        mDownloadURL = jsonObject.getString("downloadUrl");
                        System.out.println(mVersionName + mVersionCode + mDescription + mDownloadURL);
                        //判断是否有更新
                        if (mVersionCode > getVersionCode()) {
                            //如果有更新，弹出升级对话框
                            message.what = CODE_UPDATE_DIALOG;
                        } else {
                            //没有版本更新
                            message.what = CODE_ENTER_HOME;
                        }
                    }
                } catch (MalformedURLException e) {
                    //URl错误异常
                    message.what = CODE_URL_ERROR;
                    e.printStackTrace();
                } catch (IOException e) {
                    //网络异常
                    message.what = CODE_NET_ERROR;
                    e.printStackTrace();
                } catch (JSONException e) {
                    //JSON解析失败
                    message.what = CODE_JSON_ERROR;
                    e.printStackTrace();
                } finally {
                    long endTime = System.currentTimeMillis();
                    long timeUsed = endTime - startTime;
                    //展示页面2秒
                    if (timeUsed < 2000) {
                        try {
                            Thread.sleep(2000 - timeUsed);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    mHandler.sendMessage(message);
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }.start();

    }

    protected void showUpdateDailog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setCancelable(false);取消返回按键，影响用户体验
        builder.setTitle("最新版本" + mVersionName);
        builder.setMessage(mDescription);
        builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.out.println("立即更新");
                download();
            }
        });
        builder.setNegativeButton("以后再说", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                enterHome();
            }
        });
        //设置按取消键监听，让用户跳转到主页面
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                enterHome();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    /**
     * 下载apk文件
     */
    protected void download() {
        //判断SD是否有挂载
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            mTvProgress.setVisibility(View.VISIBLE);
            String target = Environment.getExternalStorageDirectory() + "/mobile_guard.apk";
            HttpUtils utils = new HttpUtils();
            utils.download(mDownloadURL, target, new RequestCallBack<File>() {

                /**
                 * 文件的下载进度
                 *
                 * @param total       总的大小
                 * @param current     当前下载大小
                 * @param isUploading 是否上传
                 */
                @Override
                public void onLoading(long total, long current, boolean isUploading) {
                    mTvProgress.setText("当前下载进度：" + current * 100 / total + "%");
                }

                /**
                 * 下载成功
                 *
                 * @param responseInfo
                 */
                @Override
                public void onSuccess(ResponseInfo<File> responseInfo) {
                    System.out.println("下载成功");
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setDataAndType(Uri.fromFile(responseInfo.result), "application/vnd.android.package-archive");
                    startActivityForResult(intent,0);//如果取消安装，会返回结果,回调onActivityResult
                }

                /**
                 * 下载失败
                 *
                 * @param e
                 * @param s
                 */
                @Override
                public void onFailure(HttpException e, String s) {
                    System.out.println("下载失败");
                }
            });
        }
    }

    /**
     * 进入主页面
     */
    private void enterHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        enterHome();
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CODE_UPDATE_DIALOG:
                    showUpdateDailog();
                    break;
                case CODE_URL_ERROR:
                    Toast.makeText(SplashActivity.this, "url错误", Toast.LENGTH_SHORT).show();
                    enterHome();
                    break;
                case CODE_NET_ERROR:
                    Toast.makeText(SplashActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    enterHome();
                    break;
                case CODE_JSON_ERROR:
                    Toast.makeText(SplashActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                    enterHome();
                    break;
                case CODE_ENTER_HOME:
                    enterHome();
                    break;
            }
        }
    }

}
