package com.kxr.siwe;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Signup extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText et_email;
    private EditText et_password;
    private Button btn_signup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        et_email = findViewById(R.id.et_email);
        et_password = findViewById(R.id.et_password);
        btn_signup = findViewById(R.id.btn_signup);

        btn_signup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mAuth = FirebaseAuth.getInstance();

                mAuth.createUserWithEmailAndPassword(et_email.getText().toString(), et_password.getText().toString())
                        .addOnCompleteListener(Signup.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d(TAG, "createUserWithEmail:success");

                                    FirebaseUser user = mAuth.getCurrentUser();

                                    Toast.makeText(Signup.this, "Logged-in",
                                            Toast.LENGTH_SHORT).show();

                                    finish();

                                    // updateUI(user);
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                    Toast.makeText(Signup.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                    // updateUI(null);
                                }
                            }
                        });
            }
        });
    }
}