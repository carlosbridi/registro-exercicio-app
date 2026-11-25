package br.com.registroexercicio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditarActivity extends Activity {

    public static final String EXTRA_ID = "id";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_MINUTOS = "minutos";

    private static final int VINHO = Color.parseColor("#6D1F2F");

    private final SimpleDateFormat dbFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat uiFmt = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));

    private DbHelper db;
    private long id;
    private String tipo;
    private final Calendar data = Calendar.getInstance();

    private View tileBicicleta, tileEsteira;
    private TextView txtData;
    private EditText editMinutos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar);
        db = new DbHelper(this);

        id = getIntent().getLongExtra(EXTRA_ID, -1);
        tipo = getIntent().getStringExtra(ListaActivity.EXTRA_TIPO);
        try {
            data.setTime(dbFmt.parse(getIntent().getStringExtra(EXTRA_DATA)));
        } catch (ParseException e) {
            // mantém a data de hoje
        }

        ((TextView) findViewById(R.id.txtTitulo)).setText(R.string.editar_atividade);
        ((TextView) findViewById(R.id.txtSub)).setText(R.string.app_name);
        ((TextView) findViewById(R.id.txtSalvar)).setText(R.string.salvar_alteracoes);
        findViewById(R.id.atalhos).setVisibility(View.GONE);
        findViewById(R.id.btnVoltar).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

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
        escolherTipo(tipo);

        txtData.setText(uiFmt.format(data.getTime()));
        findViewById(R.id.btnData).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { escolherData(); }
        });

        editMinutos.setText(String.valueOf(getIntent().getIntExtra(EXTRA_MINUTOS, 0)));
        findViewById(R.id.btnSalvar).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { salvar(); }
        });
        adicionarBotaoExcluir();
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    private void escolherTipo(String novo) {
        tipo = novo;
        marcar(tileBicicleta, DbHelper.TIPO_BICICLETA.equals(novo));
        marcar(tileEsteira, DbHelper.TIPO_ESTEIRA.equals(novo));
    }

    private void marcar(View tile, boolean sel) {
        tile.setSelected(sel);
        ((ImageView) ((android.view.ViewGroup) tile).getChildAt(0)).setColorFilter(sel ? Color.WHITE : VINHO);
    }

    private void escolherData() {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int ano, int mes, int dia) {
                data.set(ano, mes, dia);
                txtData.setText(uiFmt.format(data.getTime()));
            }
        }, data.get(Calendar.YEAR), data.get(Calendar.MONTH), data.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void adicionarBotaoExcluir() {
        Button btn = new Button(this);
        btn.setText("Excluir atividade");
        btn.setAllCaps(false);
        btn.setTextColor(Color.parseColor("#C62828"));
        btn.setBackgroundResource(R.drawable.bg_chip_normal);
        ViewGroup pai = (ViewGroup) findViewById(R.id.btnSalvar).getParent();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (56 * getResources().getDisplayMetrics().density));
        lp.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        pai.addView(btn, lp);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { confirmarExclusao(); }
        });
    }

    private void confirmarExclusao() {
        new AlertDialog.Builder(this)
                .setTitle("Excluir atividade")
                .setMessage("Deseja realmente excluir esta atividade?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        db.excluir(id);
                        Toast.makeText(EditarActivity.this, "Atividade excluída!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                })
                .show();
    }

    private void salvar() {
        String txt = editMinutos.getText().toString().trim();
        int minutos = txt.length() == 0 ? 0 : Integer.parseInt(txt);
        if (minutos <= 0) {
            editMinutos.setError("Informe os minutos");
            return;
        }
        db.atualizar(id, tipo, dbFmt.format(data.getTime()), minutos);
        Toast.makeText(this, "Atividade atualizada!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
