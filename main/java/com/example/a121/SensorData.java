package com.example.a121;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sensor_data")
public class SensorData {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String dataType; // 如 "hr", "br", "st"
    public String value;
    public long timestamp;
}