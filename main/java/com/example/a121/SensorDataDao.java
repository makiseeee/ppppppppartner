package com.example.a121;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SensorDataDao {
    @Insert
    void insert(SensorData data);

    @Query("SELECT * FROM sensor_data WHERE dataType = :type ORDER BY timestamp DESC")
    LiveData<List<SensorData>> getDataByType(String type);

    @Query("SELECT MAX(value) FROM sensor_data WHERE dataType = :type")
    LiveData<String> getMaxValue(String type);

    @Query("SELECT MIN(value) FROM sensor_data WHERE dataType = :type")
    LiveData<String> getMinValue(String type);
}