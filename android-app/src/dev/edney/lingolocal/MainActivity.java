package dev.edney.lingolocal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;

public class MainActivity extends Activity {
    private final int INK = Color.rgb(31, 51, 43), GREEN = Color.rgb(37, 107, 79);
    private final int MUTED = Color.rgb(100, 115, 105), BG = Color.rgb(250, 250, 246);
    private JSONObject unit;
    private JSONArray lessons;
    private SharedPreferences prefs;
    private LinearLayout root, content, response;
    private Button action;
    private TextView feedback, assembled;
    private MediaPlayer player;
    private ArrayList<JSONObject> questions = new ArrayList<>();
    private ArrayList<Integer> selectedTokens = new ArrayList<>();
    private ArrayList<Button> tokenButtons = new ArrayList<>();
    private String[] tokens;
    private int lessonIndex = -1, step, correct;
    private boolean checked, answeredCorrect, review;
    private String chosen = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("learning", MODE_PRIVATE);
        try {
            unit = new JSONObject(readAsset("unit.json"));
            lessons = unit.getJSONArray("lessons");
            if (state != null && state.getBoolean("inLesson")) {
                lessonIndex=state.getInt("lessonIndex"); review=state.getBoolean("review");
                ArrayList<String> savedIds=state.getStringArrayList("sessionIds");
                if(savedIds==null) throw new IllegalStateException("Sessão sem exercícios");
                for(String id:savedIds) {
                    for(int l=0;l<lessons.length();l++) {
                        JSONArray entries=lessons.getJSONObject(l).getJSONArray("exercises");
                        for(int i=0;i<entries.length();i++) if(entries.getJSONObject(i).getString("id").equals(id)) questions.add(entries.getJSONObject(i));
                    }
                }
                step = Math.min(state.getInt("step"), questions.size());
                correct = state.getInt("correct");
                if (questions.isEmpty()) showHome(); else if(step>=questions.size()) finishLesson(); else showQuestion();
            } else showHome();
        } catch (Exception ex) {
            base();
            root.addView(label("Não foi possível carregar a unidade.", 22, INK, true));
            root.addView(label("Os arquivos da unidade estão ausentes ou inválidos. Recrie o APK pelo script do projeto.", 16, MUTED, false));
        }
    }

    private String readAsset(String path) throws IOException {
        try (InputStream in = getAssets().open(path); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    private int dp(float n) { return (int)(getResources().getDisplayMetrics().density * n + .5f); }
    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(18));
        if (stroke != 0) bg.setStroke(dp(1), stroke); return bg;
    }
    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        view.setPadding(0, dp(5), 0, dp(5)); return view;
    }
    private Button button(String text, boolean primary) {
        Button view = new Button(this); view.setText(text); view.setAllCaps(false); view.setTextSize(16);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); view.setTextColor(primary ? Color.WHITE : INK);
        view.setBackground(shape(primary ? GREEN : Color.WHITE, primary ? 0 : Color.rgb(217, 225, 218)));
        view.setMinHeight(dp(54)); view.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(6), 0, dp(6));
        view.setLayoutParams(lp); return view;
    }
    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private void base() {
        stopAudio(); root = column(); root.setBackgroundColor(BG); root.setPadding(dp(22), dp(12), dp(22), dp(12)); setContentView(root);
    }
    private LinearLayout scrollContent() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout layout = column(); scroll.addView(layout); return layout;
    }
    private String cap(String value) { return value.length() == 0 ? value : value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1); }
    private Set<String> mistakes() { return new HashSet<>(prefs.getStringSet("mistakes", new HashSet<String>())); }
    private void updateMistake(String id, boolean wrong) {
        Set<String> values = mistakes(); if (wrong) values.add(id); else values.remove(id);
        prefs.edit().putStringSet("mistakes", values).apply();
    }
    private void showHome() {
        lessonIndex = -1; questions.clear(); base();
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = label("Lingo Local", 26, INK, true); header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView pill = label("● Offline", 13, GREEN, true); header.addView(pill); root.addView(header);
        root.addView(label("INGLÊS  /  PORTUGUÊS", 11, MUTED, true));
        content = scrollContent();
        int completed = 0; for (int i=0; i<lessons.length(); i++) if (prefs.getBoolean("done-"+i, false)) completed++;
        int streak=prefs.getInt("streak",0);
        TextView stats = label(completed + " de 3 lições   ·   " + prefs.getInt("xp", 0) + " XP   ·   " + streak + (streak==1?" dia":" dias"), 14, GREEN, true);
        stats.setPadding(0, dp(18), 0, dp(14)); content.addView(stats);
        LinearLayout hero = column(); hero.setPadding(dp(20), dp(16), dp(20), dp(18)); hero.setBackground(shape(Color.rgb(229, 240, 223), 0));
        hero.addView(label("UNIDADE 01", 12, GREEN, true)); hero.addView(label(unit.optString("title"), 30, INK, true));
        hero.addView(label(unit.optString("subtitle"), 16, INK, false)); content.addView(hero);
        content.addView(label("Um pequeno passo, todos os dias.", 15, MUTED, false));
        for (int i=0; i<lessons.length(); i++) {
            final int index = i; JSONObject lesson = lessons.optJSONObject(i);
            boolean done = prefs.getBoolean("done-"+i, false);
            String score = done ? "  ·  melhor: " + prefs.getInt("best-"+i,0) + "/6" : "  ·  6 exercícios";
            Button card = button((done ? "✓  " : (i+1)+"  ") + lesson.optString("title") + "\n" + lesson.optString("description") + "\n" + score.trim(), false);
            card.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); card.setTextSize(15); card.setMinHeight(dp(104));
            card.setOnClickListener(v -> showLessonIntro(index)); content.addView(card);
        }
        Button revision = button("Revisar meus erros  ·  " + mistakes().size(), true);
        revision.setEnabled(!mistakes().isEmpty()); revision.setAlpha(mistakes().isEmpty() ? .45f : 1f);
        revision.setOnClickListener(v -> startLesson(0,true)); content.addView(revision);
        Button about = button("Sobre esta unidade", false); about.setOnClickListener(v -> showAbout()); content.addView(about);
    }
    private void showAbout() {
        new AlertDialog.Builder(this).setTitle("Conteúdo que funciona sem rede")
            .setMessage("Esta unidade tem 18 frases do STAPLE 2020, publicadas pelo Duolingo no Harvard Dataverse (V6).\n\nOrganização, explicações e tipos de exercício: curadoria própria do projeto. Não é a unidade original nem o curso completo do Duolingo.\n\nÁudio sintético gerado localmente com Microsoft Zira Desktop; não são as vozes originais.\n\nOrigem: doi:10.7910/DVN/38OJR6\nLicença do conteúdo: CC BY-NC 4.0, com atribuição e uso não comercial.\n\nSem conta e sem permissão de internet. O progresso fica neste aplicativo no aparelho.")
            .setPositiveButton("Entendi", null).show();
    }
    private void showLessonIntro(int index) {
        JSONObject lesson = lessons.optJSONObject(index);
        new AlertDialog.Builder(this).setTitle(lesson.optString("title")).setMessage(lesson.optString("tip"))
            .setPositiveButton("Começar", (dialog, which) -> startLesson(index,false)).setNegativeButton("Voltar", null).show();
    }
    private void startLesson(int index, boolean isReview) {
        lessonIndex = index; review = isReview; questions = new ArrayList<>();
        if (isReview) {
            Set<String> missed = mistakes();
            for (int l=0; l<lessons.length(); l++) {
                JSONArray values = lessons.optJSONObject(l).optJSONArray("exercises");
                for (int i=0; i<values.length(); i++) if (missed.contains(values.optJSONObject(i).optString("id"))) questions.add(values.optJSONObject(i));
            }
        } else {
            JSONArray values = lessons.optJSONObject(index).optJSONArray("exercises");
            for (int i=0; i<values.length(); i++) questions.add(values.optJSONObject(i));
        }
        step=0; correct=0;
        if (questions.isEmpty()) showHome(); else showQuestion();
    }
    private void showQuestion() {
        base(); chosen=""; checked=false; selectedTokens.clear(); tokenButtons.clear();
        JSONObject question = questions.get(step); String type = question.optString("type");
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        Button close = button("×", false); close.setContentDescription("Sair da lição");
        header.addView(close,new LinearLayout.LayoutParams(dp(48),dp(48)));
        close.setOnClickListener(v -> confirmExit());
        TextView progress = label("   " + (step+1) + " / " + questions.size() + (review ? "  ·  Revisão" : "  ·  Lição " + (lessonIndex+1)),14,MUTED,true);
        header.addView(progress); root.addView(header);
        ProgressBar bar = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); bar.setMax(questions.size()); bar.setProgress(step);
        LinearLayout.LayoutParams barParams=new LinearLayout.LayoutParams(-1,dp(8)); barParams.setMargins(0,dp(16),0,dp(12)); root.addView(bar,barParams);
        content=scrollContent();
        boolean listening = type.equals("listening"); boolean order = type.equals("word_order");
        content.addView(label(listening ? "Ouça e escolha" : order ? "Monte a frase em inglês" : "Escolha a tradução",25,INK,true));
        content.addView(label(listening ? "Use o áudio para entender a frase." : order ? "Toque nas palavras na ordem certa." : "Leia em inglês e encontre o sentido.",15,MUTED,false));
        TextView prompt=label(listening ? "♪" : cap(question.optString(order ? "portuguese" : "english")), listening ? 58 : 24,INK,true);
        prompt.setPadding(dp(20),dp(22),dp(20),dp(22)); prompt.setBackground(shape(Color.WHITE,Color.rgb(220,228,220))); content.addView(prompt);
        Button audio=button(order ? "Ouvir uma dica" : "▶  Ouvir em inglês",false); audio.setOnClickListener(v -> playAudio(question.optString("audio"))); content.addView(audio);
        response=column(); content.addView(response);
        if (order) buildWordOrder(question); else buildChoices(question);
        feedback=label("",15,INK,true); feedback.setPadding(dp(12),dp(10),dp(12),dp(10)); root.addView(feedback);
        action=button("Verificar",true); action.setEnabled(false); action.setAlpha(.45f); root.addView(action);
        action.setOnClickListener(v -> {
            if (!checked) checkAnswer(question,order);
            else { step++; if (step>=questions.size()) finishLesson(); else showQuestion(); }
        });
    }
    private void buildChoices(JSONObject question) {
        ArrayList<String> choices=new ArrayList<>(); choices.add(question.optString("portuguese"));
        JSONArray pool=lessons.optJSONObject(lessonIndex).optJSONArray("exercises");
        for (int i=0;i<pool.length() && choices.size()<4;i++) {
            String alternative=pool.optJSONObject((step+i+1)%pool.length()).optString("portuguese");
            if (!choices.contains(alternative)) choices.add(alternative);
        }
        Collections.shuffle(choices,new Random(question.optString("id").hashCode()));
        for (String value:choices) {
            Button option=button(cap(value),false); option.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
            option.setOnClickListener(v -> {
                if (checked) return;
                chosen=value;
                for (int i=0;i<response.getChildCount();i++) response.getChildAt(i).setBackground(shape(Color.WHITE,Color.rgb(217,225,218)));
                option.setBackground(shape(Color.rgb(226,239,223),GREEN)); enableCheck();
            }); response.addView(option);
        }
    }
    private void buildWordOrder(JSONObject question) {
        tokens=question.optString("english").split("\\s+");
        assembled=label("Sua frase aparece aqui",20,INK,true); assembled.setMinHeight(dp(72)); response.addView(assembled);
        ArrayList<Integer> indices=new ArrayList<>(); for(int i=0;i<tokens.length;i++) indices.add(i);
        Collections.shuffle(indices,new Random(question.optString("id").hashCode()));
        LinearLayout row=null;
        for(int position=0;position<indices.size();position++) {
            if(position%3==0) { row=new LinearLayout(this); response.addView(row); }
            final int index=indices.get(position); Button token=button(tokens[index],false);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(dp(3),dp(4),dp(3),dp(4)); row.addView(token,lp); tokenButtons.add(token);
            token.setOnClickListener(v -> { if(checked) return; selectedTokens.add(index); token.setEnabled(false); token.setAlpha(.35f); updateAssembled(); });
        }
        Button undo=button("↶  Desfazer",false); undo.setOnClickListener(v -> {
            if(checked || selectedTokens.isEmpty()) return; int removed=selectedTokens.remove(selectedTokens.size()-1);
            for(Button token:tokenButtons) if(token.getTag()!=null && token.getTag().equals(removed)) { token.setEnabled(true); token.setAlpha(1); }
            updateAssembled();
        });
        for(int i=0;i<tokenButtons.size();i++) tokenButtons.get(i).setTag(indices.get(i)); response.addView(undo);
    }
    private void updateAssembled() {
        StringBuilder value=new StringBuilder(); for(int index:selectedTokens) { if(value.length()>0) value.append(' '); value.append(tokens[index]); }
        chosen=value.toString(); assembled.setText(chosen.isEmpty()?"Sua frase aparece aqui":cap(chosen));
        boolean complete=selectedTokens.size()==tokens.length; action.setEnabled(complete); action.setAlpha(complete?1f:.45f);
    }
    private void enableCheck() { action.setEnabled(true); action.setAlpha(1); }
    private String normalize(String text) {
        return Normalizer.normalize(text,Normalizer.Form.NFC).toLowerCase(Locale.ROOT).replaceAll("[\\p{Punct}’]", "").replaceAll("\\s+"," ").trim();
    }
    private void checkAnswer(JSONObject question,boolean order) {
        answeredCorrect=normalize(chosen).equals(normalize(question.optString(order?"english":"portuguese")));
        if(answeredCorrect) correct++;
        updateMistake(question.optString("id"),!answeredCorrect); checked=true;
        feedback.setText((answeredCorrect?"✓ Muito bem!":"Vamos aprender juntos.") + "\n" + cap(question.optString("english")) + "\n" + cap(question.optString("portuguese")));
        feedback.setBackground(shape(answeredCorrect?Color.rgb(223,239,222):Color.rgb(255,237,212),0));
        action.setText("Continuar");
    }
    private void finishLesson() {
        if(!review) {
            SharedPreferences.Editor edit=prefs.edit();
            if(!prefs.getBoolean("done-"+lessonIndex,false)) edit.putInt("xp",prefs.getInt("xp",0)+15);
            edit.putBoolean("done-"+lessonIndex,true).putInt("best-"+lessonIndex,Math.max(correct,prefs.getInt("best-"+lessonIndex,0)));
            String today=LocalDate.now().toString(),last=prefs.getString("last-day","");
            if(!today.equals(last)) edit.putInt("streak",LocalDate.now().minusDays(1).toString().equals(last)?prefs.getInt("streak",0)+1:1).putString("last-day",today);
            edit.apply();
        }
        base(); LinearLayout body=scrollContent(); body.setGravity(Gravity.CENTER);
        body.addView(label("✓",72,GREEN,true)); body.addView(label(review?"Revisão concluída":"Lição concluída!",30,INK,true));
        body.addView(label(correct+" de "+questions.size()+" respostas corretas",20,INK,true));
        int pending=mistakes().size();
        body.addView(label("O progresso foi salvo neste aparelho.\n"+pending+(pending==1?" exercício aguarda revisão.":" exercícios aguardam revisão."),16,MUTED,false));
        Button home=button("Voltar à unidade",true); root.addView(home); home.setOnClickListener(v -> showHome());
        lessonIndex=-1; questions.clear();
    }
    private void playAudio(String path) {
        stopAudio();
        try(AssetFileDescriptor fd=getAssets().openFd(path)) {
            player=new MediaPlayer(); player.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
            player.setOnCompletionListener(mp -> stopAudio()); player.prepare(); player.start();
        } catch(Exception ex) { stopAudio(); Toast.makeText(this,"Não foi possível tocar o áudio local.",Toast.LENGTH_LONG).show(); }
    }
    private void stopAudio() { if(player!=null) { player.release(); player=null; } }
    private void confirmExit() {
        new AlertDialog.Builder(this).setTitle("Voltar à unidade?").setMessage("Os exercícios já corrigidos ficam na revisão. A pontuação desta tentativa não será concluída.")
            .setPositiveButton("Voltar",(dialog,which)->showHome()).setNegativeButton("Continuar lição",null).show();
    }
    @Override public void onBackPressed() { if(lessonIndex>=0) confirmExit(); else super.onBackPressed(); }
    @Override protected void onPause() { super.onPause(); stopAudio(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putBoolean("inLesson",lessonIndex>=0); state.putInt("lessonIndex",lessonIndex);
        ArrayList<String> savedIds=new ArrayList<>(); for(JSONObject question:questions) savedIds.add(question.optString("id")); state.putStringArrayList("sessionIds",savedIds);
        state.putInt("step",checked?step+1:step); state.putInt("correct",correct); state.putBoolean("review",review);
    }
}
