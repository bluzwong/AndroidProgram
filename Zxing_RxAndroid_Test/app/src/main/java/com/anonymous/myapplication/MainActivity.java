package com.anonymous.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.google.zxing.*;
import com.google.zxing.client.android.decode.CaptureActivity;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.google.zxing.common.HybridBinarizer;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import java.util.EnumMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 0;

    @Bind(R.id.qrcode_encode)
    Button mEncodeBtn;
    @Bind(R.id.qrcode_decode)
    Button mDecodeBtn;
    @Bind(R.id.qrcode_img)
    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        // 生成二维码点击事件
        mEncodeBtn.setOnClickListener(this::generateBitmap);
        // 扫描二维码点击事件
        mDecodeBtn.setOnClickListener(this::gotoCapture);
        // 长按解析二维码事件
        mImageView.setOnLongClickListener(this::decodeQrcode);


        /// bluz version

        mEncodeBtn.setOnClickListener(view ->
                getQrBitmapByString("https://github.com/Res2013/AndroidProgram.git")
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(bitmap -> mImageView.setImageBitmap(bitmap),
                                throwable -> throwable.printStackTrace())
        );

        mImageView.setOnClickListener(view ->
                getBitmapByView(view)
                        .flatMap(bitmap ->
                                getStringByQrBitmap(bitmap).subscribeOn(Schedulers.computation()))
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<String>() {
                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                e.printStackTrace();
                            }

                            @Override
                            public void onNext(String result) {
                                Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
                            }
                        }));

    }


    public Observable<Bitmap> getBitmapByView(View view) {
        return Observable.just(view)
                .map(view1 -> {
                    view.setDrawingCacheEnabled(true);
                    Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
                    view.setDrawingCacheEnabled(false);
                    return bitmap;
                });
    }

    public Observable<String> getStringByQrBitmap(Bitmap bitmap) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] pixels = new int[width * height];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                try {
                    Result result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), HINTS);
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(result.getText());
                    }
                } catch (NotFoundException e) {
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        });
    }

    public Observable<Bitmap> getQrBitmapByString(String text) {
        return Observable.create((Observable.OnSubscribe<Bitmap>) subscriber -> {
            try {
                Bitmap bitmap = QRCodeEncoder.encodeAsBitmap(text, 600);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(bitmap);
                }
            } catch (WriterException e) {
                subscriber.onError(e);
            }
            subscriber.onCompleted();
        });
    }

    /**
     * 生成二维码
     */
    public void generateBitmap(View v) {
        try {
            Bitmap mBitmap = QRCodeEncoder.encodeAsBitmap("https://github.com/Res2013/AndroidProgram.git", 600);
            mImageView.setImageBitmap(mBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void gotoCapture(View v) {
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(intent, REQUEST_CODE);
    }

    /**
     * 解析二维码
     */
    public boolean decodeQrcode(View v) {/** 看起来变量v有点多余,为啥要用?是因为参数和返回值同Lambda表达式一样,可用方法引用形式(即用方法名) */
        mImageView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(mImageView.getDrawingCache());
        mImageView.setDrawingCacheEnabled(false);
        decodeBitmap(bitmap);
        return true;
    }


    /**
     * 要解析的二维码图片
     */
    public final Map<DecodeHintType, Object> HINTS = new EnumMap<>(DecodeHintType.class);

    public void decodeBitmap(Bitmap bitmap) {
        Observable.just(bitmap)/** just:获取输入数据,直接分发,更加简洁,省略其他回调 */
                .map(this::doInBackground)/** map:映射,对输入数据进行加工 */
                .subscribeOn(Schedulers.newThread())/** 子线程中执行 */
                .observeOn(AndroidSchedulers.mainThread())/** 主线程调度器传递消息给订阅者 */
                .subscribe(this::doOnUIThread);/** 订阅者最终处理消息 */
    }

    public String doInBackground(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            Result result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), HINTS);
            return result.getText();
        } catch (Exception e) {
            return null;
        }
    }

    public void doOnUIThread(String result) {
        Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString("result");
            Toast.makeText(MainActivity.this, scanResult, Toast.LENGTH_SHORT).show();
        }
    }
}
