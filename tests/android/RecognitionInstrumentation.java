package dev.edney.lingolocal.tests;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.*;

/** Testa o motor Android real com WAV PCM local; não simula fala humana. */
public class RecognitionInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            Context context=getTargetContext();
            File modelFolder=new File(context.getFilesDir(),"speech-en-0.15");
            if(!new File(modelFolder,"ready").exists())throw new IOException("Inicie primeiro o microfone na atividade de fala para instalar o modelo.");
            JSONObject reference=new JSONObject(read(context,"reference-activities.json"));
            String path=reference.getJSONArray("exercises").getJSONObject(2).getString("audio");
            try(Model model=new Model(modelFolder.getAbsolutePath())) {
                String recognized=decode(context,model,path);
                if(!recognized.contains("green")||!recognized.contains("shirt"))throw new IOException("Transcrição inesperada: "+recognized);
                result.putString("recognition",recognized);
                String negative=decode(context,model,reference.getJSONArray("exercises").getJSONObject(0).getString("audio"));
                if(negative.equals("that green shirt is nice"))throw new IOException("Áudio diferente foi aceito como frase esperada");
                result.putString("different_audio",negative);
                try(Recognizer silence=new Recognizer(model,16000f)) {
                    byte[] zeros=new byte[64000];silence.acceptWaveForm(zeros,zeros.length);
                    if(!new JSONObject(silence.getFinalResult()).optString("text").isEmpty())throw new IOException("Silêncio produziu frase");
                }
            }
            result.putString("success","true");finish(-1,result);
        } catch(Throwable error) {result.putString("error",error.toString());finish(0,result);}
    }
    private String read(Context c,String path)throws Exception{
        try(InputStream in=c.getAssets().open(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new String(out.toByteArray(),"UTF-8");}
    }
    private int littleInt(DataInputStream in)throws IOException{return Integer.reverseBytes(in.readInt());}
    private String decode(Context c,Model model,String path)throws Exception {
        try(DataInputStream in=new DataInputStream(c.getAssets().open(path))) {
            in.skipBytes(12);int rate=0;
            while(true){byte[] name=new byte[4];in.readFully(name);int size=littleInt(in);String chunk=new String(name,"US-ASCII");
                if(chunk.equals("fmt ")){in.skipBytes(4);rate=littleInt(in);in.skipBytes(size-8+(size%2));}
                else if(chunk.equals("data")) {
                    if(rate==0)throw new IOException("WAV sem formato");
                    try(Recognizer recognizer=new Recognizer(model,rate)) {
                        byte[] data=new byte[4096];StringBuilder words=new StringBuilder();int left=size;
                        while(left>0){int n=in.read(data,0,Math.min(left,data.length));if(n<0)break;left-=n;if(recognizer.acceptWaveForm(data,n))words.append(new JSONObject(recognizer.getResult()).optString("text")).append(' ');}
                        return (words+new JSONObject(recognizer.getFinalResult()).optString("text")).trim();
                    }
                } else in.skipBytes(size+(size%2));
            }
        }
    }
}
