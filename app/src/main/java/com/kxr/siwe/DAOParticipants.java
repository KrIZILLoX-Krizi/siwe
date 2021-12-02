package com.kxr.siwe;

import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class DAOParticipants {
    public DatabaseReference databaseReference;

    public DAOParticipants () {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        databaseReference = db.getReference(Participants.class.getSimpleName());
    }

    public Task<Void> add (Participants participants) {
        return databaseReference.push().setValue(participants);
    }

    public Task<Void> update (String key, HashMap<String, Object> hashMap) {
        return databaseReference.child(key).updateChildren(hashMap);
    }

    public Task<Void> remove (String key) {
        return databaseReference.child(key).removeValue();
    }
}
