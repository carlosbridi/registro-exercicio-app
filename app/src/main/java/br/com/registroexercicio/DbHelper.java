package br.com.registroexercicio;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DbHelper extends SQLiteOpenHelper {

    public static final String TIPO_BICICLETA = "Bicicleta";
    public static final String TIPO_ESTEIRA = "Esteira";

    private static final String DB_NAME = "exercicios.db";
    private static final int DB_VERSION = 1;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE atividade ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "tipo TEXT NOT NULL,"
                + "data TEXT NOT NULL,"      // yyyy-MM-dd
                + "minutos INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_atividade_data ON atividade(data)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void inserir(String tipo, String data, int minutos) {
        ContentValues v = new ContentValues();
        v.put("tipo", tipo);
        v.put("data", data);
        v.put("minutos", minutos);
        getWritableDatabase().insert("atividade", null, v);
    }

    /** Soma de minutos por tipo no intervalo (inclusive). Todos os tipos aparecem, mesmo com 0. */
    public Map<String, Integer> totaisPorTipo(String de, String ate) {
        Map<String, Integer> totais = new LinkedHashMap<String, Integer>();
        totais.put(TIPO_BICICLETA, 0);
        totais.put(TIPO_ESTEIRA, 0);
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT tipo, SUM(minutos) FROM atividade WHERE data BETWEEN ? AND ? GROUP BY tipo",
                new String[]{de, ate});
        try {
            while (c.moveToNext()) {
                totais.put(c.getString(0), c.getInt(1));
            }
        } finally {
            c.close();
        }
        return totais;
    }

    public static class Registro {
        public long id;
        public String tipo;
        public String data;   // yyyy-MM-dd
        public int minutos;
    }

    public List<Registro> listar(String tipo, String de, String ate, int limite, int offset) {
        List<Registro> lista = new ArrayList<Registro>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, tipo, data, minutos FROM atividade "
                        + "WHERE tipo = ? AND data BETWEEN ? AND ? ORDER BY data DESC, id DESC LIMIT ? OFFSET ?",
                new String[]{tipo, de, ate, String.valueOf(limite), String.valueOf(offset)});
        try {
            while (c.moveToNext()) {
                Registro r = new Registro();
                r.id = c.getLong(0);
                r.tipo = c.getString(1);
                r.data = c.getString(2);
                r.minutos = c.getInt(3);
                lista.add(r);
            }
        } finally {
            c.close();
        }
        return lista;
    }

    /** Todas as atividades, da mais antiga para a mais recente. */
    public List<Registro> listarTodas() {
        List<Registro> lista = new ArrayList<Registro>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, tipo, data, minutos FROM atividade ORDER BY data, id", null);
        try {
            while (c.moveToNext()) {
                Registro r = new Registro();
                r.id = c.getLong(0);
                r.tipo = c.getString(1);
                r.data = c.getString(2);
                r.minutos = c.getInt(3);
                lista.add(r);
            }
        } finally {
            c.close();
        }
        return lista;
    }

    public int contar() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM atividade", null);
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    /**
     * Importa atividades de um backup e devolve quantas foram inseridas.
     * Com {@code substituir}, apaga tudo antes. Sem ele, só insere o que excede o que já existe
     * (mesmo tipo, data e minutos), de modo que reimportar o mesmo backup não duplica nada.
     */
    public int importar(List<Registro> registros, boolean substituir) {
        SQLiteDatabase db = getWritableDatabase();
        int inseridos = 0;
        db.beginTransaction();
        try {
            Map<String, Integer> existentes = new HashMap<String, Integer>();
            if (substituir) {
                db.delete("atividade", null, null);
            } else {
                Cursor c = db.rawQuery(
                        "SELECT tipo, data, minutos, COUNT(*) FROM atividade GROUP BY tipo, data, minutos", null);
                try {
                    while (c.moveToNext()) {
                        existentes.put(c.getString(0) + '|' + c.getString(1) + '|' + c.getInt(2), c.getInt(3));
                    }
                } finally {
                    c.close();
                }
            }
            for (Registro r : registros) {
                String chave = r.tipo + '|' + r.data + '|' + r.minutos;
                Integer ja = existentes.get(chave);
                if (ja != null && ja > 0) {
                    existentes.put(chave, ja - 1);
                    continue;
                }
                ContentValues v = new ContentValues();
                v.put("tipo", r.tipo);
                v.put("data", r.data);
                v.put("minutos", r.minutos);
                db.insertOrThrow("atividade", null, v);
                inseridos++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return inseridos;
    }

    public void atualizar(long id, String tipo, String data, int minutos) {
        ContentValues v = new ContentValues();
        v.put("tipo", tipo);
        v.put("data", data);
        v.put("minutos", minutos);
        getWritableDatabase().update("atividade", v, "id = ?", new String[]{String.valueOf(id)});
    }

    public void excluir(long id) {
        getWritableDatabase().delete("atividade", "id = ?", new String[]{String.valueOf(id)});
    }

    /** Soma de minutos por mês (chave yyyy-MM) de um tipo no intervalo (inclusive). */
    public Map<String, Integer> totaisPorMes(String tipo, String de, String ate) {
        Map<String, Integer> totais = new LinkedHashMap<String, Integer>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT substr(data, 1, 7), SUM(minutos) FROM atividade "
                        + "WHERE tipo = ? AND data BETWEEN ? AND ? GROUP BY 1",
                new String[]{tipo, de, ate});
        try {
            while (c.moveToNext()) {
                totais.put(c.getString(0), c.getInt(1));
            }
        } finally {
            c.close();
        }
        return totais;
    }
}
