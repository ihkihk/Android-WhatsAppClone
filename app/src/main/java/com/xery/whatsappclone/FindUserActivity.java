package com.xery.whatsappclone;

import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.xery.whatsappclone.User.UserObject;
import com.xery.whatsappclone.User.UserListAdapter;
import com.xery.whatsappclone.Utils.Iso2Phone;

import java.util.ArrayList;

public class FindUserActivity extends AppCompatActivity {

    private RecyclerView mUserList;
    private RecyclerView.Adapter mUserListAdapter;
    private RecyclerView.LayoutManager mUserListLayoutManager;

    ArrayList<UserObject> userList, contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_user);

        initializeRecycleView();

        getContactsList();
    }

    private void getContactsList() {
        String ISOPrefix = getCountryISO();

        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);

        if (phones != null) {
            while (phones.moveToNext()) {
                String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phone = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                phone = phone.replace(" ", "");
                phone = phone.replace("-", "");
                phone = phone.replace("(", "");
                phone = phone.replace(")", "");

                if (!String.valueOf(phone.charAt(0)).equals("+"))
                    phone = ISOPrefix + phone;

                UserObject mContact = new UserObject("", name, phone);
                contactList.add(mContact);
                getUserDetails(mContact);
            }

            phones.close();
        }
    }

    private void getUserDetails(final UserObject mContact) {
        DatabaseReference mUserDB = FirebaseDatabase.getInstance().getReference().child("users");
        Query query = mUserDB.orderByChild("phone").equalTo(mContact.getPhone());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String phone = "", name = "";

                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        if (childSnapshot.child("phone").getValue() != null)
                            phone = childSnapshot.child("phone").getValue().toString();
                        if (childSnapshot.child("name").getValue() != null)
                            name = childSnapshot.child("name").getValue().toString();

                        UserObject mUser = new UserObject(childSnapshot.getKey(), name, phone);

                        if (name.equals(phone)) {
                            for (UserObject it : contactList) {
                                if (it.getPhone().equals(mUser.getPhone())) {
                                    mUser.setName(it.getName());
                                }
                            }
                        }
                        userList.add(mUser);
                        mUserListAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private String getCountryISO() {
        String iso = null;

        TelephonyManager telMngr = (TelephonyManager)getApplicationContext().getSystemService(android.content.Context.TELEPHONY_SERVICE);

        if (telMngr != null && telMngr.getNetworkCountryIso() != null) {
            if (!telMngr.getNetworkCountryIso().equals(""))
                iso = telMngr.getNetworkCountryIso();
        }

        return Iso2Phone.getPhone(iso);
    }

    private void initializeRecycleView() {
        userList = new ArrayList<>();
        contactList = new ArrayList<>();

        mUserList = findViewById(R.id.user_list);
        mUserList.setNestedScrollingEnabled(false);
        mUserList.setHasFixedSize(false);

        mUserListLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        mUserList.setLayoutManager(mUserListLayoutManager);

        mUserListAdapter = new UserListAdapter(userList);
        mUserList.setAdapter(mUserListAdapter);

    }
}
