package com.example.alldemo.features.facerecognitionandroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class DBManager extends SQLiteOpenHelper {

    public static ArrayList<Person> personList = new ArrayList<Person>();

    public DBManager(Context context) {
        super(context, "mydb" , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "create table person " +
                        "(name text, face blob, templates blob)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS person");
        onCreate(db);
    }

    public void insertPerson (String name, Bitmap face, byte[] templates) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        face.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] faceJpg = byteArrayOutputStream.toByteArray();

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("face", faceJpg);
        contentValues.put("templates", templates);
        db.insert("person", null, contentValues);

        personList.add(new Person(name, face, templates));
    }

    public Integer deletePerson (String name) {
        for(int i = 0; i < personList.size(); i ++) {
            if(personList.get(i).name != null && personList.get(i).name.equals(name)) {
                personList.remove(i);
                i --;
            }
        }

        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("person",
                "name = ? ",
                new String[] { name });
    }

    public Integer clearDB () {
        personList.clear();

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from person");
        return 0;
    }

    public void loadPerson() {
        personList.clear();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "select * from person", null );
        if (res != null && res.moveToFirst()) {
            while (!res.isAfterLast()) {
                int nameIdx = res.getColumnIndex("name");
                int faceIdx = res.getColumnIndex("face");
                int tplIdx = res.getColumnIndex("templates");

                if (nameIdx != -1 && faceIdx != -1 && tplIdx != -1) {
                    String name = res.getString(nameIdx);
                    byte[] faceJpg = res.getBlob(faceIdx);
                    byte[] templates = res.getBlob(tplIdx);
                    Bitmap face = BitmapFactory.decodeByteArray(faceJpg, 0, faceJpg.length);

                    Person person = new Person(name, face, templates);
                    personList.add(person);
                }
                res.moveToNext();
            }
            res.close();
        }
    }
}
