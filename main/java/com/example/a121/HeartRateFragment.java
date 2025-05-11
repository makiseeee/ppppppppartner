package com.example.a121;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class HeartRateFragment extends Fragment {
    private TextView tvHeartRate, tvHeartRateRange, tvHeartRateMaxMin;
    private GraphView chartView;
    private MediaPlayer mediaPlayer;
    private SharedViewModel sharedViewModel;
    private boolean isPeoplePresent = false; // 新增：人员存在状态

    // HeartRateFragment.java 关键修改
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // 添加数据观察日志
        sharedViewModel.getHeartRate().observe(this, value -> {
            Log.d("HeartRate", "收到心率数据: " + value);
            if (value != null) {
                tvHeartRate.setText("心率: " + value + " 次/分钟");
                try {
                    int hrValue = Integer.parseInt(value);
                    checkHeartRateAlarm(hrValue);
                } catch (NumberFormatException e) {
                    Log.e("HeartRate", "数据格式错误: " + value);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heart_rate, container, false);

        tvHeartRate = view.findViewById(R.id.tv_heart_rate);
        tvHeartRateRange = view.findViewById(R.id.tv_heart_rate_range);
        tvHeartRateMaxMin = view.findViewById(R.id.tv_heart_rate_max_min);
        chartView = view.findViewById(R.id.chart_heart_rate);

        tvHeartRateRange.setText("参考值: 60-100 次/分钟");




        loadHistoryData();
        setupChart();

        return view;
    }

    private void checkHeartRateAlarm(int heartRate) {
        if (heartRate < 60 || heartRate > 100) {
            // 有人且心率异常时才报警
            tvHeartRate.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvHeartRate.setText(tvHeartRate.getText() + " ⚠️ 异常！");

            Toast.makeText(getContext(), "⚠️ 心率异常: " + heartRate + " 次/分钟", Toast.LENGTH_LONG).show();

            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        } else {
            resetAlarmState();
        }
    }

    private void resetAlarmState() {
        // 重置报警状态
        tvHeartRate.setTextColor(getResources().getColor(android.R.color.black));
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
        }
        // 移除异常提示文本
        String currentText = tvHeartRate.getText().toString();
        if (currentText.contains("⚠️")) {
            tvHeartRate.setText(currentText.replace(" ⚠️ 异常！", ""));
        }
    }

    private void loadHistoryData() {
        LiveData<String> heartRateMax = MyApplication.getDatabase().sensorDataDao().getMaxValue("hr");
        LiveData<String> heartRateMin = MyApplication.getDatabase().sensorDataDao().getMinValue("hr");
        heartRateMax.observe(getViewLifecycleOwner(), max ->
                heartRateMin.observe(getViewLifecycleOwner(), min -> {
                    tvHeartRateMaxMin.setText("历史最高/最低: " +
                            (max != null ? max : "--") + " / " +
                            (min != null ? min : "--"));
                })
        );
    }

    private void setupChart() {
        LiveData<List<SensorData>> heartRateData = MyApplication.getDatabase().sensorDataDao().getDataByType("hr");
        heartRateData.observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                List<DataPoint> points = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    try {
                        double value = Double.parseDouble(data.get(i).value);
                        points.add(new DataPoint(i, value));
                    } catch (NumberFormatException e) {
                        Log.e("HeartRateFragment", "无效的心率数据: " + data.get(i).value);
                    }
                }
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(points.toArray(new DataPoint[0]));
                series.setColor(getResources().getColor(android.R.color.holo_red_light));
                series.setTitle("心率数值");
                chartView.addSeries(series);
                chartView.getViewport().setScalable(true);
                chartView.getViewport().setScrollable(true);
                chartView.setTitle("心率数值");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}