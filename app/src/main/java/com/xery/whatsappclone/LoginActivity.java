package com.xery.whatsappclone;

import android.content.Intent;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class LoginActivity extends AppCompatActivity {

    private static final String IS_PHONE_VERIF_RUNNING_KEY = "PHVERRUNKEY";
    private static final String TAG = "WAPCLONE";

    private EditText mPhone, mCode;
    private Button mSendCode;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider mPhoneAuth;
    private boolean isPhoneVerificationRunning;
    private String mVerificationId;

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putBoolean(IS_PHONE_VERIF_RUNNING_KEY, isPhoneVerificationRunning);

        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        isPhoneVerificationRunning = savedInstanceState.getBoolean(IS_PHONE_VERIF_RUNNING_KEY, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Fresco.initialize(this);
        OneSignal.startInit(this).init();

        mAuth = FirebaseAuth.getInstance();
        mPhoneAuth = PhoneAuthProvider.getInstance();

        userIsLoggedIn();

        mPhone = findViewById(R.id.phoneNumber);
        mCode = findViewById(R.id.authCode);
        Button mGetCode = findViewById(R.id.getCode);
        mSendCode = findViewById(R.id.sendCode);

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                Log.wtf(TAG, "onVerificationCompleted: " + phoneAuthCredential);

                isPhoneVerificationRunning = false;
                signInWithPhoneCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.d(TAG, "onVerificationFailed: " + e);

                isPhoneVerificationRunning = false;

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(LoginActivity.this, "Incorrect credentials\n" +
                                    e.getMessage(),
                            Toast.LENGTH_LONG).show();
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    Toast.makeText(LoginActivity.this,
                            "Sorry, two many connection request. Wait a bit!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);

                isPhoneVerificationRunning = false;

                mVerificationId = s;

                mCode.setEnabled(true);
                mSendCode.setEnabled(true);
            }
        };


        mGetCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPhoneNumberVerification();
            }
        });

        mSendCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, mCode.getText().toString());

                signInWithPhoneCredential(credential);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isPhoneVerificationRunning)
            mPhoneAuth.verifyPhoneNumber(
                mPhone.getText().toString(),
                60, TimeUnit.SECONDS,
                this,
                mCallbacks);

    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(
                this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    final FirebaseUser user = mAuth.getCurrentUser();

                    if (user != null) {
                        final DatabaseReference mUserDB = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid());

                        mUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (!dataSnapshot.exists()) {
                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("phone", user.getPhoneNumber());
                                    userMap.put("name", user.getPhoneNumber());
                                    mUserDB.updateChildren(userMap);
                                }

                                userIsLoggedIn();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }
        });
    }

    private void userIsLoggedIn() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName("Ivailo").build()).
                    addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.wtf(TAG, "Firebase.user.updateProfile(): Finished update user displayName");
                        }
                    });

            Log.wtf(TAG, "Firebase.getCurrentUser(): " + user.getDisplayName());

            startActivity(new Intent(getApplicationContext(), MainPageActivity.class));
            finish();
        }
    }

    private void startPhoneNumberVerification() {
        isPhoneVerificationRunning = true;

        mPhoneAuth.verifyPhoneNumber(
                mPhone.getText().toString(),
                60, TimeUnit.SECONDS,
                this,
                mCallbacks);
    }
}
