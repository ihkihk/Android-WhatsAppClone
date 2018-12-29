package com.xery.whatsappclone;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;
import com.xery.whatsappclone.Chat.ChatObject;
import com.xery.whatsappclone.Chat.ChatListAdapter;
import com.xery.whatsappclone.User.UserObject;

import java.util.ArrayList;

public class MainPageActivity extends AppCompatActivity {

    private RecyclerView mChatList;
    private RecyclerView.Adapter mChatListAdapter;
    private RecyclerView.LayoutManager mChatListLayoutManager;

    ArrayList<ChatObject> chatList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        OneSignal.startInit(this).init();
        OneSignal.setSubscription(true);
        OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
            @Override
            public void idsAvailable(String userId, String registrationId) {
                FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getUid()).
                        child("notificationKey").setValue(userId);
            }
        });
        OneSignal.setInFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification);

        Button mLogout = (Button)findViewById(R.id.logout);
        Button mFinduser = (Button)findViewById(R.id.find_user);

        mFinduser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), FindUserActivity.class));
            }
        });

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                OneSignal.setSubscription(false);
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        getPermissions();
        initializeRecycleView();
        getUserChatList();
    }

    private void getUserChatList() {
        DatabaseReference mUserChatDB = FirebaseDatabase.getInstance().getReference().child("users").
                child(FirebaseAuth.getInstance().getUid()).child("chats");

        mUserChatDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        ChatObject chat = new ChatObject(child.getKey());
                        boolean exists = false;
                        for (ChatObject it : chatList) {
                            if (it.getChatId().equals(chat.getChatId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (exists)
                            continue;
                        chatList.add(chat);
                        getChatData(chat.getChatId());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getChatData(final String chatId) {
        DatabaseReference mChatDB = FirebaseDatabase.getInstance().getReference().child("chats").child(chatId).child("info");

        mChatDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String chatId = "";

                    if (dataSnapshot.child("id").getValue() != null)
                        chatId = dataSnapshot.child("id").getValue().toString();

                    for (DataSnapshot userSnapshot : dataSnapshot.child("users").getChildren()) {
                        for (ChatObject chat : chatList) {
                            if (chat.getChatId().equals(chatId)) {
                                UserObject mUser = new UserObject(userSnapshot.getKey());
                                chat.addUser(mUser);
                                getUserData(mUser);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getUserData(UserObject mUser) {
        DatabaseReference mUserDb = FirebaseDatabase.getInstance().getReference().child("users").child(mUser.getUid());

        mUserDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserObject mUser = new UserObject(dataSnapshot.getKey());

                if (dataSnapshot.child("notificationKey").getValue() != null)
                    mUser.setNotificationKey(dataSnapshot.child("notificationKey").getValue().toString());

                for (ChatObject mChat : chatList) {
                    for (UserObject mUserIt : mChat.getUserObjectArrayList()) {
                        if (mUserIt.getUid().equals(mUser.getUid())) {
                            mUserIt.setNotificationKey(mUser.getNotificationKey());
                        }
                    }
                }

                mChatListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void initializeRecycleView() {
        chatList = new ArrayList<>();

        mChatList = findViewById(R.id.chatList);
        mChatList.setNestedScrollingEnabled(false);
        mChatList.setHasFixedSize(false);

        mChatListLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        mChatList.setLayoutManager(mChatListLayoutManager);

        mChatListAdapter = new ChatListAdapter(chatList);
        mChatList.setAdapter(mChatListAdapter);

    }

    private void getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] {Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS}, 1);
        }
    }
}
