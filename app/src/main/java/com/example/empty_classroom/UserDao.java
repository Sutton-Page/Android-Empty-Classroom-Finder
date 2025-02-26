package com.example.empty_classroom;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.Date;
import java.util.List;

@Dao
public interface UserDao {
    

    @Query("SELECT * FROM data")
    List<Data> getAll();


    @Query("SELECT DISTINCT room from data WHERE location = :building")
    List<String> getUniqueRooms(String building);

    @Query("SELECT DISTINCT location FROM data")
    List<String> getBuildings();

    @Query("SELECT * from data where location = :building")
    public List<Data> selectClasses(String building);

    @Query("SELECT * FROM data WHERE location = :building AND days IN (:query)")
    public List<Data> selectClasses(String building, List<String> query);


}
