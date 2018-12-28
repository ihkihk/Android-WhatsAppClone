package com.xery.whatsappclone.User;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.xery.whatsappclone.R;

import java.util.ArrayList;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserListViewHolder> {

    ArrayList<UserObject> userList;

    public UserListAdapter(ArrayList<UserObject> userList) {
        this.userList = userList;
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    @NonNull
    @Override
    public UserListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);

        UserListViewHolder rcv = new UserListViewHolder(layoutView);
        return rcv;
    }

    @Override
    public void onBindViewHolder(@NonNull UserListViewHolder holder, final int position) {
        holder.mName.setText(userList.get(position).getName());
        holder.mPhone.setText(userList.get(position).getPhone());

        holder.mLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = FirebaseDatabase.getInstance().getReference().child("chats").push().getKey();

                FirebaseDatabase.getInstance().getReference().child("users").
                        child(FirebaseAuth.getInstance().getUid()).child("chat").
                        child(key).setValue(true);
                FirebaseDatabase.getInstance().getReference().child("users").
                        child(userList.get(position).getUid()).child("chat").
                        child(key).setValue(true);

            }
        });

    }

    class UserListViewHolder extends RecyclerView.ViewHolder {

        private TextView mName, mPhone;
        private LinearLayout mLayout;

        public UserListViewHolder(View itemView) {
            super(itemView);

            mName = itemView.findViewById(R.id.name);
            mPhone = itemView.findViewById(R.id.phone);
            mLayout = itemView.findViewById(R.id.user_layout);
        }


    }
}
