package com.example.a121;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class TempFragment extends Fragment {
    private TextView tvRoomTemp, tvBodyTemp, tvRoomTempRange, tvBodyTempRange, tvRoomTempMaxMin, tvBodyTempMaxMin;
    private GraphView chartView;
    private SharedViewModel sharedViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_temp, container, false);

        tvRoomTemp = view.findViewById(R.id.tv_room_temp);
        tvBodyTemp = view.findViewById(R.id.tv_body_temp);
        tvRoomTempRange = view.findViewById(R.id.tv_room_temp_range);
        tvBodyTempRange = view.findViewById(R.id.tv_body_temp_range);
        tvRoomTempMaxMin = view.findViewById(R.id.tv_room_temp_max_min);
        tvBodyTempMaxMin = view.findViewById(R.id.tv_body_temp_max_min);
        chartView = view.findViewById(R.id.chart_temp);

        tvRoomTempRange.setText("最适宜26℃");
        tvBodyTempRange.setText("参考值: 24-34 °C");

        observeLiveData();     // 实时更新温度文本
        loadHistoryData();     // 获取历史最高/最低
        setupChart();          // 绘图

        return view;
    }

    private void observeLiveData() {
        sharedViewModel.getRoomTemp().observe(getViewLifecycleOwner(), value -> {
            if (value != null) {
                try {
                    double val = Double.parseDouble(value) / 10;
                    tvRoomTemp.setText("室温: " + val + " °C");
                } catch (NumberFormatException e) {
                    Log.e("TempFragment", "室温数据格式错误: " + value);
                }
            }
        });

        sharedViewModel.getBodyTemp().observe(getViewLifecycleOwner(), value -> {
            if (value != null) {
                try {
                    double val = Double.parseDouble(value) / 10;
                    tvBodyTemp.setText("体表温度: " + val + " °C");
                } catch (NumberFormatException e) {
                    Log.e("TempFragment", "体温数据格式错误: " + value);
                }
            }
        });
    }

    private void loadHistoryData() {
        LiveData<String> roomTempMax = MyApplication.getDatabase().sensorDataDao().getMaxValue("rt");
        LiveData<String> roomTempMin = MyApplication.getDatabase().sensorDataDao().getMinValue("rt");
        roomTempMax.observe(getViewLifecycleOwner(), max ->
                roomTempMin.observe(getViewLifecycleOwner(), min -> {
                    String maxStr = (max != null) ? String.format("%.1f", Float.parseFloat(max) / 10f) : "--";
                    String minStr = (min != null) ? String.format("%.1f", Float.parseFloat(min) / 10f) : "--";
                    tvRoomTempMaxMin.setText("历史最高/最低: " + maxStr + " / " + minStr);
                })
        );

        LiveData<String> bodyTempMax = MyApplication.getDatabase().sensorDataDao().getMaxValue("bt");
        LiveData<String> bodyTempMin = MyApplication.getDatabase().sensorDataDao().getMinValue("bt");
        bodyTempMax.observe(getViewLifecycleOwner(), max ->
                bodyTempMin.observe(getViewLifecycleOwner(), min -> {
                    String maxStr = (max != null) ? String.format("%.1f", Float.parseFloat(max) / 10f) : "--";
                    String minStr = (min != null) ? String.format("%.1f", Float.parseFloat(min) / 10f) : "--";
                    tvBodyTempMaxMin.setText("历史最高/最低: " + maxStr + " / " + minStr);
                })
        );
    }


    private void setupChart() {
        LiveData<List<SensorData>> tempData = MyApplication.getDatabase().sensorDataDao().getDataByType("bt");
        tempData.observe(getViewLifecycleOwner(), data -> {
            if (data != null && !data.isEmpty()) {
                List<DataPoint> points = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    try {
                        double value = Double.parseDouble(data.get(i).value) / 10;
                        points.add(new DataPoint(i, value));
                    } catch (NumberFormatException e) {
                        Log.e("TempFragment", "无效的体温数据: " + data.get(i).value);
                    }
                }

                chartView.removeAllSeries(); // 防止重复叠加
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(points.toArray(new DataPoint[0]));
                series.setColor(getResources().getColor(android.R.color.holo_orange_light));
                chartView.addSeries(series);

                chartView.getViewport().setMinY(30);
                chartView.getViewport().setMaxY(42);
                chartView.getViewport().setMinX(0);
                chartView.getViewport().setMaxX(points.size());

                chartView.getViewport().setScalable(true);
                chartView.getViewport().setScrollable(true);
                chartView.setTitle("体温趋势图");
            } else {
                Log.d("TempFragment", "图表数据为空");
            }
        });
    }
}
