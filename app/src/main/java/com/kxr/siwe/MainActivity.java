package com.kxr.siwe;

import static android.content.ContentValues.TAG;

import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Button select_date;
    private Button btn_schedule;
    private Button select_to;
    private Button select_from;
    private EditText edit_to;
    private EditText edit_from;
    private ListView participants_list;
    private ListView selected_participants;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final int[] mHourMinute_from = new int[2];
        final int[] mHourMinute_to = new int[2];
        final int[] mDate = new int[3];

        select_date = findViewById(R.id.btn_select_date);
        btn_schedule = findViewById(R.id.btn_schedule);
        select_to = findViewById(R.id.btn_to);
        select_from = findViewById(R.id.btn_from);
        edit_to = findViewById(R.id.edit_to);
        edit_from = findViewById(R.id.edit_from);
        participants_list = findViewById(R.id.list_participants_list);
        selected_participants = findViewById(R.id.list_participants_selected);

        edit_to.setEnabled(false);
        edit_from.setEnabled(false);

        DAOParticipants dao = new DAOParticipants();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this);

        //******************************************************************************************
        //              FETCHING NAMES DATA ON STARTUP
        //******************************************************************************************

        // participants info list stores participants details in Participants class format
        final ArrayList<Participants> participants_info_list = new ArrayList<>();
        final ArrayList<String> participants_info_list_String = new ArrayList<>();
        final ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<String >
                (MainActivity.this, android.R.layout.simple_list_item_1, participants_info_list_String);

        // database setup
        DatabaseReference dbReference = dao.databaseReference.getRef().child("Participants");
        DatabaseReference dbReferenceChild = dao.databaseReference.getRef().child("");

        // fetches the data into the list called mArrayList
        dbReferenceChild.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // clears the list everytime to render new changes
                participants_info_list_String.clear();
                int i = 0;
                for(DataSnapshot dsnap : snapshot.getChildren()) {

                    Participants participants = new Participants(dsnap.getKey(), dsnap.child("name").getValue().
                            toString(),
                            dsnap.child("date").getValue().toString(),
                            dsnap.child("time_from").getValue().toString(),
                            dsnap.child("time_to").getValue().toString());

                    participants_info_list.add(participants);
                    participants_info_list_String.add("Name: " + participants.getName() +
                            "\t\tDate: " + participants.getDate() + "\t\tFrom: " + participants.getTime_from()
                    + "\t\tTo: " + participants.getTime_to());
                    mArrayAdapter.notifyDataSetChanged();
                    ++i;
                }
                // less than 2 participants selected
                if(participants_info_list.size() < 2) {
                    int m_size = participants_info_list.size();
                    Toast.makeText(MainActivity.this, getString(R.string.less_than_2_in_db),
                            Toast.LENGTH_SHORT).show();
                    btn_schedule.setEnabled(false);
                }
                else {
                    btn_schedule.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
        //******************************************************************************************
        //              DATA FETCH ENDS HERE
        //******************************************************************************************

        //******************************************************************************************
        //              SELECT PARTICIPANT FROM LIST
        //******************************************************************************************
        final int[] count_selected = {0};
        ArrayList<Integer> selected_indices = new ArrayList<Integer>(1000);

        // list on UI set adapter from Participants list
        participants_list.setAdapter(mArrayAdapter);
        participants_list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // list for selected participants
        final ArrayList<String> selected_participants_string = new ArrayList<>();
        final ArrayList<Participants> selected_participants_list = new ArrayList<>();
        final ArrayAdapter<String> mArrayAdapterSelected = new ArrayAdapter<String >
                (MainActivity.this, android.R.layout.simple_list_item_1, selected_participants_string);

        // on click listener to save the list of selected participants
        participants_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String record = adapterView.getItemAtPosition(i).toString();
                Toast.makeText(MainActivity.this, record, Toast.LENGTH_SHORT).show();

                // preventing duplicate entries
                if(selected_participants_string.contains(String.valueOf(participants_info_list.get(i).getName())) == false) {
                    selected_participants_list.add(participants_info_list.get(i));
                    selected_participants_string.add(String.valueOf(participants_info_list.get(i).getName()));
                }
                selected_participants.setAdapter(mArrayAdapterSelected);
            }
        });

        // code to prevent scroll fault
        participants_list.setOnTouchListener(new ListView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                // Handle ListView touch events.
                v.onTouchEvent(event);
                return true;
            }
        });

        selected_participants.setOnTouchListener(new ListView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                // Handle ListView touch events.
                v.onTouchEvent(event);
                return true;
            }
        });

        //******************************************************************************************
        //              CLEAR LIST ON CLICKING ELEMENT FROM SELECTED LIST
        //******************************************************************************************

        selected_participants.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selected_participants_list.clear();
                selected_participants_string.clear();
                mArrayAdapterSelected.notifyDataSetChanged();
            }
        });

        //******************************************************************************************
        //              SELECTED PARTICIPANTS LIST OPERATION ENDS HERE
        //******************************************************************************************

        //******************************************************************************************
        //              MULTIPLE CHOICE LISTENER
        //******************************************************************************************

        participants_list.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
                final int checkedCount = participants_list.getCheckedItemCount();
                actionMode.setTitle(checkedCount + " Selected");
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        });
        //******************************************************************************************
        //              MULTICHOICE LISTENER ENDS HERE
        //******************************************************************************************

        //******************************************************************************************
        //              DATE AND TIME PICKERS
        //******************************************************************************************
        // date picker on click
        select_date.setOnClickListener(new View.OnClickListener() {
                                           @Override
                                           public void onClick(View view) {
                                               final Calendar c = Calendar.getInstance();
                                               mDate[0] = c.get(Calendar.YEAR);
                                               mDate[1] = c.get(Calendar.MONTH);
                                               mDate[2] = c.get(Calendar.DAY_OF_MONTH);

                                               DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                                                       new DatePickerDialog.OnDateSetListener() {

                                                           @Override
                                                           public void onDateSet(DatePicker view, int year,
                                                                                 int monthOfYear, int dayOfMonth) {

                                                               mDate[0] = dayOfMonth;
                                                               mDate[1] = monthOfYear;
                                                               mDate[2] = year;
                                                           }
                                                       }, mDate[0], mDate[1], mDate[2]);
                                               datePickerDialog.show();
                                           }
                                       });

        // time picker on click
        //******************************************************************************************
        //              TO TIME
        //******************************************************************************************
        select_to.setOnClickListener(new View.OnClickListener() {
                                            @Override
            public void onClick(View view) {
                                                final Calendar c = Calendar.getInstance();
                                                mHourMinute_to[0] = c.get(Calendar.HOUR_OF_DAY);
                                                mHourMinute_to[1] = c.get(Calendar.MINUTE);

                                                // Launch Time Picker Dialog
                                                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                                                        new TimePickerDialog.OnTimeSetListener() {

                                                            @Override
                                                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                                                  int minute) {

                                                                edit_to.setText(hourOfDay + ":" + minute);
                                                                mHourMinute_to[0] = hourOfDay;
                                                                mHourMinute_to[1] = minute;
                                                            }
                                                        }, mHourMinute_to[0], mHourMinute_to[1], false);
                                                timePickerDialog.show();
                                            }
        });

        //******************************************************************************************
        //              FROM TIME
        //******************************************************************************************
        select_from.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                mHourMinute_from[0] = c.get(Calendar.HOUR_OF_DAY);
                mHourMinute_from[1] = c.get(Calendar.MINUTE);

                // Launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {

                                edit_from.setText(hourOfDay + ":" + minute);
                                mHourMinute_from[0] = hourOfDay;
                                mHourMinute_from[1] = minute;
                            }
                        }, mHourMinute_from[0], mHourMinute_from[1], false);
                timePickerDialog.show();
            }
        });
        //******************************************************************************************
        //              DATE AND TIME PICKERS END HERE
        //******************************************************************************************

        //******************************************************************************************
        //              CREATE READ UPDATE DELETE - BEGINS
        //******************************************************************************************

        /*
        btn_update.setOnClickListener(v -> {
            HashMap<String, Object> hashMap = new HashMap<>();

            hashMap.put("name", edit_name.getText().toString());
            hashMap.put("position", picker_date.getText().toString());

            dao.update("-MpS495J6xIXCFDFpYmD", hashMap).addOnSuccessListener(success -> {
                Toast.makeText(this, "Participant Updated", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(error -> {
                Toast.makeText(this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        btn_delete.setOnClickListener(v -> {
            dao.remove("-MpS495J6xIXCFDFpYmD").addOnSuccessListener(success -> {
                Toast.makeText(this, "Deleted Record", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(fail -> {
                Toast.makeText(this, "Deletion Unsuccessful", Toast.LENGTH_SHORT).show();
            });
        });
        */

        //******************************************************************************************
        //              SCHEDULE BUTTON - CREATE
        //******************************************************************************************
        btn_schedule.setOnClickListener(v -> {
            // check if atleast 2 participants selected
            if(selected_participants_list.size() < 2) {
                Toast.makeText(this, "Less than 2 participants in slot. Retry",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            // check if time slot is valid
            if(mHourMinute_to[0] < mHourMinute_from[0] || mHourMinute_to[1] < mHourMinute_from[1]) {
                Toast.makeText(this, "Invalid time slot!", Toast.LENGTH_SHORT).show();
                return;
            }

            // if slot already scheduled || overlapping slot
            //**************************************************************************************

            // for each time slot check if the selected slot is overlapping anywhere
            ArrayList<String> selectedKeys = new ArrayList<>();
            // storing keys of selected participants
            for(int i = 0; i < selected_participants_list.size(); ++i) {
                selectedKeys.add(selected_participants_list.get(i).getMyKey());
            }

            //**************************************************************************************
            // for each participant check if slot matches
            int flag = 0;
            for(int i = 0; i < participants_info_list.size(); ++i) {
                // see if the elements from the selected list are not being checked of overlapping
                // because they are being rescheduled
                flag = 0;
                for(int j = 0; j < selectedKeys.size(); ++j) {
                    if(selectedKeys.get(j) == participants_info_list.get(i).getMyKey()) {
                        flag = 1;
                        break;
                    }
                }

                // if key matches, continue with the next iteration
                if(flag == 1) { continue; }

                // hours and minutes && from and to of each participant
                //----------------------------------------------------------------------------------
                int hour_from, hour_to, mins_from, mins_to;
                //----------------------------------------------------------------------------------
                String[] time_from = participants_info_list.get(i).getTime_from().
                        split(String.valueOf('-'));
                String[] time_to = participants_info_list.get(i).getTime_to().
                        split(String.valueOf('-'));
                //----------------------------------------------------------------------------------
                hour_from = Integer.valueOf(time_from[0]);
                mins_from = Integer.valueOf(time_from[1]);
                hour_to = Integer.valueOf(time_to[0]);
                mins_to = Integer.valueOf(time_to[1]);
                //----------------------------------------------------------------------------------

                Log.d(TAG, "entry: " + String.valueOf(hour_from) + " " + String.valueOf(mins_from) + " " + String.valueOf(hour_to) + " " + String.valueOf(mins_to));

                // logic for preoccupied time slots
                if(mHourMinute_from[0] <= hour_from && mHourMinute_from[1] <= mins_from &&
                    mHourMinute_to[0] <= hour_to && mHourMinute_to[1] <= hour_to) {

                    Toast.makeText(this, "Overlap in time schedule.", Toast.LENGTH_SHORT).show();
                    return;
                }
                else if(mHourMinute_from[0] <= hour_from && mHourMinute_from[1] <= mins_from &&
                        mHourMinute_to[0] >= hour_to && mHourMinute_to[1] >= hour_to) {

                    Toast.makeText(this, "Overlap in time schedule.", Toast.LENGTH_SHORT).show();
                    return;
                }
                else if(mHourMinute_from[0] >= hour_from && mHourMinute_from[1] >= mins_from &&
                        mHourMinute_from[0] <= hour_to && mHourMinute_from[1] <= mins_to) {

                    Toast.makeText(this, "Overlap in time schedule.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // maybe also return where the overlapping slot resides
            //**************************************************************************************

            //              end of finding overlap

            //**************************************************************************************
            // creating strings for new date and time
            String date = String.valueOf(mDate[0]) + '-' + String.valueOf(mDate[1]) + '-' +
                    String.valueOf(mDate[2]);
            String time_from = String.valueOf(mHourMinute_from[0]) + '-'
                    + String.valueOf(mHourMinute_from[1]);
            String time_to = String.valueOf(mHourMinute_to[0]) + '-'
                    + String.valueOf(mHourMinute_to[1]);

            HashMap<String, Object> hashMap = new HashMap<>();

            for(int i = 0; i < selected_participants_list.size(); ++i) {
                String key = selected_participants_list.get(i).getMyKey();

                hashMap.put("time_from", String.valueOf(mHourMinute_from[0])
                        + "-" + String.valueOf(mHourMinute_from[1]));
                hashMap.put("time_to", String.valueOf(mHourMinute_to[0])
                        + "-" + String.valueOf(mHourMinute_to[1]));

                dao.update(selected_participants_list.get(i).getMyKey(), hashMap).addOnSuccessListener(success -> {
                    Toast.makeText(this, "Participant Updated", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(error -> {
                    Toast.makeText(this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
            /*
            hashMap.put("name", edit_name.getText().toString());
            hashMap.put("position", picker_date.getText().toString());

            dao.update("-MpS495J6xIXCFDFpYmD", hashMap).addOnSuccessListener(success -> {
                Toast.makeText(this, "Participant Updated", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(error -> {
                Toast.makeText(this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            });
             */

            // send to database
            /*
            Participants participants = new Participants(name, date, time_from, time_to);
            dao.add(participants).addOnSuccessListener(sucess -> {
                Toast.makeText(this, "Interview Scheduled", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(error -> {
                Toast.makeText(this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            });
             */
            count_selected[0] = 0;
        });
        //******************************************************************************************
        //              CRUD ENDS HERE
        //******************************************************************************************
    }
}