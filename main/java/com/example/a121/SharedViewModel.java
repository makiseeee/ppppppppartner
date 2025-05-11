package com.example.a121;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<String> heartRate = new MutableLiveData<>();
    private final MutableLiveData<String> breathRate = new MutableLiveData<>();
    private final MutableLiveData<String> sleepStatus = new MutableLiveData<>();
    private final MutableLiveData<String> roomTemp = new MutableLiveData<>();
    private final MutableLiveData<String> peopleDistance = new MutableLiveData<>();
    private final MutableLiveData<String> moveLevel = new MutableLiveData<>();
    private final MutableLiveData<String> bodyTemp = new MutableLiveData<>();

    public void setHeartRate(String value) { heartRate.postValue(value); } // 使用 postValue 替代 setValue
    public LiveData<String> getHeartRate() { return heartRate; }

    public void setBreathRate(String value) { breathRate.postValue(value); } // 使用 postValue 替代 setValue
    public LiveData<String> getBreathRate() { return breathRate; }

    public void setSleepStatus(String value) { sleepStatus.postValue(value); } // 使用 postValue 替代 setValue
    public LiveData<String> getSleepStatus() { return sleepStatus; }

    public void setRoomTemp(String value) { roomTemp.postValue(value); } // 使用 postValue 替代 setValue
    public LiveData<String> getRoomTemp() { return roomTemp; }

    public void setBodyTemp(String value) { bodyTemp.postValue(value); } // 使用 postValue 替代 setValue
    public LiveData<String> getBodyTemp() { return bodyTemp; }

    public void setPeopleDistance(String value) { peopleDistance.postValue(value); } // 使用 postValue 替代 setValue
    public LiveData<String> getPeopleDistance() { return peopleDistance; }

    public void setMoveLevel(String value) { moveLevel.postValue(value); } // 使用 postValue 替代 setValue
    public LiveData<String> getMoveLevel() { return moveLevel; }
}
