package com.example.a121;

import android.app.Application;
import androidx.room.Room;

public class MyApplication extends Application {
    private static AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "app-database").build();
    }

    public static AppDatabase getDatabase() {
        return database;
    }
}