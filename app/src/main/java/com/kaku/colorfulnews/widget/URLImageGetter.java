/*
 * Copyright (c) 2016 咖枯 <kaku201313@163.com | 3772304@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.kaku.colorfulnews.widget;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.widget.TextView;

import com.kaku.colorfulnews.App;
import com.kaku.colorfulnews.R;
import com.socks.library.KLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author 咖枯
 * @version 1.0 2016/6/19
 */
public class URLImageGetter implements Html.ImageGetter {
    private TextView mTextView;
    private int mPicWidth;
    private String mNewsBody;
    private int mPicCount;
    private int mPicTotal;
//    private static final String filePath = App.getAppContext().getCacheDir().getAbsolutePath();

    public URLImageGetter(TextView textView, String newsBody, int picTotal) {
        mTextView = textView;
        mPicWidth = mTextView.getWidth();
        mNewsBody = newsBody;
        mPicTotal = picTotal;
    }

    @Override
    public Drawable getDrawable(final String source) {
        Drawable drawable = null;
        File file = new File(App.getAppContext().getCacheDir(), source.hashCode() + "");
        if (source.startsWith("http")) {
            if (file.exists()) {
                drawable = getDrawableFromDisk(file);
            } else {
                drawable = getDrawableFromNet(source);
            }
        }
        return drawable;
    }

    @NonNull
    private Drawable getDrawableFromNet(final String source) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                subscriber.onNext(loadNetPicture(source));
                subscriber.onCompleted();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        KLog.i("onCompleted");

                    }

                    @Override
                    public void onError(Throwable e) {
                        KLog.e(e.toString());
                    }

                    @Override
                    public void onNext(Boolean isLoadSuccess) {
                        mPicCount++;
                        if (/*isLoadSuccess &&*/ (mPicCount == mPicTotal - 1)) {
                            mTextView.setText(Html.fromHtml(mNewsBody, URLImageGetter.this, null));
                        }
                    }
                });

        return createPicPlaceholder();
    }

    @SuppressWarnings("deprecation")
    @NonNull
    private Drawable createPicPlaceholder() {
        Drawable drawable;
        drawable = new ColorDrawable(App.getAppContext().getResources().getColor(R.color.background_color));
        drawable.setBounds(0, 0, mPicWidth, mPicWidth / 3);
        return drawable;
    }

    @Nullable
    private Drawable getDrawableFromDisk(File file) {
        Drawable drawable;
        drawable = Drawable.createFromPath(file.getAbsolutePath());
        if (drawable != null) {
            float imgWidth = drawable.getIntrinsicWidth();
            float imgHeight = drawable.getIntrinsicHeight();
            float rate = imgHeight / imgWidth;

            int picHeight = (int) (mPicWidth * rate);
            drawable.setBounds(0, 0, mPicWidth, picHeight);
        }
        return drawable;
    }

    private boolean loadNetPicture(String filePath) {

        File file = new File(App.getAppContext().getCacheDir(), filePath.hashCode() + "");

        InputStream in = null;

        FileOutputStream out = null;

        try {
            URL url = new URL(filePath);

            HttpURLConnection connUrl = (HttpURLConnection) url.openConnection();

            connUrl.setConnectTimeout(5000);

            connUrl.setRequestMethod("GET");

            if (connUrl.getResponseCode() == 200) {

                in = connUrl.getInputStream();

                out = new FileOutputStream(file);

                byte[] buffer = new byte[1024];

                int len;

                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } else {
                KLog.i(connUrl.getResponseCode() + "");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}