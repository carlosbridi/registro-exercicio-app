package br.com.registroexercicio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int VINHO = Color.parseColor("#6D1F2F");
    private static final int SUAVE = Color.parseColor("#8A6B70");

    private final SimpleDateFormat dbFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat uiFmt = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));

    private DbHelper db;
    private float dp;

    private static final int TELA_REGISTRAR = 0, TELA_RELATORIO = 1, TELA_BACKUP = 2;
    private static final int REQ_IMPORTAR = 1, REQ_SALVAR_BACKUP = 2;

    private View telaRegistrar, telaRelatorio, telaBackup;
    private TextView txtSubtitulo;
    private int telaAtual = TELA_REGISTRAR;

    // Registrar
    private View tileBicicleta, tileEsteira;
    private TextView txtData;
    private EditText editMinutos;
    private String tipoSelecionado = DbHelper.TIPO_BICICLETA;
    private Calendar dataRegistro = Calendar.getInstance();

    // Relatório
    private static final int[] PERIODOS = {7, 15, 30, 0}; // 0 = personalizado
    private final TextView[] chips = new TextView[4];
    private int periodoSelecionado = 7;
    private View blocoCustom;
    private TextView btnDe, btnAte, txtIntervalo, txtTotal;
    private LinearLayout listaResultado;
    private Calendar dataDe = Calendar.getInstance();
    private Calendar dataAte = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DbHelper(this);
        dp = getResources().getDisplayMetrics().density;

        telaRegistrar = findViewById(R.id.telaRegistrar);
        telaRelatorio = findViewById(R.id.telaRelatorio);
        telaBackup = findViewById(R.id.telaBackup);
        txtSubtitulo = (TextView) findViewById(R.id.txtSubtitulo);

        findViewById(R.id.tabRegistrar).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { mostrarTela(TELA_REGISTRAR); }
        });
        findViewById(R.id.tabRelatorio).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { mostrarTela(TELA_RELATORIO); }
        });
        findViewById(R.id.tabBackup).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { mostrarTela(TELA_BACKUP); }
        });

        iniciarRegistrar();
        iniciarRelatorio();
        iniciarBackup();
        mostrarTela(savedInstanceState == null ? TELA_REGISTRAR : savedInstanceState.getInt("tela", TELA_REGISTRAR));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Atualiza o relatório ao voltar da lista/edição.
        if (telaRelatorio.getVisibility() == View.VISIBLE) atualizarRelatorio();
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    private int px(int valorDp) { return (int) (valorDp * dp + 0.5f); }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // O seletor de arquivos pode derrubar a activity; ao voltar, reabre na mesma aba.
        outState.putInt("tela", telaAtual);
    }

    private void mostrarTela(int tela) {
        telaAtual = tela;
        telaRegistrar.setVisibility(tela == TELA_REGISTRAR ? View.VISIBLE : View.GONE);
        telaRelatorio.setVisibility(tela == TELA_RELATORIO ? View.VISIBLE : View.GONE);
        telaBackup.setVisibility(tela == TELA_BACKUP ? View.VISIBLE : View.GONE);
        txtSubtitulo.setText(tela == TELA_REGISTRAR ? R.string.subtitulo_registrar
                : tela == TELA_RELATORIO ? R.string.subtitulo_relatorio : R.string.subtitulo_backup);
        estiloAba(R.id.iconeRegistrar, R.id.rotuloRegistrar, tela == TELA_REGISTRAR);
        estiloAba(R.id.iconeRelatorio, R.id.rotuloRelatorio, tela == TELA_RELATORIO);
        estiloAba(R.id.iconeBackup, R.id.rotuloBackup, tela == TELA_BACKUP);
        if (tela == TELA_RELATORIO) atualizarRelatorio();
    }

    private void estiloAba(int iconeId, int rotuloId, boolean ativa) {
        int cor = ativa ? VINHO : SUAVE;
        ((ImageView) findViewById(iconeId)).setColorFilter(cor);
        TextView r = (TextView) findViewById(rotuloId);
        r.setTextColor(cor);
        r.setTypeface(null, ativa ? Typeface.BOLD : Typeface.NORMAL);
    }

    /** Aplica a cor do ícone (primeiro ImageView filho) conforme o estado selecionado. */
    private void selecionar(View tile, boolean sel) {
        tile.setSelected(sel);
        if (tile instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) tile;
            for (int i = 0; i < g.getChildCount(); i++) {
                if (g.getChildAt(i) instanceof ImageView) {
                    ((ImageView) g.getChildAt(i)).setColorFilter(sel ? Color.WHITE : VINHO);
                }
            }
        }
    }

    // ---------- Tela Registrar ----------

    private void iniciarRegistrar() {
        tileBicicleta = findViewById(R.id.tileBicicleta);
        tileEsteira = findViewById(R.id.tileEsteira);
        txtData = (TextView) findViewById(R.id.txtData);
        editMinutos = (EditText) findViewById(R.id.editMinutos);

        tileBicicleta.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { escolherTipo(DbHelper.TIPO_BICICLETA); }
        });
        tileEsteira.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { escolherTipo(DbHelper.TIPO_ESTEIRA); }
        });
        escolherTipo(DbHelper.TIPO_BICICLETA);

        txtData.setText(uiFmt.format(dataRegistro.getTime()));
        findViewById(R.id.btnData).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { escolherData(dataRegistro, null); }
        });

        // atalhos de duração
        LinearLayout atalhos = (LinearLayout) findViewById(R.id.atalhos);
        for (final int min : new int[]{15, 30, 45, 60}) {
            TextView t = new TextView(this);
            t.setText(min + " min");
            t.setTextColor(VINHO);
            t.setTextSize(13);
            t.setTypeface(null, Typeface.BOLD);
            t.setGravity(Gravity.CENTER);
            t.setBackgroundResource(R.drawable.bg_quick);
            t.setClickable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, px(34), 1);
            lp.rightMargin = px(6);
            lp.leftMargin = px(6);
            t.setLayoutParams(lp);
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { editMinutos.setText(String.valueOf(min)); }
            });
            atalhos.addView(t);
        }

        findViewById(R.id.btnSalvar).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { salvar(); }
        });
    }

    private void escolherTipo(String tipo) {
        tipoSelecionado = tipo;
        selecionar(tileBicicleta, DbHelper.TIPO_BICICLETA.equals(tipo));
        selecionar(tileEsteira, DbHelper.TIPO_ESTEIRA.equals(tipo));
    }

    private void salvar() {
        String txt = editMinutos.getText().toString().trim();
        int minutos = txt.length() == 0 ? 0 : Integer.parseInt(txt);
        if (minutos <= 0) {
            editMinutos.setError("Informe os minutos");
            return;
        }
        db.inserir(tipoSelecionado, dbFmt.format(dataRegistro.getTime()), minutos);
        editMinutos.setText("");
        Toast.makeText(this, "Atividade registrada!", Toast.LENGTH_SHORT).show();
    }

    // ---------- Tela Relatório ----------

    private void iniciarRelatorio() {
        blocoCustom = findViewById(R.id.blocoCustom);
        btnDe = (TextView) findViewById(R.id.btnDe);
        btnAte = (TextView) findViewById(R.id.btnAte);
        txtIntervalo = (TextView) findViewById(R.id.txtIntervalo);
        txtTotal = (TextView) findViewById(R.id.txtTotal);
        listaResultado = (LinearLayout) findViewById(R.id.listaResultado);

        dataDe.add(Calendar.DAY_OF_YEAR, -6);
        atualizarBotoesCustom();

        String[] nomes = {getString(R.string.dias7), getString(R.string.dias15),
                getString(R.string.dias30), getString(R.string.personalizado)};
        LinearLayout box = (LinearLayout) findViewById(R.id.chips);
        for (int i = 0; i < 4; i++) {
            final int periodo = PERIODOS[i];
            TextView t = new TextView(this);
            t.setText(nomes[i]);
            t.setTextColor(getResources().getColorStateList(R.color.texto_selecao));
            t.setTextSize(13);
            t.setTypeface(null, Typeface.BOLD);
            t.setGravity(Gravity.CENTER);
            t.setBackgroundResource(R.drawable.bg_chip);
            t.setClickable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, px(40), 1);
            lp.rightMargin = px(3);
            lp.leftMargin = px(3);
            t.setLayoutParams(lp);
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { escolherPeriodo(periodo); }
            });
            chips[i] = t;
            box.addView(t);
        }
        escolherPeriodo(7);

        Runnable aoMudar = new Runnable() {
            public void run() { atualizarRelatorio(); }
        };
        btnDe.setOnClickListener(seletorCustom(dataDe, aoMudar));
        btnAte.setOnClickListener(seletorCustom(dataAte, aoMudar));
    }

    private void escolherPeriodo(int periodo) {
        periodoSelecionado = periodo;
        for (int i = 0; i < 4; i++) chips[i].setSelected(PERIODOS[i] == periodo);
        blocoCustom.setVisibility(periodo == 0 ? View.VISIBLE : View.GONE);
        atualizarRelatorio();
    }

    private View.OnClickListener seletorCustom(final Calendar alvo, final Runnable aoMudar) {
        return new View.OnClickListener() {
            public void onClick(View v) { escolherData(alvo, aoMudar); }
        };
    }

    private void atualizarBotoesCustom() {
        btnDe.setText(getString(R.string.de) + ": " + uiFmt.format(dataDe.getTime()));
        btnAte.setText(getString(R.string.ate) + ": " + uiFmt.format(dataAte.getTime()));
    }

    private void atualizarRelatorio() {
        if (listaResultado == null || chips[3] == null) return;
        Calendar de = Calendar.getInstance();
        Calendar ate = Calendar.getInstance();
        if (periodoSelecionado == 0) {
            de = (Calendar) dataDe.clone();
            ate = (Calendar) dataAte.clone();
        } else {
            de.add(Calendar.DAY_OF_YEAR, -(periodoSelecionado - 1));
        }

        String sDe = dbFmt.format(de.getTime());
        String sAte = dbFmt.format(ate.getTime());
        txtIntervalo.setText(uiFmt.format(de.getTime()) + "  \u2192  " + uiFmt.format(ate.getTime()));
        listaResultado.removeAllViews();

        if (sDe.compareTo(sAte) > 0) {
            txtTotal.setText("--");
            TextView aviso = new TextView(this);
            aviso.setText("A data inicial é maior que a final.");
            aviso.setTextColor(SUAVE);
            listaResultado.addView(aviso);
            return;
        }

        Map<String, Integer> totais = db.totaisPorTipo(sDe, sAte);
        int soma = 0;
        for (int v : totais.values()) soma += v;
        txtTotal.setText(formatarMinutos(soma));
        for (Map.Entry<String, Integer> e : totais.entrySet()) {
            adicionarCard(e.getKey(), e.getValue(), soma, sDe, sAte);
        }
    }

    private void adicionarCard(final String tipo, int minutos, int soma, final String de, final String ate) {
        LinearLayout card = new LinearLayout(this);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(px(16), px(16), px(16), px(16));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = px(12);
        card.setLayoutParams(cp);

        ImageView icone = new ImageView(this);
        icone.setImageResource(DbHelper.TIPO_BICICLETA.equals(tipo) ? R.drawable.ic_bike : R.drawable.ic_run);
        icone.setColorFilter(VINHO);
        icone.setBackgroundResource(R.drawable.bg_icone);
        icone.setPadding(px(10), px(10), px(10), px(10));
        card.addView(icone, new LinearLayout.LayoutParams(px(48), px(48)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        colp.leftMargin = px(14);
        card.addView(col, colp);

        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        TextView nome = new TextView(this);
        nome.setText(tipo);
        nome.setTextColor(Color.parseColor("#3B1F25"));
        nome.setTextSize(16);
        nome.setTypeface(null, Typeface.BOLD);
        linha.addView(nome, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView valor = new TextView(this);
        valor.setText(formatarMinutos(minutos));
        valor.setTextColor(VINHO);
        valor.setTextSize(16);
        valor.setTypeface(null, Typeface.BOLD);
        linha.addView(valor);
        col.addView(linha);

        int pct = soma == 0 ? 0 : Math.round(100f * minutos / soma);
        LinearLayout trilha = new LinearLayout(this);
        trilha.setBackgroundResource(R.drawable.bg_track);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, px(8));
        tp.topMargin = px(10);
        View preenchido = new View(this);
        preenchido.setBackgroundResource(R.drawable.bg_fill);
        trilha.addView(preenchido, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, pct));
        trilha.addView(new View(this), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100 - pct));
        col.addView(trilha, tp);

        TextView perc = new TextView(this);
        perc.setText(pct + "% do tempo total");
        perc.setTextColor(SUAVE);
        perc.setTextSize(12);
        perc.setPadding(0, px(6), 0, 0);
        col.addView(perc);

        ImageView seta = new ImageView(this);
        seta.setImageResource(R.drawable.ic_chevron);
        seta.setColorFilter(SUAVE);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(px(24), px(24));
        sp.leftMargin = px(6);
        card.addView(seta, sp);

        card.setClickable(true);
        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ListaActivity.class);
                i.putExtra(ListaActivity.EXTRA_TIPO, tipo);
                i.putExtra(ListaActivity.EXTRA_DE, de);
                i.putExtra(ListaActivity.EXTRA_ATE, ate);
                startActivity(i);
            }
        });
        listaResultado.addView(card);
    }

    private String formatarMinutos(int min) {
        if (min < 60) return min + " min";
        return (min / 60) + "h " + (min % 60) + "min";
    }

    // ---------- Tela Backup ----------

    private void iniciarBackup() {
        findViewById(R.id.btnBackup).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { fazerBackup(); }
        });
        findViewById(R.id.btnImportar).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { escolherArquivoImportacao(); }
        });
    }

    private void fazerBackup() {
        List<DbHelper.Registro> registros = db.listarTodas();
        if (registros.isEmpty()) {
            Toast.makeText(this, "Não há atividades para incluir no backup.", Toast.LENGTH_LONG).show();
            return;
        }
        final File arquivo;
        try {
            File pasta = BackupProvider.pasta(this);
            File[] antigos = pasta.listFiles();
            if (antigos != null) for (File f : antigos) f.delete();
            arquivo = new File(pasta, "registro_exercicios_"
                    + new SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(new Date()) + ".csv");
            OutputStream out = new FileOutputStream(arquivo);
            try {
                out.write(BackupCsv.gerar(registros).getBytes("UTF-8"));
            } finally {
                out.close();
            }
        } catch (IOException e) {
            avisar("Backup", "Não foi possível gerar o arquivo de backup.");
            return;
        }

        AlertDialog.Builder dlg = new AlertDialog.Builder(this)
                .setTitle("Backup concluído")
                .setMessage(atividades(registros.size()) + " no arquivo " + arquivo.getName()
                        + ".\n\nCompartilhe o arquivo para enviá-lo por e-mail ou guardá-lo no Google Drive.")
                .setNegativeButton("Fechar", null)
                .setPositiveButton("Compartilhar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { compartilhar(arquivo); }
                });
        if (Build.VERSION.SDK_INT >= 19) {
            dlg.setNeutralButton("Salvar no aparelho", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType(BackupProvider.MIME);
                    i.putExtra(Intent.EXTRA_TITLE, arquivo.getName());
                    abrir(i, REQ_SALVAR_BACKUP);
                }
            });
        }
        dlg.show();
    }

    private void compartilhar(File arquivo) {
        Uri uri = BackupProvider.uriPara(arquivo);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType(BackupProvider.MIME);
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.putExtra(Intent.EXTRA_SUBJECT, "Backup - " + getString(R.string.app_name));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= 16) {
            i.setClipData(ClipData.newRawUri(arquivo.getName(), uri));
        } else {
            // Antes do Android 4.1 a permissão não acompanha o EXTRA_STREAM: concede a cada app de destino.
            for (ResolveInfo info : getPackageManager().queryIntentActivities(i, 0)) {
                grantUriPermission(info.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }
        abrir(Intent.createChooser(i, "Compartilhar backup"), 0);
    }

    private void escolherArquivoImportacao() {
        // "*/*" porque cada fonte (Drive, e-mail, gerenciadores) classifica CSV com um tipo diferente.
        Intent i = new Intent(Build.VERSION.SDK_INT >= 19 ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        abrir(Build.VERSION.SDK_INT >= 19 ? i : Intent.createChooser(i, "Selecionar backup"), REQ_IMPORTAR);
    }

    private void abrir(Intent i, int requestCode) {
        try {
            if (requestCode == 0) startActivity(i);
            else startActivityForResult(i, requestCode);
        } catch (ActivityNotFoundException e) {
            avisar("Backup", "Nenhum aplicativo disponível para esta ação.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        if (requestCode == REQ_IMPORTAR) lerBackup(data.getData());
        else if (requestCode == REQ_SALVAR_BACKUP) salvarBackupEm(data.getData());
    }

    private void salvarBackupEm(Uri destino) {
        try {
            OutputStream out = getContentResolver().openOutputStream(destino);
            if (out == null) throw new IOException();
            try {
                out.write(BackupCsv.gerar(db.listarTodas()).getBytes("UTF-8"));
            } finally {
                out.close();
            }
            Toast.makeText(this, "Backup salvo!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            avisar("Backup", "Não foi possível salvar o backup no local escolhido.");
        }
    }

    /** Lê o arquivo fora da thread principal: a fonte pode ser remota (ex.: Google Drive). */
    private void lerBackup(final Uri origem) {
        Toast.makeText(this, "Lendo backup…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                BackupCsv.Leitura lida = null;
                try {
                    InputStream in = getContentResolver().openInputStream(origem);
                    if (in != null) {
                        try {
                            lida = BackupCsv.ler(in);
                        } finally {
                            in.close();
                        }
                    }
                } catch (Exception e) {
                    lida = null;
                }
                final BackupCsv.Leitura leitura = lida;
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (isFinishing()) return;
                        if (leitura == null) {
                            avisar("Importar", "Não foi possível ler o arquivo selecionado.");
                        } else if (leitura.registros.isEmpty()) {
                            avisar("Importar", "O arquivo selecionado não contém atividades válidas. "
                                    + "Escolha um CSV gerado pelo backup deste aplicativo.");
                        } else {
                            confirmarImportacao(leitura);
                        }
                    }
                });
            }
        }).start();
    }

    private void confirmarImportacao(final BackupCsv.Leitura leitura) {
        String msg = atividades(leitura.registros.size()) + " no arquivo.";
        if (leitura.ignoradas > 0) msg += "\n" + leitura.ignoradas + " linha(s) inválida(s) serão ignoradas.";
        msg += "\n\nAdicionar: mantém as atividades atuais e inclui apenas as que ainda não existem."
                + "\nSubstituir: apaga as atividades atuais e fica só com as do arquivo.";
        new AlertDialog.Builder(this)
                .setTitle("Importar atividades")
                .setMessage(msg)
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Substituir", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { confirmarSubstituicao(leitura); }
                })
                .setPositiveButton("Adicionar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { importar(leitura, false); }
                })
                .show();
    }

    private void confirmarSubstituicao(final BackupCsv.Leitura leitura) {
        int atuais = db.contar();
        if (atuais == 0) {
            importar(leitura, true);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Substituir atividades")
                .setMessage(atividades(atuais) + " atualmente no aplicativo. Tudo será apagado e substituído "
                        + "pelo conteúdo do arquivo. Deseja continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Substituir", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { importar(leitura, true); }
                })
                .show();
    }

    private void importar(BackupCsv.Leitura leitura, boolean substituir) {
        int inseridas;
        try {
            inseridas = db.importar(leitura.registros, substituir);
        } catch (RuntimeException e) {
            avisar("Importar", "Falha ao importar. Nenhuma atividade foi alterada.");
            return;
        }
        int repetidas = leitura.registros.size() - inseridas;
        String msg = inseridas == 1 ? "1 atividade importada." : inseridas + " atividades importadas.";
        if (repetidas > 0) msg += "\n" + repetidas + " já existia(m) e não foi(ram) duplicada(s).";
        avisar("Importação concluída", msg);
    }

    private String atividades(int n) {
        return n == 1 ? "1 atividade" : n + " atividades";
    }

    private void avisar(String titulo, String mensagem) {
        new AlertDialog.Builder(this).setTitle(titulo).setMessage(mensagem).setPositiveButton("OK", null).show();
    }

    // ---------- Util ----------

    private void escolherData(final Calendar alvo, final Runnable aoMudar) {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int ano, int mes, int dia) {
                alvo.set(ano, mes, dia);
                txtData.setText(uiFmt.format(dataRegistro.getTime()));
                atualizarBotoesCustom();
                if (aoMudar != null) aoMudar.run();
            }
        }, alvo.get(Calendar.YEAR), alvo.get(Calendar.MONTH), alvo.get(Calendar.DAY_OF_MONTH)).show();
    }
}
