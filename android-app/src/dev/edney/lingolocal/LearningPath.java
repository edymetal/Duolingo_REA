package dev.edney.lingolocal;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** Trilha nativa: recursos vetoriais do APK de referência, progresso local. */
final class LearningPath extends FrameLayout {
    interface Actions { void lesson(int index); void activities(); void guide(); void progress(); void review(); void about(); }
    private final Actions actions;
    private final Typeface regular, bold;
    private final int blue = Color.rgb(28,176,246), muted = Color.rgb(147,167,176);
    LearningPath(Context context, Typeface regular, Typeface bold, String title, String[] titles,
                 boolean[] finished, int xp, int streak, Actions actions) {
        super(context); this.actions=actions; this.regular=regular; this.bold=bold;
        LinearLayout page=new LinearLayout(context); page.setOrientation(1); addView(page,new LayoutParams(-1,-1));
        LinearLayout brand=new LinearLayout(context); brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.setPadding(d(22),d(3),d(22),0);
        brand.addView(text("Lingo Edy",18,muted,true),new LinearLayout.LayoutParams(0,-2,1));
        brand.addView(text("● OFFLINE",11,Color.rgb(88,204,2),true)); page.addView(brand);
        int completed=0; for(boolean done:finished) if(done)completed++;
        LinearLayout stats=new LinearLayout(context); stats.setGravity(Gravity.CENTER_VERTICAL); stats.setPadding(d(20),d(8),d(20),d(14));
        stats.addView(icon("course_flag_en",34,28));
        TextView count=text("  "+completed+"/"+titles.length,18,Color.WHITE,true); stats.addView(count,new LinearLayout.LayoutParams(0,-2,1));
        stats.addView(icon("friends_streak_flame",26,30));
        TextView days=text(" "+streak,18,Color.rgb(255,75,75),true); days.setContentDescription(streak+" dias de sequência"); stats.addView(days,new LinearLayout.LayoutParams(0,-2,1));
        stats.addView(icon("gem",28,30)); stats.addView(text(" "+xp+" XP",18,blue,true)); page.addView(stats);
        ScrollView scroll=new ScrollView(context); scroll.setVerticalScrollBarEnabled(false);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); LinearLayout journey=new LinearLayout(context); journey.setOrientation(1); scroll.addView(journey);
        LinearLayout banner=new LinearLayout(context); banner.setGravity(Gravity.CENTER_VERTICAL); banner.setPadding(d(16),d(12),d(12),d(14));
        GradientDrawable bannerBg=new GradientDrawable(); bannerBg.setColor(blue); bannerBg.setCornerRadius(d(17)); banner.setBackground(bannerBg);
        LinearLayout heading=new LinearLayout(context); heading.setOrientation(1);
        heading.addView(text("SEÇÃO 1, UNIDADE 1",14,Color.rgb(181,232,255),true)); heading.addView(text(title,22,Color.WHITE,true)); banner.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        ImageView guide=icon("guidebook_white",36,44); guide.setContentDescription("Guia da unidade"); guide.setOnClickListener(v->actions.guide()); banner.addView(guide);
        LinearLayout.LayoutParams bannerLp=new LinearLayout.LayoutParams(-1,-2); bannerLp.setMargins(d(22),0,d(22),d(4)); journey.addView(banner,bannerLp);
        FrameLayout map=new FrameLayout(context) {
            @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
                super.onSizeChanged(w,h,oldw,oldh);
                float[] centers={.50f,.37f,.30f,.37f,.50f,.64f};
                for(int i=0;i<6;i++) {
                    View node=getChildAt(i); FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)node.getLayoutParams();
                    p.leftMargin=(int)(w*centers[i])-d(38); p.topMargin=d(12+i*91); node.setLayoutParams(p);
                }
                View character=getChildAt(6); FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)character.getLayoutParams();
                p.leftMargin=(int)(w*.58f); p.topMargin=d(145); character.setLayoutParams(p);
                View chest=getChildAt(7); FrameLayout.LayoutParams c=(FrameLayout.LayoutParams)chest.getLayoutParams(); c.leftMargin=d(23); c.topMargin=d(20); chest.setLayoutParams(c);
            }
        };
        journey.addView(map,new LinearLayout.LayoutParams(-1,d(580)));
        for(int i=0;i<6;i++) {
            final int lesson=i==3?3:i%3; final boolean practice=i>=4;
            View node=new LessonNode(context,i%3,finished[lesson]);
            node.setContentDescription((practice?"Praticar: ":"")+titles[lesson]); node.setFocusable(true);
            node.setOnClickListener(v->actions.lesson(lesson)); map.addView(node,new FrameLayout.LayoutParams(d(76),d(74)));
        }
        ImageView character=icon("bea_sitting",152,223); character.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); map.addView(character,new FrameLayout.LayoutParams(d(152),d(223)));
        ImageView chest=icon("chest_closed",54,58); chest.setContentDescription("Meu progresso"); chest.setOnClickListener(v->actions.progress()); map.addView(chest,new FrameLayout.LayoutParams(d(54),d(58)));
        TextView hint=text("4 lições · 25 exercícios\nToque em uma estrela para começar.\nCores e roupas também está no botão de prática.",14,muted,false); hint.setGravity(Gravity.CENTER); hint.setPadding(d(22),0,d(22),d(22)); journey.addView(hint);
        View divider=new View(context); divider.setBackgroundColor(Color.rgb(55,70,79)); page.addView(divider,new LinearLayout.LayoutParams(-1,d(2)));
        LinearLayout nav=new LinearLayout(context); nav.setGravity(Gravity.CENTER); nav.setPadding(d(10),d(10),d(10),d(10));
        String[] icons={"home_active","chest_closed","leagues_active","heart_active","icon_practice","more_active"};
        String[] names={"Início","Guia da unidade","Meu progresso","Revisar meus erros","Praticar lições","Sobre o Lingo Edy"};
        for(int i=0;i<icons.length;i++) {
            final int n=i; FrameLayout cell=new FrameLayout(context); nav.addView(cell,new LinearLayout.LayoutParams(0,d(48),1));
            ImageView image=icon(icons[i],32,32); FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(d(34),d(34),Gravity.CENTER); cell.addView(image,p); cell.setContentDescription(names[i]); cell.setFocusable(true);
            if(i==0) { GradientDrawable selected=new GradientDrawable(); selected.setColor(Color.rgb(25,48,59)); selected.setCornerRadius(d(12)); selected.setStroke(d(2),Color.rgb(63,133,167)); cell.setBackground(selected); }
            cell.setOnClickListener(v->{ if(n==0)scroll.smoothScrollTo(0,0); else if(n==1)actions.guide(); else if(n==2)actions.progress(); else if(n==3)actions.review(); else if(n==4)actions.activities(); else actions.about(); });
        }
        page.addView(nav);
    }
    private int d(float value) { return (int)(getResources().getDisplayMetrics().density*value+.5f); }
    private TextView text(String value,int size,int color,boolean heavy) { TextView t=new TextView(getContext()); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(heavy?bold:regular); t.setPadding(0,d(3),0,d(3)); return t; }
    private Drawable drawable(String name) { return getResources().getDrawable(getResources().getIdentifier("original_"+name,"drawable",getContext().getPackageName()),null); }
    private ImageView icon(String name,int width,int height) { ImageView image=new ImageView(getContext()); image.setImageDrawable(drawable(name)); image.setScaleType(ImageView.ScaleType.FIT_CENTER); image.setLayoutParams(new LinearLayout.LayoutParams(d(width),d(height))); return image; }
    private final class LessonNode extends View {
        private final Paint paint=new Paint(3); private final int kind; private final boolean done;
        LessonNode(Context c,int kind,boolean done) { super(c); this.kind=kind; this.done=done; }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); float w=getWidth(),h=getHeight();
            paint.setColor(Color.rgb(18,142,192)); c.drawOval(0,h*.15f,w,h,paint);
            paint.setColor(blue); c.drawOval(0,0,w,h*.87f,paint);
            c.save(); Path clip=new Path(); clip.addOval(0,0,w,h*.87f,Path.Direction.CW); c.clipPath(clip);
            paint.setColor(Color.rgb(78,199,246)); paint.setStrokeWidth(w*.17f); paint.setStrokeCap(Paint.Cap.ROUND);
            c.drawLine(w*.04f,h*.42f,w*.49f,h*.02f,paint); c.drawLine(w*.28f,h*.71f,w*.79f,h*.17f,paint); c.restore();
            Drawable image=drawable(kind==2?"contacts_book":"level_star"); image=image.mutate(); image.setTint(Color.WHITE);
            if(kind==2)image.setBounds((int)(w*.25f),(int)(h*.23f),(int)(w*.75f),(int)(h*.64f)); else image.setBounds(0,(int)(-h*.04f),(int)w,(int)(h*.83f)); image.draw(c);
            if(done) { paint.setColor(Color.rgb(88,204,2)); c.drawCircle(w*.83f,h*.16f,w*.13f,paint); paint.setColor(Color.WHITE); paint.setTextSize(w*.16f); paint.setTypeface(bold); c.drawText("✓",w*.76f,h*.22f,paint); }
        }
    }
}
