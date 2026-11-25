package br.com.registroexercicio;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ListaActivity extends Activity {

    public static final String EXTRA_TIPO = "tipo";
    public static final String EXTRA_DE = "de";
    public static final String EXTRA_ATE = "ate";

    private static final int REQ_EDITAR = 1;
    private static final int TAMANHO_PAGINA = 10;
    private static final int VINHO = Color.parseColor("#6D1F2F");

    private final SimpleDateFormat dbFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat uiFmt = new SimpleDateFormat("EEE, dd/MM/yyyy", new Locale("pt", "BR"));

    private DbHelper db;
    private LinearLayout lista;
    private String tipo, de, ate;
    private float dp;
    private int offset;
    private String ultimoMes;
    private Map<String, Integer> totaisMes;
    private Button btnMais;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);
        db = new DbHelper(this);
        dp = getResources().getDisplayMetrics().density;

        tipo = getIntent().getStringExtra(EXTRA_TIPO);
        de = getIntent().getStringExtra(EXTRA_DE);
        ate = getIntent().getStringExtra(EXTRA_ATE);

        ((TextView) findViewById(R.id.txtTitulo)).setText(tipo);
        ((TextView) findViewById(R.id.txtSub)).setText(formatar(de) + "  →  " + formatar(ate));
        lista = (LinearLayout) findViewById(R.id.lista);
        findViewById(R.id.btnVoltar).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Edição salva: volta direto para o relatório.
        if (requestCode == REQ_EDITAR && resultCode == RESULT_OK) finish();
    }

    private int px(int v) { return (int) (v * dp + 0.5f); }

    private String formatar(String yyyyMMdd) {
        try {
            return new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR")).format(dbFmt.parse(yyyyMMdd));
        } catch (ParseException e) {
            return yyyyMMdd;
        }
    }

    private String formatarLongo(String yyyyMMdd) {
        try {
            String s = uiFmt.format(dbFmt.parse(yyyyMMdd));
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        } catch (ParseException e) {
            return yyyyMMdd;
        }
    }

    private void carregar() {
        carregarQuantidade(TAMANHO_PAGINA);
    }

    /** Recarrega do início, já trazendo os primeiros {@code quantidade} registros. */
    private void carregarQuantidade(int quantidade) {
        lista.removeAllViews();
        offset = 0;
        btnMais = null;
        ultimoMes = null;
        totaisMes = db.totaisPorMes(tipo, de, ate);
        renderizar(quantidade);
    }

    private void carregarPagina() {
        renderizar(TAMANHO_PAGINA);
    }

    private void renderizar(int limite) {
        if (btnMais != null) {
            lista.removeView(btnMais);
            btnMais = null;
        }
        // Busca um a mais para saber se existem mais registros.
        List<DbHelper.Registro> registros = db.listar(tipo, de, ate, limite + 1, offset);
        boolean temMais = registros.size() > limite;
        if (temMais) registros = registros.subList(0, limite);
        offset += registros.size();
        if (registros.isEmpty() && offset == 0) {
            TextView vazio = new TextView(this);
            vazio.setText("Nenhuma atividade no período.");
            vazio.setTextColor(Color.parseColor("#8A6B70"));
            vazio.setGravity(Gravity.CENTER);
            vazio.setPadding(0, px(40), 0, 0);
            lista.addView(vazio);
            return;
        }
        for (final DbHelper.Registro r : registros) {
            String mes = r.data.substring(0, 7);
            if (!mes.equals(ultimoMes)) {
                adicionarSeparador(mes);
                ultimoMes = mes;
            }
            LinearLayout card = new LinearLayout(this);
            card.setBackgroundResource(R.drawable.bg_card);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setClickable(true);
            card.setPadding(px(16), px(14), px(12), px(14));

            ImageView icone = new ImageView(this);
            icone.setImageResource(R.drawable.ic_edit);
            icone.setColorFilter(VINHO);
            icone.setBackgroundResource(R.drawable.bg_icone);
            icone.setPadding(px(9), px(9), px(9), px(9));
            card.addView(icone, new LinearLayout.LayoutParams(px(40), px(40)));

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            colp.leftMargin = px(14);
            card.addView(col, colp);

            TextView data = new TextView(this);
            data.setText(formatarLongo(r.data));
            data.setTextColor(Color.parseColor("#3B1F25"));
            data.setTextSize(15);
            data.setTypeface(null, Typeface.BOLD);
            col.addView(data);

            TextView min = new TextView(this);
            min.setText(r.minutos + " min");
            min.setTextColor(VINHO);
            min.setTextSize(14);
            col.addView(min);

            ImageView seta = new ImageView(this);
            seta.setImageResource(R.drawable.ic_chevron);
            seta.setColorFilter(Color.parseColor("#8A6B70"));
            card.addView(seta, new LinearLayout.LayoutParams(px(24), px(24)));

            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(ListaActivity.this, EditarActivity.class);
                    i.putExtra(EditarActivity.EXTRA_ID, r.id);
                    i.putExtra(ListaActivity.EXTRA_TIPO, r.tipo);
                    i.putExtra(EditarActivity.EXTRA_DATA, r.data);
                    i.putExtra(EditarActivity.EXTRA_MINUTOS, r.minutos);
                    startActivityForResult(i, REQ_EDITAR);
                }
            });
            adicionarComSwipe(card, r);
        }
        if (temMais) {
            btnMais = new Button(this);
            btnMais.setText("Carregar mais");
            btnMais.setAllCaps(false);
            btnMais.setTextColor(Color.WHITE);
            btnMais.setBackgroundResource(R.drawable.bg_btn_normal);
            btnMais.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { carregarPagina(); }
            });
            lista.addView(btnMais, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private String nomeMes(String yyyyMM) {
        try {
            String s = new SimpleDateFormat("MMMM 'de' yyyy", new Locale("pt", "BR"))
                    .format(new SimpleDateFormat("yyyy-MM", Locale.US).parse(yyyyMM));
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        } catch (ParseException e) {
            return yyyyMM;
        }
    }

    private String formatarMinutos(int min) {
        if (min < 60) return min + " min";
        return (min / 60) + "h " + (min % 60) + "min";
    }

    private void adicionarSeparador(String mes) {
        Integer total = totaisMes.get(mes);

        LinearLayout linha = new LinearLayout(this);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setPadding(px(4), lista.getChildCount() == 0 ? 0 : px(8), px(4), px(8));

        TextView nome = new TextView(this);
        nome.setText(nomeMes(mes));
        nome.setTextColor(Color.parseColor("#3B1F25"));
        nome.setTextSize(16);
        nome.setTypeface(null, Typeface.BOLD);
        linha.addView(nome, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView soma = new TextView(this);
        soma.setText(formatarMinutos(total == null ? 0 : total));
        soma.setTextColor(VINHO);
        soma.setTextSize(15);
        soma.setTypeface(null, Typeface.BOLD);
        linha.addView(soma);

        lista.addView(linha, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /** Envolve o card num fundo vermelho e permite arrastar para a esquerda para excluir. */
    private void adicionarComSwipe(final View card, final DbHelper.Registro r) {
        final FrameLayout wrapper = new FrameLayout(this);
        wrapper.setBackgroundResource(R.drawable.bg_excluir);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wp.bottomMargin = px(10);
        wrapper.setLayoutParams(wp);

        TextView rotulo = new TextView(this);
        rotulo.setText("Excluir");
        rotulo.setTextColor(Color.WHITE);
        rotulo.setTextSize(15);
        rotulo.setTypeface(null, Typeface.BOLD);
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        rp.rightMargin = px(24);
        wrapper.addView(rotulo, rp);
        wrapper.addView(card, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        card.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            boolean arrastando;

            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX();
                        downY = e.getRawY();
                        arrastando = false;
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - downX;
                        if (!arrastando && dx < -slop && Math.abs(dx) > Math.abs(e.getRawY() - downY)) {
                            arrastando = true;
                            v.setPressed(false);
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        if (arrastando) {
                            v.setTranslationX(Math.min(0, dx));
                            return true;
                        }
                        return false;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!arrastando) return false;
                        arrastando = false;
                        if (e.getActionMasked() == MotionEvent.ACTION_UP
                                && -v.getTranslationX() > v.getWidth() / 3f) {
                            confirmarExclusao(wrapper, v, r);
                        } else {
                            v.animate().translationX(0).setDuration(150);
                        }
                        return true;
                }
                return false;
            }
        });
        lista.addView(wrapper);
    }

    private void confirmarExclusao(final View wrapper, final View card, final DbHelper.Registro r) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir atividade")
                .setMessage("Deseja realmente excluir esta atividade?")
                .setCancelable(false)
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        card.animate().translationX(0).setDuration(150);
                    }
                })
                .setPositiveButton("Excluir", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        db.excluir(r.id);
                        // Recarrega mantendo a quantidade já exibida, para atualizar os totais.
                        carregarQuantidade(Math.max(offset - 1, TAMANHO_PAGINA));
                    }
                })
                .show();
    }
}
