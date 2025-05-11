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

public class BreathFragment extends Fragment {
    private TextView tvBreathRate, tvBreathRateRange, tvBreathRateMaxMin;
    private GraphView chartView;
    private SharedViewModel sharedViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_breath, container, false);

        tvBreathRate = view.findViewById(R.id.tv_breath_rate);
        tvBreathRateRange = view.findViewById(R.id.tv_breath_rate_range);
        tvBreathRateMaxMin = view.findViewById(R.id.tv_breath_rate_max_min);
        chartView = view.findViewById(R.id.chart_breath);

        tvBreathRateRange.setText("参考值: 12-20 次/分钟");

        observeLiveData();
        loadHistoryData();
        setupChart();

        return view;
    }

    private void observeLiveData() {
        sharedViewModel.getBreathRate().observe(getViewLifecycleOwner(), value -> {
            if (value != null) {
                try {
                    int breath = Integer.parseInt(value);
                    tvBreathRate.setText("呼吸频率: " + breath + " 次/分钟");
                } catch (NumberFormatException e) {
                    Log.e("BreathFragment", "呼吸频率格式错误: " + value);
                }
            }
        });
    }

    private void loadHistoryData() {
        LiveData<String> breathMax = MyApplication.getDatabase().sensorDataDao().getMaxValue("br");
        LiveData<String> breathMin = MyApplication.getDatabase().sensorDataDao().getMinValue("br");
        breathMax.observe(getViewLifecycleOwner(), max ->
                breathMin.observe(getViewLifecycleOwner(), min -> {
                    tvBreathRateMaxMin.setText("历史最高/最低: " +
                            (max != null ? max : "--") + " / " +
                            (min != null ? min : "--"));
                })
        );
    }

    private void setupChart() {
        LiveData<List<SensorData>> breathData = MyApplication.getDatabase().sensorDataDao().getDataByType("br");
        breathData.observe(getViewLifecycleOwner(), data -> {
            if (data != null && !data.isEmpty()) {
                Log.d("BreathFragment", "加载呼吸数据数量: " + data.size());
                List<DataPoint> points = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    try {
                        double value = Double.parseDouble(data.get(i).value);
                        points.add(new DataPoint(i, value));
                    } catch (NumberFormatException e) {
                        Log.e("BreathFragment", "无效的呼吸数据: " + data.get(i).value);
                    }
                }

                chartView.removeAllSeries(); // 防止图表重复
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(points.toArray(new DataPoint[0]));
                series.setColor(getResources().getColor(android.R.color.holo_green_light));
                series.setTitle("呼吸频率");

                chartView.addSeries(series);
                chartView.getViewport().setScalable(true);
                chartView.getViewport().setScrollable(true);
                chartView.setTitle("呼吸趋势图");
            } else {
                Log.d("BreathFragment", "呼吸图表数据为空");
            }
        });
    }
}
