package com.kxr.siwe;

import static android.content.ContentValues.TAG;

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
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
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

public class MainActivity extends AppCompatActivity {

    private Button select_date;
    private Button btn_schedule;
    private Button select_to;
    private Button select_from;
    private EditText edit_to;
    private EditText edit_from;
    private ListView participants_list;

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

//******        // variables to be deleted
        final String[] global_name = new String[1000];
        final String[] global_from = new String[1000];
        final String[] global_to = new String[1000];
        final String[] global_date = new String[1000];

        // fetches the data into the list called mArrayList
        dbReferenceChild.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // clears the list everytime to render new changes
                participants_info_list.clear();
                int i = 0;
                for(DataSnapshot dsnap : snapshot.getChildren()) {
//*******
                    /*
                    global_name[i] = dsnap.child("name").getValue().toString();
                    global_date[i] = dsnap.child("date").getValue().toString();
                    global_from[i] = dsnap.child("time_from").getValue().toString();
                    global_to[i] = dsnap.child("time_to").getValue().toString();
                    */

                    Participants participants = new Participants(dsnap.child("name").getValue().
                            toString(),
                            dsnap.child("date").getValue().toString(),
                            dsnap.child("time_from").getValue().toString(),
                            dsnap.child("time_to").getValue().toString());

                    participants_info_list.add(participants);
                    participants_info_list_String.add("Name: " + participants.getName() +
                            " Date: " + participants.getDate() + " From: " + participants.getTime_from()
                    + " To: " + participants.getTime_to());
                    mArrayAdapter.notifyDataSetChanged();
                    ++i;
                }
                // less than 2 participants selected
                if(i < 2) {
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
        //              FETCH THE NAME ON THE LISTVIEW ON SELECTION
        //******************************************************************************************
        final int[] count_selected = {0};
        ArrayList<Integer> selected_indices = new ArrayList<Integer>(1000);

        // list on UI set adapter from Participants list
        participants_list.setAdapter(mArrayAdapter);
        participants_list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // on click listener
        participants_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String name = adapterView.getItemAtPosition(i).toString();
                Toast.makeText(MainActivity.this, name, Toast.LENGTH_SHORT).show();

                selected_indices.add(i);
                Log.d(TAG, global_name[i]);
                ++count_selected[0];
            }
        });
        //******************************************************************************************
        //              FETCH THE NAME ENDS HERE
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
            if(count_selected[0] < 2) {
                Toast.makeText(this, "Less than 2 participants in slot. Retry",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            // check if time slot is valid
            if(mHourMinute_to[0] < mHourMinute_from[0] || mHourMinute_to[1] < mHourMinute_from[1]) {
                Toast.makeText(this, "Invalid time slot!", Toast.LENGTH_SHORT).show();
                return;
            }

            // if slot already scheduled


            // creating strings for date and time
            String date = String.valueOf(mDate[0]) + '-' + String.valueOf(mDate[1]) + '-' +
                    String.valueOf(mDate[2]);
            String time_from = String.valueOf(mHourMinute_from[0]) + '-'
                    + String.valueOf(mHourMinute_from[1]);
            String time_to = String.valueOf(mHourMinute_to[0]) + '-'
                    + String.valueOf(mHourMinute_to[1]);

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