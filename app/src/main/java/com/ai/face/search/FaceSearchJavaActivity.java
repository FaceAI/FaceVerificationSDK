package com.ai.face.search;

import static com.ai.face.FaceApplication.CACHE_SEARCH_FACE_DIR;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.*;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.THRESHOLD_ERROR;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import com.ai.face.R;
import com.ai.face.base.view.CameraXFragment;
import com.ai.face.databinding.ActivityFaceSearchBinding;
import com.ai.face.faceSearch.search.FaceSearchEngine;
import com.ai.face.faceSearch.search.SearchProcessBuilder;
import com.ai.face.faceSearch.search.SearchProcessCallBack;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import java.io.File;

/**
 * 应多位用户要求，默认使用java 版本演示怎么快速接入SDK。JAVA FIRST
 *
 */
public class FaceSearchJavaActivity extends AppCompatActivity {
    private ActivityFaceSearchBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFaceSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tips.setOnClickListener(v -> {
            startActivity(new Intent(this, FaceImageEditActivity.class));
        });

        SharedPreferences sharedPref = getSharedPreferences("faceVerify", Context.MODE_PRIVATE);

        // 1. Camera 的初始化。第一个参数0/1 指定前后摄像头； 第二个参数linearZoom [0.1f,1.0f] 指定焦距，默认0.1
        int cameraLens = sharedPref.getInt("cameraFlag", sharedPref.getInt("cameraFlag", 0));
        CameraXFragment cameraXFragment = CameraXFragment.newInstance(cameraLens,0.11f);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraXFragment)
                .commit();

        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            //可以加个红外检测之类的，有人靠近再启动检索服务，不然机器老化快
            if (!isDestroyed() && !isFinishing()) {
                FaceSearchEngine.Companion.getInstance().runSearch(imageProxy, 0);
            }
        });


        // 2.各种参数的初始化设置
        SearchProcessBuilder faceProcessBuilder = new SearchProcessBuilder.Builder(getApplication())
                .setLifecycleOwner(this)
                .setThreshold(0.82f)               //识别成功阈值设置，范围仅限 [0.8 , 0.9] 建议0.8+
                .setLicenceKey("yourLicense key")  //合作的VIP定制客户群体需要
                .setFaceLibFolder(CACHE_SEARCH_FACE_DIR)  //内部存储目录中保存N 个图片库的目录
                .setProcessCallBack(new SearchProcessCallBack() {
                    @Override
                    public void onMostSimilar(String similar) {
                        binding.searchTips.setText(similar);
                        Glide.with(getBaseContext())
                                .load(CACHE_SEARCH_FACE_DIR + File.separatorChar + similar)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .transform(new RoundedCorners(11))
                                .into(binding.image);
                    }

                    @Override
                    public void onProcessTips(int i) {
                        showPrecessTips(i);
                    }

                    @Override
                    public void onLog(String log) {
                        binding.tips.setText(log);
                    }

                }).create();


        //3.初始化引擎，是个耗时耗资源操作
        FaceSearchEngine.Companion.getInstance().initSearchParams(faceProcessBuilder);

    }


    /**
     * 显示提示
     *
     * @param code
     */
    private void showPrecessTips(int code) {
        binding.image.setImageResource(R.mipmap.ic_launcher);
        switch (code) {
            default:
                binding.searchTips.setText("提示码："+code);
                break;

            case THRESHOLD_ERROR :
                binding.searchTips.setText("识别阈值Threshold范围为0.8-0.9");
                break;

            case MASK_DETECTION:
                binding.searchTips.setText("请摘下口罩"); //默认无
                break;

            case NO_LIVE_FACE:
                binding.searchTips.setText("未检测到人脸");
                break;

            case EMGINE_INITING:
                binding.searchTips.setText("初始化中");
                break;

            case FACE_DIR_EMPTY:
                binding.searchTips.setText("人脸库为空");
                break;

            case NO_MATCHED: {
                //本次摄像头预览帧无匹配而已，会快速取下一帧进行分析检索
                binding.searchTips.setText("暂无匹配人脸");
                break;
            }

            case SEARCHING: {
                binding.searchTips.setText("搜索中");
                break;
            }
        }
    }

    /**
     * 销毁，停止
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        FaceSearchEngine.Companion.getInstance().stopSearchProcess();
    }


}