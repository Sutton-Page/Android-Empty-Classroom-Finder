package com.example.empty_classroom;

import android.app.TimePickerDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.room.Room;

import com.example.empty_classroom.databinding.ActivityMainBinding;
import com.google.android.material.chip.Chip;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;

    private AppDatabase db;

    private Handler handle;

    private ArrayAdapter<String> adapter;
    private ArrayList<String> dynamicArray;

    private CustomAdapter roomAdapter;
    private ArrayList<String> roomData;

    public boolean doTimesOverlap(int start1, int end1, int start2, int end2) {
        return start1 < end2 && start2 < end1;
    }

    public List<Data> filterData(List<Data> raw, String days){

        List<Data> keep = new ArrayList<>();

        for(int i = 0; i < raw.size(); i++){

            Data item = raw.get(i);

            if(days.contains(item.days)){

                keep.add(item);
            }
        }

        return keep;
    }

    public void queryClasses(String days, String building){


        Thread th = new Thread(() -> {

            if(db != null){


                List<Data> buildingClasses = db.userDao().selectClasses(building);
                List<String> allRooms = db.userDao().getUniqueRooms(building);

                Set<String> allRoomSet = new HashSet<>(allRooms);

                char[] sort = days.toCharArray();
                Arrays.sort(sort);
                String sorted = new String(sort);

                List<Data> limited = this.filterData(buildingClasses,sorted);


                String available = "";

                int userTimeStart = (startHour * 100) + startMinute;
                int userTimeEnd = (endHour * 100) + endMinute;

                Set<String> conflictingRooms = new HashSet<String>();


                for(int i = 0; i < limited.size(); i++){

                    Data item = limited.get(i);

                    String start = item.start;
                    String end = item.end;

                    String[] startSplit = start.split(":");
                    int classStart = (Integer.parseInt(startSplit[0]) * 100) + Integer.parseInt(startSplit[1]);

                    String[] endSplit = end.split(":");
                    int classEnd = (Integer.parseInt(endSplit[0]) * 100) + Integer.parseInt(endSplit[1]);

                    boolean overlap = this.doTimesOverlap(userTimeStart,userTimeEnd,classStart,classEnd);

                    if(overlap){

                        conflictingRooms.add(item.room);
                    }

                }

                allRoomSet.removeAll(conflictingRooms);


                ArrayList<String> finalData = new ArrayList<>(allRoomSet);
                Collections.sort(finalData);

                handle.post(() -> {

                    roomData.clear();
                    roomData.addAll(finalData);
                    roomAdapter.notifyDataSetChanged();
                });


            }
        });

        th.start();
    }
    private String getSelectedChips(){

        List<Integer> ids = binding.weekSelect.getCheckedChipIds();

        String selected = "";

        for(int i = 0; i < ids.size(); i++){

            Chip chip = binding.weekSelect.findViewById(ids.get(i));
            selected+= chip.getText();
        }

        //binding.testDisplay.setText(selected);

        return selected;

    }
    private void initalizeDb(){

        Thread th = new Thread(() -> {

            AppDatabase instance = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "data.db")
                    .createFromAsset("data.db")
                    .build();

            List<String> result = instance.userDao().getBuildings();

            handle.post(() -> {

                db = instance;
                dynamicArray.clear();
                dynamicArray.addAll(result);
                adapter.notifyDataSetChanged();

            });

        });

        th.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        handle = new Handler(Looper.getMainLooper());

        this.initalizeDb();


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        roomData = new ArrayList<>();
        roomAdapter = new CustomAdapter(this.roomData);
        binding.displayRoom.setAdapter(roomAdapter);
        binding.displayRoom.setLayoutManager(new GridLayoutManager(this,3));

        // Create a dynamic array for Spinner items
         dynamicArray = new ArrayList<>();


        // Create an ArrayAdapter using the dynamic array and a default spinner layout
         adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dynamicArray);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the Spinner
        binding.buildingSelect.setAdapter(adapter);


        binding.submit.setOnClickListener(l -> {

            String days = this.getSelectedChips();

            boolean inputIsValid = true;

            if(days.length() == 0){

                inputIsValid = false;
                Toast.makeText(this,"Please select at least one day!",Toast.LENGTH_LONG).show();
            }

            if(startHour == endHour && startMinute == endMinute){

                inputIsValid = false;
                Toast.makeText(this,"Start and end time cannot be the same!",Toast.LENGTH_LONG).show();
            }

            if(startHour > endHour){

                inputIsValid = false;
                Toast.makeText(this,"End time cannot be before start time!",Toast.LENGTH_LONG).show();
            }

            if(startHour == endHour){

                if(startMinute > endMinute){
                    inputIsValid = false;
                    Toast.makeText(this,"End time cannot be before start time!",Toast.LENGTH_LONG).show();
                }
            }


            if(inputIsValid){

                roomData.clear();
                roomAdapter.notifyDataSetChanged();

                // gets selected building
                String building = binding.buildingSelect.getSelectedItem().toString();

                this.queryClasses(days,building);

            }



        });

        binding.endSelect.setOnClickListener(l -> {

            final Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // TimePickerDialog
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    MainActivity.this,
                    (TimePicker view1, int hourOfDay, int minuteOfHour) -> {
                        // Save the selected time to variables
                        endHour = hourOfDay;
                        endMinute= minuteOfHour;

                        // Optionally, update the button text or do something with the time
                        String formattedTime = String.format("%02d:%02d", endHour, endMinute);
                        //timePickerButton.setText(formattedTime); // Example: Update button text
                        String updatedString = "End Time: " + formattedTime;

                        binding.eTime.setText(updatedString);
                    },
                    hour, minute, false); // true for 24-hour format, false for AM/PM format

            timePickerDialog.show();
        });

        binding.startSelect.setOnClickListener(l -> {

            final Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // TimePickerDialog
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    MainActivity.this,
                    (TimePicker view1, int hourOfDay, int minuteOfHour) -> {
                        // Save the selected time to variables
                        startHour = hourOfDay;
                        startMinute = minuteOfHour;

                        // Optionally, update the button text or do something with the time
                        String formattedTime = String.format("%02d:%02d", startHour, startMinute);
                        //timePickerButton.setText(formattedTime); // Example: Update button text
                        String updatedString = "Start Time: " + formattedTime;
                        binding.sTime.setText(updatedString);
                    },
                    hour, minute, false); // true for 24-hour format, false for AM/PM format

            timePickerDialog.show();
        });


    }
}