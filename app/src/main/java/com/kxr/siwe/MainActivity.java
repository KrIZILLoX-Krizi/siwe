package com.kxr.siwe;

import static android.content.ContentValues.TAG;
import static android.provider.AlarmClock.EXTRA_MESSAGE;

import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.icu.util.Calendar;
import android.net.Uri;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private FirebaseAuth mAuth;

    private static final int SECOND_ACTIVITY_REQUEST_CODE = 0;

    @RequiresApi(api = Build.VERSION_CODES.N)

    @Override
    public void onStart() {

        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null) {
            Toast.makeText(MainActivity.this, "Log-in successful!",
                    Toast.LENGTH_SHORT).show();
        }
        else {
            Intent intent = new Intent(MainActivity.this, Authentication.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //------------------------------------------------------------------------------------------
        //                                      AUTHENTICATION
        //------------------------------------------------------------------------------------------
        mAuth = FirebaseAuth.getInstance();
        //------------------------------------------------------------------------------------------

        //------------------------------------------------------------------------------------------
        // stores the current date & time from the pickers
        //------------------------------------------------------------------------------------------
        final int[] mHourMinute_from = new int[2];
        final int[] mHourMinute_to = new int[2];
        final int[] mDate = new int[3];
        //------------------------------------------------------------------------------------------

        //------------------------------------------------------------------------------------------
        // GUI components as objects
        //------------------------------------------------------------------------------------------
        select_date = findViewById(R.id.btn_select_date);
        btn_schedule = findViewById(R.id.btn_schedule);
        select_to = findViewById(R.id.btn_to);
        select_from = findViewById(R.id.btn_from);
        edit_to = findViewById(R.id.edit_to);
        edit_from = findViewById(R.id.edit_from);
        participants_list = findViewById(R.id.list_participants_list);
        selected_participants = findViewById(R.id.list_participants_selected);
        //------------------------------------------------------------------------------------------

        // disabling edit on edit texts for the time display on selection
        edit_to.setEnabled(false);
        edit_from.setEnabled(false);

        //------------------------------------------------------------------------------------------
        // database setups
        //------------------------------------------------------------------------------------------
        DAOParticipants dao = new DAOParticipants();
        DatabaseReference dbReference = dao.databaseReference.getRef().child("Participants");
        DatabaseReference dbReferenceChild = dao.databaseReference.getRef().child("");
        //------------------------------------------------------------------------------------------

        //******************************************************************************************
        //              FETCHING DATA ON STARTUP && POPULATING THE LIST VIEW
        //******************************************************************************************

        // participants info list stores participants details in Participants class format
        // storing the same as a list of string as well
        // && an adapter for the same
        //------------------------------------------------------------------------------------------
        final ArrayList<Participants> participants_info_list = new ArrayList<>();
        final ArrayList<String> participants_info_list_String = new ArrayList<>();
        final ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<String>
                (MainActivity.this, android.R.layout.simple_list_item_1,
                        participants_info_list_String);
        //------------------------------------------------------------------------------------------

        // fetches the data into the list called mArrayList
        //------------------------------------------------------------------------------------------
        dbReferenceChild.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // clears the list everytime to render new changes
                //----------------------------------------------------------------------------------
                participants_info_list_String.clear();
                int i = 0;
                for (DataSnapshot dsnap : snapshot.getChildren()) {

                    Participants participants = new Participants(dsnap.getKey(),
                            dsnap.child("name").getValue().toString(),
                            dsnap.child("email").getValue().toString(),
                            dsnap.child("date").getValue().toString(),
                            dsnap.child("time_from").getValue().toString(),
                            dsnap.child("time_to").getValue().toString());

                    participants_info_list.add(participants);
                    participants_info_list_String.add("Name: " + participants.getName() +
                            "\t\tDate: " + participants.getDate() + "\t\tFrom: " + participants.
                            getTime_from() + "\t\tTo: " + participants.getTime_to());
                    mArrayAdapter.notifyDataSetChanged();
                    ++i;
                }
                // less than 2 participants selected
                if (participants_info_list.size() < 2) {
                    int m_size = participants_info_list.size();
                    Toast.makeText(MainActivity.this, getString(R.string.less_than_2_in_db),
                            Toast.LENGTH_SHORT).show();
                    btn_schedule.setEnabled(false);
                } else {
                    btn_schedule.setEnabled(true);
                }
                //----------------------------------------------------------------------------------
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        // list on UI set adapter from Participants list
        //------------------------------------------------------------------------------------------
        participants_list.setAdapter(mArrayAdapter);
        participants_list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        //------------------------------------------------------------------------------------------

        //******************************************************************************************
        //              DATA FETCH ENDS HERE
        //******************************************************************************************

        //------------------------------------------------------------------------------------------

        //******************************************************************************************
        //              UPON SELECTING PARTICIPANT FROM LIST OF ALL
        //******************************************************************************************

        // list for selected participants
        //------------------------------------------------------------------------------------------
        final ArrayList<String> selected_participants_string = new ArrayList<>();
        final ArrayList<Participants> selected_participants_list = new ArrayList<>();
        final ArrayAdapter<String> mArrayAdapterSelected = new ArrayAdapter<String>
                (MainActivity.this, android.R.layout.simple_list_item_1,
                        selected_participants_string);
        //------------------------------------------------------------------------------------------

        // on click listener to save the list of selected participants
        //------------------------------------------------------------------------------------------
        participants_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String record = adapterView.getItemAtPosition(i).toString();
                Toast.makeText(MainActivity.this, record, Toast.LENGTH_SHORT).show();

                // preventing duplicate entries
                if (selected_participants_string.contains(String.valueOf(participants_info_list.
                        get(i).getName())) == false) {
                    selected_participants_list.add(participants_info_list.get(i));
                    selected_participants_string.add(String.valueOf(participants_info_list.get(i).
                            getName()));
                }
                selected_participants.setAdapter(mArrayAdapterSelected);
            }
        });
        //------------------------------------------------------------------------------------------

        //******************************************************************************************
        //                 CODE TO PREVENT SCROLL FAULT
        //******************************************************************************************

        // scroll disabled on the all participants list touched
        //------------------------------------------------------------------------------------------
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
        //------------------------------------------------------------------------------------------

        // scroll disabled on the selected participants list on touch
        //------------------------------------------------------------------------------------------
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
        //------------------------------------------------------------------------------------------

        //******************************************************************************************
        //              SCROLL ISSUE RESOLUTION ENDS HERE
        //******************************************************************************************

        // clearing the selected participants list on clicking any element of the list
        //------------------------------------------------------------------------------------------
        selected_participants.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selected_participants_list.clear();
                selected_participants_string.clear();
                mArrayAdapterSelected.notifyDataSetChanged();
            }
        });
        //------------------------------------------------------------------------------------------

        //******************************************************************************************
        //              SELECTED PARTICIPANTS LIST OPERATION ENDS HERE
        //******************************************************************************************

        //******************************************************************************************
        //              MULTIPLE CHOICE LISTENER
        //******************************************************************************************

        // allowing multiple selections from the participants list
        //------------------------------------------------------------------------------------------
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
        //------------------------------------------------------------------------------------------

        //******************************************************************************************
        //              MULTIPLE CHOICE LISTENER ENDS HERE
        //******************************************************************************************

        //******************************************************************************************
        //              DATE AND TIME PICKERS
        //******************************************************************************************

        // date picker pop up on button click
        //------------------------------------------------------------------------------------------
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
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                                mDate[0] = dayOfMonth;
                                mDate[1] = monthOfYear;
                                mDate[2] = year;
                            }
                        }, mDate[0], mDate[1], mDate[2]);

                datePickerDialog.show();
            }
        });
        //------------------------------------------------------------------------------------------

        // time picker pop up on button click - end time
        //------------------------------------------------------------------------------------------
        select_to.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();

                mHourMinute_to[0] = c.get(Calendar.HOUR_OF_DAY);
                mHourMinute_to[1] = c.get(Calendar.MINUTE);

                // launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                                edit_to.setText(hourOfDay + ":" + minute);
                                mHourMinute_to[0] = hourOfDay;
                                mHourMinute_to[1] = minute;
                            }
                        }, mHourMinute_to[0], mHourMinute_to[1], false);

                timePickerDialog.show();
            }
        });
        //------------------------------------------------------------------------------------------

        // time picker pop up on button click - end time
        //------------------------------------------------------------------------------------------
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
        //------------------------------------------------------------------------------------------

        //******************************************************************************************
        //              DATE AND TIME PICKERS END HERE
        //******************************************************************************************

        //------------------------------------------------------------------------------------------

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

            // check if at least 2 participants selected
            //--------------------------------------------------------------------------------------
            if (selected_participants_list.size() < 2) {
                Toast.makeText(this, "Less than 2 participants in slot. Retry",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // check if time slot is valid
            //--------------------------------------------------------------------------------------
            if (mHourMinute_to[0] < mHourMinute_from[0] || mHourMinute_to[1] < mHourMinute_from[1]) {
                Toast.makeText(this, "Invalid time slot!", Toast.LENGTH_SHORT).show();
                return;
            }

            // if slot already scheduled || overlapping slot
            //--------------------------------------------------------------------------------------
            // for each time slot check if the selected slot is overlapping anywhere
            ArrayList<String> selectedKeys = new ArrayList<>();

            // storing keys of selected participants
            for (int i = 0; i < selected_participants_list.size(); ++i) {
                selectedKeys.add(selected_participants_list.get(i).getMyKey());
            }

            // checking for slot overlap
            //--------------------------------------------------------------------------------------
            int flag = 0;
            for (int i = 0; i < participants_info_list.size(); ++i) {
                // see if the elements from the selected list are not being checked of overlapping
                // because they are being rescheduled
                flag = 0;
                for (int j = 0; j < selectedKeys.size(); ++j) {
                    if (selectedKeys.get(j) == participants_info_list.get(i).getMyKey()) {
                        flag = 1;
                        break;
                    }
                }

                // if key matches, continue with the next iteration
                if (flag == 1) {
                    continue;
                }

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

                Log.d(TAG, "entry: " + String.valueOf(hour_from) + " " + String.valueOf(mins_from)
                        + " " + String.valueOf(hour_to) + " " + String.valueOf(mins_to));

                // logic for preoccupied time slots
                if (mHourMinute_from[0] <= hour_from && mHourMinute_from[1] <= mins_from &&
                        mHourMinute_to[0] <= hour_to && mHourMinute_to[1] <= hour_to) {

                    Toast.makeText(this, "Overlap in time schedule.", Toast.LENGTH_SHORT)
                            .show();
                    return;
                } else if (mHourMinute_from[0] <= hour_from && mHourMinute_from[1] <= mins_from &&
                        mHourMinute_to[0] >= hour_to && mHourMinute_to[1] >= hour_to) {

                    Toast.makeText(this, "Overlap in time schedule.", Toast.LENGTH_SHORT)
                            .show();
                    return;
                } else if (mHourMinute_from[0] >= hour_from && mHourMinute_from[1] >= mins_from &&
                        mHourMinute_from[0] < hour_to && mHourMinute_from[1] < mins_to) {

                    Toast.makeText(this, "Overlap in time schedule.", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
            }
            // finding overlap - ends here
            //--------------------------------------------------------------------------------------

            // maybe also return where the overlapping slot resides
            //--------------------------------------------------------------------------------------

            // hashmap to push to firebase
            HashMap<String, Object> hashMap = new HashMap<>();

            // pushing the updated records
            for (int i = 0; i < selected_participants_list.size(); ++i) {
                String key = selected_participants_list.get(i).getMyKey();

                hashMap.put("time_from", String.valueOf(mHourMinute_from[0])
                        + "-" + String.valueOf(mHourMinute_from[1]));
                hashMap.put("time_to", String.valueOf(mHourMinute_to[0])
                        + "-" + String.valueOf(mHourMinute_to[1]));

                dao.update(selected_participants_list.get(i).getMyKey(), hashMap).
                        addOnSuccessListener(success -> {
                            Toast.makeText(this, "Participant Updated", Toast.LENGTH_SHORT).
                                    show();
                        }).addOnFailureListener(error -> {
                    Toast.makeText(this, "" + error.getMessage(), Toast.LENGTH_SHORT).
                            show();
                });
            }

            // notify the adapter and clear the lists of selected participants
            selected_participants_list.clear();
            selected_participants_string.clear();
            mArrayAdapterSelected.notifyDataSetChanged();
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
        });
        //******************************************************************************************
        //              CRUD ENDS HERE
        //******************************************************************************************

        //******************************************************************************************
        //              EMAIL VIA INTENT
        //******************************************************************************************

        /*
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.setType("text/plain");

            emailIntent.putExtra(Intent.EXTRA_EMAIL, selected_participants_list.get(0).getEmail());
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, );
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Email message goes here");

            try {
                startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                finish();
                Log.i("Finished sending email...", "");
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(MainActivity.this,
                "There is no email client installed.", Toast.LENGTH_SHORT).show();
            }

         */

        //******************************************************************************************
        //              EMAIL FUNCTIONALITY ENDS HERE
        //******************************************************************************************
    }
}



























