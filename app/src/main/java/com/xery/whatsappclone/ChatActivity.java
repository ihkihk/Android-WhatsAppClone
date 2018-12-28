package com.xery.whatsappclone;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.xery.whatsappclone.Chat.MediaAdapter;
import com.xery.whatsappclone.Chat.MessageAdapter;
import com.xery.whatsappclone.Chat.MessageObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    int PICK_MEDIA_REQCODE = 1;

    private RecyclerView mChat, mMedia;
    private RecyclerView.Adapter mChatAdapter, mMediaAdapter;
    private RecyclerView.LayoutManager mChatLayoutManager, mMediaLayoutManager;

    ArrayList<MessageObject> messageList;
    ArrayList<String> mediaUriList;

    String chatID;
    DatabaseReference mChatDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatID = getIntent().getExtras().getString("chatID");

        Button mSend = findViewById(R.id.send);
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        Button mAddMedia = findViewById(R.id.addMedia);
        mAddMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        mChatDB = FirebaseDatabase.getInstance().getReference().child("chats").child(chatID);

        initializeMessages();
        initializeMedia();
        getChatMessages();
    }



    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select picture(s)"), PICK_MEDIA_REQCODE);
    }

    private void initializeMedia() {
        mediaUriList = new ArrayList<>();

        mMedia = findViewById(R.id.mediaList);
        mMedia.setNestedScrollingEnabled(false);
        mMedia.setHasFixedSize(false);

        mMediaLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false);
        mMedia.setLayoutManager(mMediaLayoutManager);

        mMediaAdapter = new MediaAdapter(getApplicationContext(), mediaUriList);
        mMedia.setAdapter(mMediaAdapter);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_MEDIA_REQCODE) {
                if (data.getClipData() == null) {
                    // only one image was picked
                    mediaUriList.add(data.getData().toString());
                } else {
                    // multiple images were selected
                    for (int i=0; i < data.getClipData().getItemCount(); i++) {
                        mediaUriList.add(data.getClipData().getItemAt(i).getUri().toString());
                    }
                }

                mMediaAdapter.notifyDataSetChanged();
            }
        }
    }

    private void getChatMessages() {
        mChatDB.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (dataSnapshot.exists()) {
                    String message = "", creator = "";
                    ArrayList<String> mediaUrlList = new ArrayList<>();

                    if (dataSnapshot.child("message").getValue() != null)
                        message = dataSnapshot.child("message").getValue().toString();
                    if (dataSnapshot.child("creator").getValue() != null)
                        creator = dataSnapshot.child("creator").getValue().toString();
                    if (dataSnapshot.child("media").getValue() != null)
                        if (dataSnapshot.child("media").getChildrenCount() != 0)
                            for (DataSnapshot media : dataSnapshot.child("media").getChildren())
                                mediaUrlList.add(media.getValue().toString());

                    MessageObject msg = new MessageObject(dataSnapshot.getKey(), creator, message, mediaUrlList);
                    messageList.add(msg);
                    mChatLayoutManager.scrollToPosition(messageList.size()-1);
                    mChatAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    int totalMediaUploaded = 0;
    ArrayList<String> mediaIdList = new ArrayList<>();
    EditText mMessage;

    private void sendMessage() {
        mMessage = findViewById(R.id.message);

        String messageId = mChatDB.push().getKey();
        final DatabaseReference msgDB = mChatDB.child(messageId);

        final Map<String, Object> newMessageMap = new HashMap<>();

        newMessageMap.put("creator", FirebaseAuth.getInstance().getUid());

        if (! mMessage.getText().toString().isEmpty())
            newMessageMap.put("message", mMessage.getText().toString());

        if (!mediaUriList.isEmpty()) {
            for (String uri : mediaUriList) {
                String mediaId = msgDB.child("media").push().getKey();
                mediaIdList.add(mediaId);

                final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("chats").
                        child(chatID).child(messageId).child(mediaId);

                UploadTask uploadTask = filePath.putFile(Uri.parse(uri));

                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                newMessageMap.put("/media/" + mediaIdList.get(totalMediaUploaded) + "/", uri.toString());

                                totalMediaUploaded++;

                                if (totalMediaUploaded == mediaUriList.size()) {
                                    updateDatabaseWithNewMessage(msgDB, newMessageMap);
                                }
                            }
                        });
                    }
                });
            }
        } else {
            if (! mMessage.getText().toString().isEmpty()) {
                updateDatabaseWithNewMessage(msgDB, newMessageMap);
            }
        }
    } /* */

    private void updateDatabaseWithNewMessage(DatabaseReference msgDB, Map<String,Object> newMessageMap) {
        msgDB.updateChildren(newMessageMap);
        totalMediaUploaded = 0;
        mMessage.setText(null);
        mediaUriList.clear();
        mediaIdList.clear();
        mMediaAdapter.notifyDataSetChanged();
    }

    private void initializeMessages() {
        messageList = new ArrayList<>();

        mChat = findViewById(R.id.messageList);
        mChat.setNestedScrollingEnabled(false);
        mChat.setHasFixedSize(false);

        mChatLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        mChat.setLayoutManager(mChatLayoutManager);

        mChatAdapter = new MessageAdapter(messageList);
        mChat.setAdapter(mChatAdapter);
    }
}
