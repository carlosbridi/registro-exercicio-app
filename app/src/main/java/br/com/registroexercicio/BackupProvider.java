package br.com.registroexercicio;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Expõe, somente leitura, os arquivos de backup gerados no cache para que outros apps
 * (e-mail, Google Drive...) possam recebê-los pelo compartilhamento. Não é exportado:
 * o acesso é concedido por URI a cada compartilhamento.
 */
public class BackupProvider extends ContentProvider {

    private static final String AUTORIDADE = "br.com.registroexercicio.backup";
    public static final String MIME = "text/csv";

    public static File pasta(Context context) {
        File pasta = new File(context.getCacheDir(), "backup");
        pasta.mkdirs();
        return pasta;
    }

    public static Uri uriPara(File arquivo) {
        return new Uri.Builder().scheme("content").authority(AUTORIDADE).appendPath(arquivo.getName()).build();
    }

    private File arquivo(Uri uri) throws FileNotFoundException {
        String nome = uri.getLastPathSegment();
        if (nome == null || nome.indexOf('/') >= 0 || nome.startsWith(".")) {
            throw new FileNotFoundException(String.valueOf(uri));
        }
        File f = new File(pasta(getContext()), nome);
        if (!f.isFile()) throw new FileNotFoundException(String.valueOf(uri));
        return f;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Somente leitura");
        return ParcelFileDescriptor.open(arquivo(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (projection == null) projection = new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor cursor = new MatrixCursor(projection, 1);
        File f;
        try {
            f = arquivo(uri);
        } catch (FileNotFoundException e) {
            return cursor;
        }
        Object[] linha = new Object[projection.length];
        for (int i = 0; i < projection.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(projection[i])) linha[i] = f.getName();
            else if (OpenableColumns.SIZE.equals(projection[i])) linha[i] = f.length();
        }
        cursor.addRow(linha);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return MIME;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
