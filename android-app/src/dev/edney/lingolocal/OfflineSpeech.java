package dev.edney.lingolocal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import java.io.*;

/** Modelo Vosk embarcado; PCM e transcrição permanecem na memória do aparelho. */
final class OfflineSpeech {
    interface Listener { void status(String value); void result(String value); void error(String value); }
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    private SpeechService service;
    private Recognizer recognizer;
    private Model model;
    OfflineSpeech(Context context) { this.context=context.getApplicationContext(); }
    static synchronized File installModel(Context context) throws IOException {
        File folder=new File(context.getFilesDir(),"speech-en-0.15");
        File marker=new File(folder,"ready");
        if(!marker.exists()) { copy(context,"speech-model",folder); if(!marker.createNewFile())throw new IOException("Modelo incompleto"); }
        return folder;
    }
    private static void copy(Context context,String asset,File target) throws IOException {
        String[] children=context.getAssets().list(asset);
        if(children!=null && children.length>0) {
            if(!target.isDirectory() && !target.mkdirs())throw new IOException("Sem espaço para o modelo");
            for(String child:children)copy(context,asset+"/"+child,new File(target,child));
        } else try(InputStream in=context.getAssets().open(asset);OutputStream out=new FileOutputStream(target)) {
            byte[] buf=new byte[65536]; int n; while((n=in.read(buf))!=-1)out.write(buf,0,n);
        }
    }
    void start(Listener listener) {
        listener.status("Preparando reconhecimento offline…");
        new Thread(()->{
            try {
                File path=installModel(context);
                Model loaded=new Model(path.getAbsolutePath());
                main.post(()->{
                    if(closed) { loaded.close(); return; }
                    model=loaded;
                    try {
                        recognizer=new Recognizer(model,16000f);
                        service=new SpeechService(recognizer,16000f);
                        listener.status("Ouvindo… toque no microfone para terminar.");
                        service.startListening(new RecognitionListener() {
                            private void accept(String json,boolean finalResult) {
                                if(closed)return;
                                try {
                                    String text=new JSONObject(json).optString("text");
                                    if(!text.isEmpty()) { listener.result(text); close(); }
                                    else if(finalResult) { listener.error("Não consegui ouvir. Tente novamente em um lugar silencioso."); close(); }
                                } catch(Exception ex) { listener.error("Não consegui interpretar o áudio. Tente novamente."); close(); }
                            }
                            public void onPartialResult(String json) {
                                if(closed)return;
                                try { String text=new JSONObject(json).optString("partial"); if(!text.isEmpty())listener.status(text); } catch(Exception ignored) { }
                            }
                            public void onResult(String json) { accept(json,false); }
                            public void onFinalResult(String json) { accept(json,true); }
                            public void onError(Exception ex) { if(!closed)listener.error("Microfone indisponível. Tente novamente ou escolha não falar agora."); close(); }
                            public void onTimeout() { if(!closed && service!=null)service.stop(); }
                        },12000);
                    } catch(Exception|LinkageError ex) { listener.error("Não foi possível iniciar o reconhecimento offline."); close(); }
                });
            } catch(Exception|LinkageError ex) { main.post(()->{ if(!closed)listener.error("Não foi possível carregar o modelo de fala local."); }); }
        },"offline-speech-load").start();
    }
    void finish() { if(service!=null)service.stop(); }
    void close() {
        closed=true;
        if(service!=null) { service.cancel(); service.shutdown(); service=null; }
        if(recognizer!=null) { recognizer.close(); recognizer=null; }
        if(model!=null) { model.close(); model=null; }
    }
}
