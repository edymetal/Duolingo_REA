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
import java.time.LocalDate;
import java.util.*;

public class MainActivity extends Activity {
    private final int INK = Color.rgb(241, 247, 250), GREEN = Color.rgb(88, 204, 2);
    private final int MUTED = Color.rgb(147, 167, 176), BG = Color.rgb(19, 31, 36);
    private Typeface regular, bold;
    private ExerciseScreen exerciseScreen;
    private OfflineSpeech speech;
    private JSONObject unit;
    private JSONArray lessons;
    private SharedPreferences prefs;
    private LinearLayout root;
    private MediaPlayer player;
    private ArrayList<JSONObject> questions = new ArrayList<>();
    private int lessonIndex = -1, step, correct;
    private boolean checked, answeredCorrect, review;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        regular=Typeface.createFromAsset(getAssets(),"fonts/regular.otf");
        bold=Typeface.createFromAsset(getAssets(),"fonts/bold.otf");
        prefs = getSharedPreferences("learning", MODE_PRIVATE);
        try {
            unit = new JSONObject(readAsset("unit.json"));
            lessons = unit.getJSONArray("lessons");
            lessons.put(new JSONObject(readAsset("reference-activities.json")));
            if (state != null && state.getBoolean("inLesson")) {
                lessonIndex=state.getInt("lessonIndex"); review=state.getBoolean("review");
                ArrayList<String> savedIds=state.getStringArrayList("sessionIds");
                if(savedIds==null) throw new IllegalStateException("Sessão sem exercícios");
                ArrayList<String> savedJson=state.getStringArrayList("sessionJson");
                if(savedJson!=null)for(String json:savedJson)questions.add(new JSONObject(json));
                else for(String id:savedIds) {
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
        view.setTypeface(bold ? this.bold : regular);
        view.setPadding(0, dp(5), 0, dp(5)); return view;
    }
    private Button button(String text, boolean primary) {
        Button view = new Button(this); view.setText(text); view.setAllCaps(false); view.setTextSize(16);
        view.setTypeface(bold); view.setTextColor(primary ? Color.rgb(25,57,0) : INK);
        view.setBackground(shape(primary ? GREEN : BG, primary ? 0 : Color.rgb(55,70,79)));
        view.setMinHeight(dp(54)); view.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(6), 0, dp(6));
        view.setLayoutParams(lp); return view;
    }
    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private void base() {
        stopAudio(); stopSpeech(); if(exerciseScreen!=null){exerciseScreen.dispose();exerciseScreen=null;} root = column(); root.setBackgroundColor(BG); root.setPadding(dp(22), dp(12), dp(22), dp(12)); setContentView(root);
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
        root.setPadding(0,0,0,0);
        boolean[] finished=new boolean[lessons.length()]; for(int i=0;i<finished.length;i++) finished[i]=prefs.getBoolean("done-"+i,false);
        String[] titles=new String[lessons.length()]; for(int i=0;i<titles.length;i++) titles[i]=lessons.optJSONObject(i).optString("title");
        root.addView(new LearningPath(this,regular,bold,unit.optString("title"),titles,finished,prefs.getInt("xp",0),prefs.getInt("streak",0),new LearningPath.Actions() {
            public void lesson(int index) { showLessonIntro(index); }
            public void activities() { showLessonIntro(3); }
            public void guide() {
                new AlertDialog.Builder(MainActivity.this).setTitle("Guia · Bebidas e pedidos")
                    .setMessage(unit.optString("subtitle")+"\n\n1. Faça seu pedido\n2. Ofereça uma bebida\n3. Gostos e hábitos\n\nCada lição tem tradução, escuta e montagem de frases. A quarta estrela abre a prática Cores e roupas; os dois últimos botões repetem lições. Todos os áudios são locais.\n\nPrática extra: Cores e roupas, com sete formatos de atividade. Abra pelo ícone de prática na barra inferior.")
                    .setPositiveButton("Entendi",null).show();
            }
            public void progress() {
                StringBuilder message=new StringBuilder(); int done=0;
                for(int i=0;i<lessons.length();i++) {
                    boolean finished=prefs.getBoolean("done-"+i,false); if(finished)done++;
                    message.append(lessons.optJSONObject(i).optString("title")).append(finished?" · melhor: "+prefs.getInt("best-"+i,0)+"/"+lessons.optJSONObject(i).optJSONArray("exercises").length():" · não concluída").append("\n\n");
                }
                new AlertDialog.Builder(MainActivity.this).setTitle(done+" de "+lessons.length()+" lições · "+prefs.getInt("xp",0)+" XP").setMessage(message.toString()).setPositiveButton("Voltar",null).show();
            }
            public void review() {
                if(mistakes().isEmpty()) new AlertDialog.Builder(MainActivity.this).setTitle("Revisar meus erros · 0").setMessage("Nenhum erro pendente. Continue praticando as lições da trilha.").setPositiveButton("Entendi",null).show();
                else startLesson(0,true);
            }
            public void about() { showAbout(); }
        }),new LinearLayout.LayoutParams(-1,-1));
    }
    private void showAbout() {
        new AlertDialog.Builder(this).setTitle("Conteúdo que funciona sem rede")
            .setMessage("As três primeiras lições têm 18 frases do STAPLE 2020, publicadas pelo Duolingo no Harvard Dataverse (V6).\n\nOrganização, explicações e tipos de exercício: curadoria própria do projeto. Não é a unidade original nem o curso completo do Duolingo.\n\nÁudio sintético gerado localmente com Microsoft Zira Desktop; não são as vozes originais.\n\nOrigem: doi:10.7910/DVN/38OJR6\nLicença do conteúdo: CC BY-NC 4.0, com atribuição e uso não comercial.\n\nPrática extra: sete atividades de cores e roupas, organizadas a partir das suas referências.\n\nFala: Vosk com modelo inglês embarcado; compara a transcrição reconhecida. Não é a avaliação de pronúncia do Duolingo. Microfone é solicitado somente ao gravar.\n\nSem conta e sem permissão de internet. O progresso fica neste aplicativo no aparelho.")
            .setNeutralButton("Licenças",(dialog,which)->showLicenses()).setPositiveButton("Entendi", null).show();
    }
    private void showLicenses(){
        try {new AlertDialog.Builder(this).setTitle("Componentes e licenças").setMessage(readAsset("THIRD-PARTY-NOTICES.txt")+"\n\n"+readAsset("licenses/Apache-2.0.txt")+"\n\n"+readAsset("licenses/Rive-MIT.txt")+"\n\n"+readAsset("licenses/JNA-LICENSE.txt")).setPositiveButton("Voltar",null).show();}
        catch(IOException ex){Toast.makeText(this,"Não foi possível abrir as licenças.",Toast.LENGTH_LONG).show();}
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
        base(); checked=false;
        JSONObject source=questions.get(step), q;
        try { q=new JSONObject(source.toString()); } catch(JSONException ex){throw new IllegalStateException(ex);}
        String type=q.optString("type");
        try {
            if(type.equals("translation")||type.equals("listening")) {
                q.put("prompt",q.optString("english")).put("answer",q.optString("portuguese"));
                if(type.equals("translation"))q.put("type","choice");
                ArrayList<String> choices=new ArrayList<>();choices.add(cap(q.optString("portuguese")));
                for(int l=0;l<lessons.length()&&choices.size()<3;l++){
                    JSONArray entries=lessons.optJSONObject(l).optJSONArray("exercises");
                    for(int i=0;i<entries.length()&&choices.size()<3;i++){
                        String value=cap(entries.optJSONObject(i).optString("portuguese"));
                        if(!value.isEmpty()&&!choices.contains(value))choices.add(value);
                    }
                }
                Collections.shuffle(choices,new Random(q.optString("id").hashCode()));q.put("choices",new JSONArray(choices));
            } else if(type.equals("word_order")) {
                q.put("prompt",q.optString("portuguese")).put("answer",q.optString("english"));
                ArrayList<String> words=new ArrayList<>(Arrays.asList(q.optString("english").split("\\s+")));
                Collections.shuffle(words,new Random(q.optString("id").hashCode()));q.put("tokens",new JSONArray(words));
            }
        } catch(JSONException ex){throw new IllegalStateException(ex);}
        root.setPadding(0,0,0,0);
        exerciseScreen=new ExerciseScreen(this,regular,bold,q,step,questions.size(),new ExerciseScreen.Actions(){
            public void exit(){confirmExit();}
            public void audio(){playAudio(source.optString("audio"));}
            public void checked(boolean right){checked=true;answeredCorrect=right;if(right)correct++;updateMistake(source.optString("id"),!right);}
            public void next(){stopSpeech();step++;if(step>=questions.size())finishLesson();else showQuestion();}
            public void speak(){startSpeech();}
            public void alternate(){
                stopSpeech();
                try {JSONObject fallback=new JSONObject(source.toString());fallback.put("type","choice").put("prompt","Ouça a frase e escolha o significado:").put("answer",source.optString("fallback_answer"));fallback.put("character","oscar");questions.set(step,fallback);showQuestion();playAudio(source.optString("audio"));}
                catch(JSONException ex){throw new IllegalStateException(ex);}
            }
        });
        root.addView(exerciseScreen,new LinearLayout.LayoutParams(-1,-1));
    }
    private void startSpeech(){
        stopAudio();
        if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},41);return;}
        if(speech!=null){speech.finish();return;}
        speech=new OfflineSpeech(this);
        speech.start(new OfflineSpeech.Listener(){
            public void status(String value){if(exerciseScreen!=null)exerciseScreen.speechStatus(value);}
            public void result(String value){speech=null;if(exerciseScreen!=null)exerciseScreen.speechResult(value);}
            public void error(String value){speech=null;if(exerciseScreen!=null)exerciseScreen.speechStatus(value);}
        });
    }
    private void stopSpeech(){if(speech!=null){speech.close();speech=null;}}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==41&&exerciseScreen!=null){
            if(results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)startSpeech();
            else exerciseScreen.speechStatus("Microfone não autorizado. Você pode tentar novamente ou escolher não falar agora.");
        }
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
    @Override protected void onPause() { super.onPause(); stopAudio(); stopSpeech(); }
    @Override protected void onDestroy(){stopSpeech();if(exerciseScreen!=null)exerciseScreen.dispose();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putBoolean("inLesson",lessonIndex>=0); state.putInt("lessonIndex",lessonIndex);
        ArrayList<String> savedIds=new ArrayList<>(); for(JSONObject question:questions) savedIds.add(question.optString("id")); state.putStringArrayList("sessionIds",savedIds);
        ArrayList<String> sessionJson=new ArrayList<>();for(JSONObject question:questions)sessionJson.add(question.toString());state.putStringArrayList("sessionJson",sessionJson);
        state.putInt("step",checked?step+1:step); state.putInt("correct",correct); state.putBoolean("review",review);
    }
}
