package com.example.a121;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.util.ArrayList;
import java.util.List;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class SleepFragment extends Fragment {
    private TextView tvSleepStatus, tvAwakeTime, tvLightSleepTime, tvDeepSleepTime, tvSleepScore;
    private GraphView chartView;
    private SharedViewModel sharedViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sleep, container, false);

        // 初始化UI组件（精简示例，保留关键组件）
        tvSleepStatus = view.findViewById(R.id.tv_sleep_status);
        chartView = view.findViewById(R.id.chart_sleep);



        setupChart();
        return view;
    }

    private String parseSleepStatus(String value) {
        try {
            int status = Integer.parseInt(value);
            switch(status) {
                case 0: return "清醒";
                case 1: return "浅睡";
                case 2: return "深睡";
                case 3: return "快速眼动";
                default: return "未知状态";
            }
        } catch (NumberFormatException e) {
            return "数据异常";
        }
    }

    private void setupChart() {
        LiveData<List<SensorData>> sleepData = MyApplication.getDatabase().sensorDataDao().getDataByType("sc");
        sleepData.observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                List<DataPoint> points = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    try {
                        points.add(new DataPoint(i, Double.parseDouble(data.get(i).value)));
                    } catch (NumberFormatException e) {
                        Log.e("SleepFragment", "无效的睡眠数据: " + data.get(i).value);
                    }
                }
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(points.toArray(new DataPoint[0]));
                series.setColor(getResources().getColor(android.R.color.holo_blue_light));
                chartView.addSeries(series);
                chartView.getViewport().setScalable(true);
            }
        });
    }
}