package com.example.overlord.btech_project.globals_mutable;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by overlord on 15/4/18.
 */

public class Singleton {
    private static final Singleton ourInstance = new Singleton();
    public static Singleton getInstance() {
        return ourInstance;
    }

    public FirebaseDatabase realtime_database;
    public DatabaseReference root;
    public DatabaseReference heartRef;

    private Singleton() {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        realtime_database = FirebaseDatabase.getInstance();
        heartRef = root.child("heartBeat");
        root     = realtime_database.getReference();

    }
}
