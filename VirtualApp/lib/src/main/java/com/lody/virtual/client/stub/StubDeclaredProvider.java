package com.lody.virtual.client.stub;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class StubDeclaredProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    public static class P0 extends StubDeclaredProvider {
    }

    public static class P1 extends StubDeclaredProvider {
    }

    public static class P2 extends StubDeclaredProvider {
    }

    public static class P3 extends StubDeclaredProvider {
    }

    public static class P4 extends StubDeclaredProvider {
    }

    public static class P5 extends StubDeclaredProvider {
    }

    public static class P6 extends StubDeclaredProvider {
    }

    public static class P7 extends StubDeclaredProvider {
    }

    public static class P8 extends StubDeclaredProvider {
    }

    public static class P9 extends StubDeclaredProvider {
    }

    public static class P10 extends StubDeclaredProvider {
    }

    public static class P11 extends StubDeclaredProvider {
    }

    public static class P12 extends StubDeclaredProvider {
    }

    public static class P13 extends StubDeclaredProvider {
    }

    public static class P14 extends StubDeclaredProvider {
    }

}
