package br.com.registroexercicio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Leitura e escrita do backup em CSV (colunas: tipo, data yyyy-MM-dd, minutos). */
public final class BackupCsv {

    public static final String CABECALHO = "tipo,data,minutos";

    public static class Leitura {
        public final List<DbHelper.Registro> registros = new ArrayList<DbHelper.Registro>();
        /** Linhas que não puderam ser interpretadas. */
        public int ignoradas;
    }

    private BackupCsv() {
    }

    public static String gerar(List<DbHelper.Registro> registros) {
        StringBuilder sb = new StringBuilder(CABECALHO).append("\r\n");
        for (DbHelper.Registro r : registros) {
            sb.append(r.tipo).append(',').append(r.data).append(',').append(r.minutos).append("\r\n");
        }
        return sb.toString();
    }

    /** Aceita separador "," ou ";" e datas em yyyy-MM-dd ou dd/MM/yyyy (planilhas costumam regravar assim). */
    public static Leitura ler(InputStream in) throws IOException {
        Leitura leitura = new Leitura();
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat br = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        iso.setLenient(false);
        br.setLenient(false);

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        boolean primeira = true;
        String linha;
        while ((linha = reader.readLine()) != null) {
            if (primeira && linha.length() > 0 && linha.charAt(0) == '﻿') linha = linha.substring(1);
            linha = linha.trim();
            if (linha.length() == 0) continue;

            String[] campos = linha.split(linha.indexOf(';') >= 0 ? ";" : ",", -1);
            for (int i = 0; i < campos.length; i++) campos[i] = limpar(campos[i]);

            boolean cabecalho = primeira && campos[0].equalsIgnoreCase("tipo");
            primeira = false;
            if (cabecalho) continue;

            DbHelper.Registro r = campos.length >= 3 ? converter(campos, iso, br) : null;
            if (r == null) {
                leitura.ignoradas++;
            } else {
                leitura.registros.add(r);
            }
        }
        return leitura;
    }

    private static String limpar(String campo) {
        campo = campo.trim();
        if (campo.length() >= 2 && campo.charAt(0) == '"' && campo.charAt(campo.length() - 1) == '"') {
            campo = campo.substring(1, campo.length() - 1).trim();
        }
        return campo;
    }

    private static DbHelper.Registro converter(String[] campos, SimpleDateFormat iso, SimpleDateFormat br) {
        DbHelper.Registro r = new DbHelper.Registro();
        if (DbHelper.TIPO_BICICLETA.equalsIgnoreCase(campos[0])) {
            r.tipo = DbHelper.TIPO_BICICLETA;
        } else if (DbHelper.TIPO_ESTEIRA.equalsIgnoreCase(campos[0])) {
            r.tipo = DbHelper.TIPO_ESTEIRA;
        } else {
            return null;
        }
        try {
            if (campos[1].matches("\\d{4}-\\d{2}-\\d{2}")) {
                r.data = iso.format(iso.parse(campos[1]));
            } else if (campos[1].matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                r.data = iso.format(br.parse(campos[1]));
            } else {
                return null;
            }
            r.minutos = Integer.parseInt(campos[2]);
        } catch (ParseException e) {
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
        return r.minutos > 0 ? r : null;
    }
}
