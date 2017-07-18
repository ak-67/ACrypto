package dev.dworks.apps.acrypto.misc;

import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import dev.dworks.apps.acrypto.App;
import dev.dworks.apps.acrypto.entity.User;

import static dev.dworks.apps.acrypto.App.APP_VERSION;
import static dev.dworks.apps.acrypto.entity.User.USERS;
import static dev.dworks.apps.acrypto.settings.SettingsActivity.getUserCurrencyFrom;

/**
 * Created by HaKr on 16/05/17.
 */

public class FirebaseHelper {

    public static FirebaseUser signInAnonymously(){
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(null == currentUser) {
            mAuth.signInAnonymously();
        }
        return currentUser;
    }

    public static FirebaseAuth getFirebaseAuth(){
        return FirebaseAuth.getInstance();
    }

    public static DatabaseReference getFirebaseDatabaseReference(){
       return FirebaseDatabase.getInstance().getReference();
    }

    public static DatabaseReference getFirebaseDatabaseReference(String path){
        return FirebaseDatabase.getInstance().getReference(path);
    }

    public static FirebaseUser getCurrentUser(){
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public static String getCurrentUid(){
        if(null != getCurrentUser()) {
            return getCurrentUser().getUid().replace(".","-");
        } else {
            return "";
        }
    }

    public static boolean isLoggedIn(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return null != user && !user.isAnonymous();
    }

    public static boolean isAnonymous(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return null != user && user.isAnonymous();
    }

    public static void logout() {
        FirebaseAuth.getInstance().signOut();
    }

    public static void updateUser() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(!isLoggedIn()){
            return;
        }
        User user = new User(
                firebaseUser.getDisplayName(),
                firebaseUser.getEmail(),
                FirebaseHelper.getCurrentUid(),
                firebaseUser.getPhotoUrl() == null ? "" : firebaseUser.getPhotoUrl().toString()
        );

        String uid = FirebaseHelper.getCurrentUid();
        String photoUrl = firebaseUser.getPhotoUrl() == null ? "" : firebaseUser.getPhotoUrl().toString();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("displayName", firebaseUser.getDisplayName());
        childUpdates.put("email", firebaseUser.getEmail());
        childUpdates.put("uid", uid);
        childUpdates.put("photoUrl", photoUrl);
        childUpdates.put("appVersion", APP_VERSION);
        childUpdates.put("nativeCurrency", getUserCurrencyFrom());

/*        String instanceId = FirebaseInstanceId.getInstance().getToken();
        if (instanceId != null) {
            childUpdates.put("instanceId", instanceId);
        }*/

        FirebaseHelper.getFirebaseDatabaseReference().child(USERS)
                .child(uid).updateChildren(childUpdates);

        FirebaseHelper.getFirebaseDatabaseReference().child(USERS)
                .child(uid)
                .child("createdAt").setValue(ServerValue.TIMESTAMP);

        App.getInstance().updateInstanceId();
    }

    public static void updateUserSubscription(String productId) {
        if(!isLoggedIn()){
            return;
        }
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("subscriptionStatus", 1);
        childUpdates.put("subscriptionId", productId);
        FirebaseHelper.getFirebaseDatabaseReference().child(USERS)
                .child(FirebaseHelper.getCurrentUid())
                .updateChildren(childUpdates);
    }

    public static void updateUserAppVersion(String version) {
        if(!isLoggedIn()){
            return;
        }
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("appVersion", version);
        FirebaseHelper.getFirebaseDatabaseReference().child(USERS)
                .child(FirebaseHelper.getCurrentUid())
                .updateChildren(childUpdates);
    }

    public static void updateUserSubscription(boolean active) {
        if(!isLoggedIn()){
            return;
        }
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("subscriptionStatus", active ? 1 : 0);
        AnalyticsManager.setProperty("IsSubscribed", String.valueOf(active));
        FirebaseHelper.getFirebaseDatabaseReference().child(USERS)
                .child(FirebaseHelper.getCurrentUid())
                .updateChildren(childUpdates);
    }

    public static void updateNativeCurrency(String currency) {
        if(!isLoggedIn()){
            return;
        }
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("nativeCurrency", currency);
        FirebaseHelper.getFirebaseDatabaseReference().child(USERS)
                .child(FirebaseHelper.getCurrentUid())
                .updateChildren(childUpdates);
    }

    public static void updateInstanceId(String registrationId) {
        if(!isLoggedIn()){
            return;
        }
        FirebaseDatabase.getInstance().getReference()
                .child(USERS)
                .child(FirebaseHelper.getCurrentUid())
                .child("instanceId")
                .setValue(registrationId);
    }

    public static void checkInstanceIdValidity(){
        if(!isLoggedIn()){
            return;
        }
        FirebaseHelper.getFirebaseDatabaseReference("users/"+FirebaseHelper.getCurrentUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if(TextUtils.isEmpty(user.instanceId)){
                    App.getInstance().updateInstanceId();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
