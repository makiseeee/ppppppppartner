package com.example.a121;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Random;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private TextView tvHeartRate, tvBreathRate, tvSleepStatus, tvRoomTemp,
            tvBodyTemp, tvPeopleExist, tvMoveLevel, tvPeopleDistance;
    private ImageView imgSticker;
    private SharedViewModel sharedViewModel;
    private OkHttpClient client;
    private String latestRoomTemp = "--";
    private String latestBodyTemp = "--";



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // UI 初始化
        tvHeartRate     = view.findViewById(R.id.tv_heart_rate);
        tvBreathRate    = view.findViewById(R.id.tv_breath_rate);
        tvSleepStatus   = view.findViewById(R.id.tv_sleep_status);
        tvRoomTemp      = view.findViewById(R.id.tv_room_temp);
        tvBodyTemp      = view.findViewById(R.id.tv_body_temp);
        tvPeopleDistance= view.findViewById(R.id.tv_people_distance);
        tvMoveLevel     = view.findViewById(R.id.tv_move_level);
        imgSticker      = view.findViewById(R.id.img_sticker);

        // ViewModel
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // OkHttpClient
        client = new OkHttpClient();

        // 订阅数据
        sharedViewModel.getHeartRate().observe(getViewLifecycleOwner(),
                value -> tvHeartRate.setText("心率: " + value + " bpm"));
        sharedViewModel.getBreathRate().observe(getViewLifecycleOwner(),
                value -> tvBreathRate.setText("呼吸率: " + value + " bpm"));
        sharedViewModel.getSleepStatus().observe(getViewLifecycleOwner(),
                value -> tvSleepStatus.setText("睡眠状态: " + value));
        sharedViewModel.getRoomTemp().observe(getViewLifecycleOwner(), value -> {
            try {
                double temp = Double.parseDouble(value) / 10.0;
                tvRoomTemp.setText("室温: " + String.format("%.1f", temp) + " ℃");
            } catch (NumberFormatException e) {
                tvRoomTemp.setText("室温: --");
            }
        });

        sharedViewModel.getBodyTemp().observe(getViewLifecycleOwner(), value -> {
            try {
                double temp = Double.parseDouble(value) / 10.0;
                tvBodyTemp.setText("体温: " + String.format("%.1f", temp) + " ℃");
            } catch (NumberFormatException e) {
                tvBodyTemp.setText("体温: --");
            }
        });


        sharedViewModel.getPeopleDistance().observe(getViewLifecycleOwner(),
                value -> tvPeopleDistance.setText("与人距离: " + value + " dm"));
        sharedViewModel.getMoveLevel().observe(getViewLifecycleOwner(),
                value -> tvMoveLevel.setText("活动等级: " + value));

        // 加载表情包
        fetchRandomSticker();

        // 点击换一张
        imgSticker.setOnClickListener(v -> fetchRandomSticker());

        return view;
    }

    private void fetchRandomSticker() {
        String url = "https://www.doro.asia/api/random-sticker";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "获取表情包失败", e);
                showToast("加载表情包失败");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "表情包接口返回码: " + response.code());
                    showToast("表情包接口失败");
                    return;
                }
                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success")) {
                        String imageUrl = json.getJSONObject("sticker").optString("url");
                        requireActivity().runOnUiThread(() ->
                                Glide.with(requireContext())
                                        .load(imageUrl)
                                        .into(imgSticker)
                        );
                    } else {
                        showToast("表情包接口返回失败");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析表情包失败", e);
                    showToast("解析表情包失败");
                }
            }
        });
    }

    private void showToast(String msg) {
        if (getContext() != null) {
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show()
            );
        }
    }
}
