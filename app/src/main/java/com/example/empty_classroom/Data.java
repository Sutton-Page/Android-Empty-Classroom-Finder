package com.example.empty_classroom;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.RoomDatabase;

import java.util.List;

@Entity(tableName = "data")
public class Data {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    public Integer id;

    @ColumnInfo(name = "location")
    public String location;

    @ColumnInfo(name = "days")
    public String days;

    @ColumnInfo(name = "start")
    public String start;

    @ColumnInfo(name = "end")
    public String end;

    @ColumnInfo(name = "room")
    public String room;
}
