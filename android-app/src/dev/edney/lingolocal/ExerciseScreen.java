package dev.edney.lingolocal;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.text.style.ReplacementSpan;
import org.json.*;
import java.util.*;

/** Atividades nativas, com a composição das referências e correção local. */
final class ExerciseScreen extends LinearLayout {
    interface Actions { void exit(); void audio(); void checked(boolean correct); void next(); void speak(); void alternate(); }
    private final Actions actions; private final JSONObject question; private final Typeface regular,bold;
    private final int bg=0xff131f24,ink=0xfff1f7fa,border=0xff37464f,blue=0xff49c0f8,green=0xff58cc02;
    private final String type; private String chosen=""; private boolean checked, pairError;
    private final ArrayList<Button> options=new ArrayList<>();
    private final ArrayList<Integer> selected=new ArrayList<>();
    private final ArrayList<CharacterView> characters=new ArrayList<>();
    private final ArrayList<Button> leftPairs=new ArrayList<>(),rightPairs=new ArrayList<>();
    private int left=-1,right=-1,pairsDone;
    private Button action; private TextView feedback,speechStatus; private LinearLayout body; private ChipFlow assembled;
    ExerciseScreen(Context context,Typeface regular,Typeface bold,JSONObject q,int step,int total,Actions actions) {
        super(context);this.actions=actions;this.question=q;this.regular=regular;this.bold=bold;type=q.optString("type");
        setOrientation(VERTICAL);setPadding(d(16),d(13),d(16),d(12));setBackgroundColor(bg);
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView exit=text("×",42,0xff526a75,false);exit.setGravity(Gravity.CENTER);exit.setContentDescription("Sair da lição");exit.setOnClickListener(v->actions.exit());header.addView(exit,new LayoutParams(d(36),d(42)));
        View progress=new View(context) {
            private final Paint p=new Paint(3);
            protected void onDraw(Canvas c) {
                float h=getHeight();p.setColor(border);c.drawRoundRect(0,0,getWidth(),h,h/2,h/2,p);
                float width=Math.max(h,getWidth()*(step+1f)/total);p.setColor(step*1f/total>.5?0xffffc800:0xff93d333);c.drawRoundRect(0,0,width,h,h/2,h/2,p);
                p.setColor(0x45ffffff);c.drawRoundRect(d(4),d(3),Math.max(d(4),width-d(4)),h*.48f,h/2,h/2,p);
            }
        };
        progress.setContentDescription("Progresso: "+(step+1)+" de "+total);LayoutParams bar=new LayoutParams(0,d(16),1);bar.setMargins(d(14),0,d(14),0);header.addView(progress,bar);
        TextView infinity=text("∞",30,0xffa568ff,true);infinity.setContentDescription("Prática sem limite");header.addView(infinity);addView(header);
        String title=type.equals("cloze")?"Complete o espaço vazio:":type.equals("speaking")?"Repita o que Óscar disse:":type.equals("image_cloze")?"Complete a frase:":type.equals("image_choice")?"Selecione a imagem correta:":type.equals("pairs")?"Combine os pares:":type.equals("translate_bank")||type.equals("word_order")?"Traduza esta frase:":type.equals("listening")?"Ouça e escolha:":"Escolha a tradução correta:";
        TextView heading=text(title,25,ink,true);heading.setPadding(0,d(20),0,d(10));addView(heading);
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);addView(scroll,new LayoutParams(-1,0,1));
        body=new LinearLayout(context);body.setOrientation(VERTICAL);scroll.addView(body,new ScrollView.LayoutParams(-1,-1));
        feedback=text("",16,ink,true);feedback.setPadding(d(14),d(12),d(14),d(12));feedback.setVisibility(GONE);addView(feedback);
        action=button(q.optString("primary_action","VERIFICAR"),true);action.setEnabled(false);styleAction(false);LayoutParams footer=new LayoutParams(-1,d(52));footer.setMargins(0,d(12),0,0);addView(action,footer);
        action.setOnClickListener(v->{if(checked)actions.next();else verify();});
        if(type.equals("pairs"))buildPairs();
        else if(type.equals("speaking"))buildSpeaking();
        else if(type.equals("image_choice"))buildImages();
        else if(type.equals("translate_bank")||type.equals("word_order"))buildBank();
        else if(type.equals("image_cloze"))buildImageCloze();
        else buildChoices();
    }
    private int d(float n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private TextView text(String value,int size,int color,boolean heavy){TextView t=new TextView(getContext());t.setText(value);t.setTextSize(size);t.setTypeface(heavy?bold:regular);t.setTextColor(color);return t;}
    private GradientDrawable round(int color,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(d(16));if(stroke!=0)g.setStroke(d(2),stroke);return g;}
    private Drawable raised(int color,int stroke){LayerDrawable layers=new LayerDrawable(new Drawable[]{round(stroke,0),round(color,stroke)});layers.setLayerInset(1,0,0,0,d(3));return layers;}
    private Button button(String value,boolean primary){Button b=new Button(getContext());b.setAllCaps(false);b.setText(value);b.setTextSize(primary?16:20);b.setTypeface(primary?bold:regular);b.setTextColor(ink);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(d(12),d(8),d(12),d(10));b.setBackground(raised(bg,border));return b;}
    private SpannableString audioLabel(String value){SpannableString text=new SpannableString("\ufffc  "+value);Drawable icon=getResources().getDrawable(getResources().getIdentifier("original_speaker_blue","drawable",getContext().getPackageName()),null);icon.setBounds(0,0,d(28),d(28));text.setSpan(new ImageSpan(icon,ImageSpan.ALIGN_BOTTOM),0,1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);return text;}
    private void underlineWords(TextView view){SpannableString value=new SpannableString(view.getText());java.util.regex.Matcher words=java.util.regex.Pattern.compile("[\\p{L}']+").matcher(value);while(words.find())value.setSpan(new ReplacementSpan(){
        public int getSize(Paint p,CharSequence text,int start,int end,Paint.FontMetricsInt fm){return (int)Math.ceil(p.measureText(text,start,end));}
        public void draw(Canvas c,CharSequence text,int start,int end,float x,int top,int y,int bottom,Paint p){c.drawText(text,start,end,x,y,p);Paint line=new Paint(3);line.setColor(border);line.setStrokeWidth(d(2));line.setPathEffect(new DashPathEffect(new float[]{d(4),d(2)},0));c.drawLine(x,y+d(6),x+p.measureText(text,start,end),y+d(6),line);}
    },words.start(),words.end(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);view.setText(value);view.setLineSpacing(d(12),1);}
    private Drawable speechBubble(){return new Drawable(){private final Paint p=new Paint(3);
        public void draw(Canvas c){Rect b=getBounds();float left=b.left+d(13),top=b.top+d(2),right=b.right-d(2),bottom=b.bottom-d(2),mid=(top+bottom)/2;Path path=new Path();path.addRoundRect(left,top,right,bottom,d(17),d(17),Path.Direction.CW);Path tail=new Path();tail.moveTo(left+1,mid-d(12));tail.lineTo(b.left+d(2),mid+d(3));tail.lineTo(left+1,mid+d(3));tail.close();path.op(tail,Path.Op.UNION);p.setStyle(Paint.Style.FILL);p.setColor(bg);c.drawPath(path,p);p.setStyle(Paint.Style.STROKE);p.setColor(border);p.setStrokeWidth(d(2));c.drawPath(path,p);}
        public void setAlpha(int alpha){}public void setColorFilter(ColorFilter filter){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    };}
    private void styleAction(boolean enabled){action.setTextColor(enabled?0xff244700:bg);action.setBackground(raised(enabled?green:border,enabled?0xff459e00:border));}
    private void enable(){action.setEnabled(true);styleAction(true);}
    private void space(float weight){body.addView(new View(getContext()),new LayoutParams(1,0,weight));}
    private void hero(String prompt,String character,int height){
        LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);
        if(!character.isEmpty()) {CharacterView c=new CharacterView(getContext(),character);characters.add(c);row.addView(c,new LayoutParams(0,d(height),.38f));}
        TextView bubble=text(prompt,21,ink,false);if(type.equals("listening")||type.equals("translate_bank"))bubble.setText(audioLabel(type.equals("listening")?"Toque para ouvir":prompt));if(type.equals("cloze")||type.equals("translate_bank"))underlineWords(bubble);bubble.setGravity(Gravity.CENTER_VERTICAL);bubble.setPadding(d(28),d(15),d(16),d(15));bubble.setBackground(speechBubble());
        bubble.setOnClickListener(v->actions.audio());bubble.setContentDescription("Ouvir: "+prompt);row.addView(bubble,new LayoutParams(0,-2,character.isEmpty()?1:.62f));body.addView(row,new LayoutParams(-1,d(height)));
    }
    private void buildChoices(){
        space(.3f);hero(type.equals("listening")?"Toque para ouvir":question.optString("prompt"),question.optString("character","oscar"),165);space(1.2f);
        JSONArray choices=question.optJSONArray("choices");if(choices!=null)for(int i=0;i<choices.length();i++)choice(choices.optString(i),body,false);
        space(1.15f);
    }
    private Button choice(String value,LinearLayout parent,boolean compact){
        Button b=button(value,false);options.add(b);LayoutParams p=new LayoutParams(compact?-2:-1,d(55));p.setMargins(compact?d(3):0,d(4),compact?d(3):0,d(4));parent.addView(b,p);
        b.setOnClickListener(v->{if(checked)return;chosen=value;for(Button item:options)item.setBackground(raised(bg,border));b.setBackground(raised(0xff153d4c,0xff1899d6));enable();});return b;
    }
    private void buildImageCloze(){
        IllustrationView picture=new IllustrationView(getContext(),question.optString("image"));body.addView(picture,new LayoutParams(-1,d(190)));
        TextView phrase=text(question.optString("prompt").replace("___","________"),21,ink,false);phrase.setPadding(d(4),d(18),0,d(15));body.addView(phrase);space(1);
        ChipFlow bank=new ChipFlow(getContext());JSONArray choices=question.optJSONArray("choices");
        for(int i=0;i<choices.length();i++){String value=choices.optString(i);Button b=button(value,false);options.add(b);bank.addView(b,new ViewGroup.LayoutParams(-2,d(50)));b.setOnClickListener(v->{if(checked)return;chosen=value;for(Button item:options)item.setBackground(raised(bg,border));b.setBackground(raised(0xff153d4c,0xff1899d6));phrase.setText(question.optString("prompt").replace("___",value));enable();});}
        body.addView(bank);space(1);
    }
    private void buildImages(){
        Button audio=button("",false);audio.setText(audioLabel(question.optString("prompt")));audio.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);audio.setBackgroundColor(Color.TRANSPARENT);audio.setContentDescription("Ouvir palavra");audio.setOnClickListener(v->actions.audio());body.addView(audio,new LayoutParams(-1,d(60)));
        JSONArray choices=question.optJSONArray("choices"),images=question.optJSONArray("images");ArrayList<LinearLayout> cards=new ArrayList<>();
        for(int row=0;row<2;row++){
            LinearLayout line=new LinearLayout(getContext());body.addView(line,new LayoutParams(-1,0,1));
            for(int col=0;col<2;col++){
                final int index=row*2+col;String value=choices.optString(index);LinearLayout card=new LinearLayout(getContext());card.setOrientation(VERTICAL);card.setGravity(Gravity.CENTER);card.setPadding(d(10),d(12),d(10),d(12));card.setBackground(raised(bg,border));card.setContentDescription(value);card.setFocusable(true);cards.add(card);
                LayoutParams p=new LayoutParams(0,-1,1);p.setMargins(col==0?0:d(7),d(6),col==0?d(7):0,d(6));line.addView(card,p);
                card.addView(new IllustrationView(getContext(),images.optString(index)),new LayoutParams(-1,0,1));TextView caption=text(value,20,ink,false);caption.setGravity(Gravity.CENTER);card.addView(caption);
                card.setOnClickListener(v->{if(checked)return;chosen=value;for(LinearLayout c:cards)c.setBackground(raised(bg,border));card.setBackground(raised(0xff153d4c,0xff1899d6));enable();});
            }
        }
    }
    private void buildBank(){
        space(.3f);hero(question.optString("prompt"),question.optString("character","zari"),170);space(.6f);
        assembled=new ChipFlow(getContext()) {
            private final Paint p=new Paint(3);
            @Override protected void dispatchDraw(Canvas c){p.setColor(border);p.setStrokeWidth(d(2));c.drawLine(0,getHeight()-d(4),getWidth(),getHeight()-d(4),p);c.drawLine(0,d(45),getWidth(),d(45),p);super.dispatchDraw(c);}
        };assembled.setMinimumHeight(d(100));body.addView(assembled,new LayoutParams(-1,-2));space(.7f);
        ChipFlow bank=new ChipFlow(getContext());JSONArray tokens=question.optJSONArray("tokens");
        for(int i=0;i<tokens.length();i++){
            final int index=i;Button chip=button(tokens.optString(i),false);options.add(chip);bank.addView(chip,new ViewGroup.LayoutParams(-2,d(50)));
            chip.setOnClickListener(v->{if(checked)return;selected.add(index);chip.setEnabled(false);chip.setAlpha(.25f);updateBank();});
        }
        body.addView(bank);space(.8f);
    }
    private void updateBank(){
        assembled.removeAllViews();StringBuilder sentence=new StringBuilder();JSONArray tokens=question.optJSONArray("tokens");
        for(int index:selected){if(sentence.length()>0)sentence.append(' ');sentence.append(tokens.optString(index));Button chip=button(tokens.optString(index),false);assembled.addView(chip,new ViewGroup.LayoutParams(-2,d(45)));
            chip.setOnClickListener(v->{if(checked)return;selected.remove(Integer.valueOf(index));options.get(index).setEnabled(true);options.get(index).setAlpha(1);updateBank();});}
        chosen=sentence.toString();action.setEnabled(!selected.isEmpty());styleAction(!selected.isEmpty());
    }
    private void buildPairs(){
        space(1);JSONArray pairs=question.optJSONArray("pairs");ArrayList<Integer> order=new ArrayList<>();for(int i=0;i<pairs.length();i++)order.add(i);Collections.shuffle(order,new Random(27));
        for(int i=0;i<pairs.length();i++){
            final int index=i;final int rightIndex=order.get(i);LinearLayout row=new LinearLayout(getContext());LayoutParams rp=new LayoutParams(-1,d(73));rp.setMargins(0,0,0,d(9));body.addView(row,rp);
            Button l=button(pairs.optJSONArray(i).optString(0),false),r=button(pairs.optJSONArray(rightIndex).optString(1),false);l.setTextSize(19);r.setTextSize(19);
            LayoutParams lp=new LayoutParams(0,-1,1);lp.setMargins(0,0,d(12),0);row.addView(l,lp);row.addView(r,new LayoutParams(0,-1,1));leftPairs.add(l);rightPairs.add(r);
            l.setOnClickListener(v->{if(checked)return;if(left>=0)leftPairs.get(left).setBackground(raised(bg,border));left=index;l.setBackground(raised(0xff153d4c,0xff1899d6));matchPair(order);});
            r.setOnClickListener(v->{if(checked)return;if(right>=0)rightPairs.get(right).setBackground(raised(bg,border));right=index;r.setBackground(raised(0xff153d4c,0xff1899d6));matchPair(order);});
        }
        space(1);
    }
    private void matchPair(ArrayList<Integer> order){
        if(left<0||right<0)return;Button l=leftPairs.get(left),r=rightPairs.get(right);
        if(left==order.get(right)){l.setEnabled(false);r.setEnabled(false);l.setAlpha(.35f);r.setAlpha(.35f);l.setBackground(raised(0xff244422,0xff58a700));r.setBackground(raised(0xff244422,0xff58a700));pairsDone++;if(pairsDone==order.size()){chosen="all_pairs";enable();}}
        else {pairError=true;l.setBackground(raised(0xff512b2b,0xffff4b4b));r.setBackground(raised(0xff512b2b,0xffff4b4b));postDelayed(()->{if(l.isEnabled())l.setBackground(raised(bg,border));if(r.isEnabled())r.setBackground(raised(bg,border));},450);}
        left=-1;right=-1;
    }
    private void buildSpeaking(){
        TextView bubble=text("",21,ink,false);bubble.setText(audioLabel(question.optString("prompt")));underlineWords(bubble);bubble.setPadding(d(28),d(14),d(18),d(14));bubble.setBackground(speechBubble());bubble.setOnClickListener(v->actions.audio());bubble.setContentDescription("Ouvir frase");body.addView(bubble);space(.3f);
        CharacterView character=new CharacterView(getContext(),"oscar");characters.add(character);body.addView(character,new LayoutParams(-1,d(210)));space(1);
        Button microphone=button("",false);microphone.setCompoundDrawablesWithIntrinsicBounds(null,getResources().getDrawable(getResources().getIdentifier("original_microphone","drawable",getContext().getPackageName()),null),null,null);microphone.setContentDescription("Gravar minha fala");microphone.setBackground(raised(blue,0xff1899c9));LayoutParams mp=new LayoutParams(d(92),d(92));mp.gravity=Gravity.CENTER_HORIZONTAL;body.addView(microphone,mp);microphone.setOnClickListener(v->{if(!checked)actions.speak();});
        speechStatus=text("Toque no microfone e repita a frase.",14,0xff93a7b0,false);speechStatus.setGravity(Gravity.CENTER);LayoutParams sp=new LayoutParams(-1,-2);sp.setMargins(0,d(12),0,0);body.addView(speechStatus,sp);space(1);
        action.setText("NÃO POSSO FALAR AGORA");action.setBackgroundColor(Color.TRANSPARENT);action.setTextColor(0xff526a75);action.setEnabled(true);action.setOnClickListener(v->{if(checked)actions.next();else actions.alternate();});
    }
    void speechStatus(String value){if(speechStatus!=null)speechStatus.setText(value);}
    void speechResult(String value){if(checked)return;chosen=value;speechStatus("Você disse: "+value);verify();}
    private void verify(){
        if(checked)return;boolean correct=ExerciseRules.matches(chosen,question.optString("answer"));
        JSONArray accepted=question.optJSONArray("accepted");if(accepted!=null)for(int i=0;i<accepted.length();i++)correct|=ExerciseRules.matches(chosen,accepted.optString(i));
        if(type.equals("pairs")&&pairError)correct=false;
        checked=true;actions.checked(correct);for(CharacterView character:characters)character.feedback(correct);feedback.setVisibility(VISIBLE);feedback.setText((correct?"✓ Muito bem!":"Resposta correta:")+"\n"+(type.equals("pairs")?"Pares concluídos"+(pairError?" · vamos revisar novamente.":"!"):question.optString("answer")));
        feedback.setTextColor(correct?0xffb8e986:0xffffb8b8);feedback.setBackground(round(correct?0xff203c1c:0xff512b2b,0));
        action.setEnabled(true);styleAction(true);action.setText("CONTINUAR");action.setOnClickListener(v->actions.next());
    }
    void dispose(){for(CharacterView character:characters)character.dispose();characters.clear();}
    private class ChipFlow extends ViewGroup {
        ChipFlow(Context c){super(c);}
        protected void onMeasure(int ws,int hs){int width=MeasureSpec.getSize(ws),x=0,y=0,row=0;for(int i=0;i<getChildCount();i++){View v=getChildAt(i);measureChild(v,ws,hs);int w=Math.min(width,v.getMeasuredWidth()),h=v.getMeasuredHeight();if(x+w>width&&x>0){y+=row+d(8);x=0;row=0;}x+=w+d(7);row=Math.max(row,h);}setMeasuredDimension(width,Math.max(getSuggestedMinimumHeight(),y+row+d(4)));}
        protected void onLayout(boolean changed,int l,int t,int r,int b){int x=0,y=0,row=0;for(int i=0;i<getChildCount();i++){View v=getChildAt(i);int w=Math.min(getWidth(),v.getMeasuredWidth()),h=v.getMeasuredHeight();if(x+w>getWidth()&&x>0){x=0;y+=row+d(8);row=0;}v.layout(x,y,x+w,y+h);x+=w+d(7);row=Math.max(row,h);}}
        protected ViewGroup.LayoutParams generateDefaultLayoutParams(){return new ViewGroup.LayoutParams(-2,-2);}
    }
}
