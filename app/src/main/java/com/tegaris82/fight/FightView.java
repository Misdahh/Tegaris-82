package com.tegaris82.fight;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import java.util.*;

public class FightView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rng = new Random();
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final ArrayList<DamageText> damages = new ArrayList<>();

    private int screen = 0; // 0 menu, 1 select, 2 fight, 3 result
    private int selected = 0;
    private int enemy = 1;
    private float playerHP = 100, enemyHP = 100, energy = 0;
    private int combo = 0;
    private long comboUntil = 0, aiNext = 0, lastFrame = 0;
    private float touchDownX, touchDownY;
    private long touchDownTime;
    private boolean playerWon = false;
    private long skillFlashUntil = 0;
    private boolean skillUltimate = false;
    private int arena = 0;
    private final String[] arenaNames = {"NEON DISTRICT", "CYBER LAB", "DESERT RUINS", "ICE REACTOR", "SKY PLATFORM", "VOLCANIC CORE", "MMC PONSEL SERVICE"};
    private final CharacterInfo[] chars = new CharacterInfo[] {
        new CharacterInfo("GARIS", "RUSH", 0xFF18E0FF, 0xFF17202A),
        new CharacterInfo("TEGA", "IRON FIST", 0xFFFFC107, 0xFF263238),
        new CharacterInfo("NOVA", "STAR BURST", 0xFFB388FF, 0xFF311B4E),
        new CharacterInfo("BLAZE", "INFERNO", 0xFFFF7043, 0xFF4A1C16),
        new CharacterInfo("SHADOW", "VOID STEP", 0xFF90A4AE, 0xFF151A20),
        new CharacterInfo("TITAN", "GROUND BREAK", 0xFF8D6E63, 0xFF30251F),
        new CharacterInfo("RAVEN", "DARK WING", 0xFF7E57C2, 0xFF1A1426),
        new CharacterInfo("VORTEX", "CYCLONE", 0xFF26A69A, 0xFF12352F),
        new CharacterInfo("PHANTOM", "PHASE HIT", 0xFF64B5F6, 0xFF16263A),
        new CharacterInfo("FURY", "BERSERK", 0xFFEF5350, 0xFF3A1717),
        new CharacterInfo("STORM", "THUNDER", 0xFF42A5F5, 0xFF15283D),
        new CharacterInfo("ZERO", "ZERO LIMIT", 0xFFECEFF1, 0xFF20252A),
        new CharacterInfo("KIRA", "NEON BLADE", 0xFFFF4081, 0xFF3A1023),
        new CharacterInfo("AXEL", "MAX POWER", 0xFF66BB6A, 0xFF18351E),
        new CharacterInfo("MIRA", "ICE NOVA", 0xFF80DEEA, 0xFF123238),
        new CharacterInfo("REX", "WAR MODE", 0xFFFFA726, 0xFF40250D),
        new CharacterInfo("MISDAH", "HACKER BOT", 0xFF00E5FF, 0xFF102A33)
    };

    public FightView(Context c) {
        super(c);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        setFocusable(true);
    }

    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (screen == 0) drawMenu(c);
        else if (screen == 1) drawSelect(c);
        else if (screen == 2) drawFight(c);
        else drawResult(c);
        invalidate();
    }


    private void panel(Canvas c,float l,float t,float r,float b,int fill,int stroke,float radius){
        p.setStyle(Paint.Style.FILL); p.setColor(0x55000000);
        c.drawRoundRect(l+5,t+6,r+5,b+6,radius,radius,p);
        p.setColor(fill); c.drawRoundRect(l,t,r,b,radius,radius,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(stroke);
        c.drawRoundRect(l,t,r,b,radius,radius,p);
        p.setStyle(Paint.Style.FILL);
    }
    private void topBrand(Canvas c){
        p.setTextAlign(Paint.Align.LEFT); p.setTextSize(18); p.setColor(Color.WHITE);
        c.drawText("TEGARIS82",24,31,p);
        p.setTextSize(10); p.setColor(0xFF18E0FF);
        c.drawText("FIGHT ARENA",25,46,p);
    }
    private void drawCornerDecor(Canvas c){
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(0x4418E0FF);
        c.drawLine(18,62,70,62,p); c.drawLine(18,62,18,92,p);
        c.drawLine(getWidth()-18,62,getWidth()-70,62,p); c.drawLine(getWidth()-18,62,getWidth()-18,92,p);
        p.setStyle(Paint.Style.FILL);
    }

    private void bg(Canvas c, int color) {
        c.drawColor(color);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF0B0F14);
        for (int i=0;i<18;i++) {
            float x=(i*97+35)%getWidth(), y=(i*61+20)%getHeight();
            c.drawCircle(x,y,2,p);
        }
    }

    private void logo(Canvas c, float cx, float y, float scale) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF111820);
        c.drawRoundRect(cx-190*scale,y-48*scale,cx+190*scale,y+48*scale,18*scale,18*scale,p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5*scale);
        p.setColor(0xFF18E0FF);
        c.drawRoundRect(cx-190*scale,y-48*scale,cx+190*scale,y+48*scale,18*scale,18*scale,p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(42*scale);
        c.drawText("TEGARIS82",cx,y+14*scale,p);
        p.setTextSize(13*scale);
        p.setColor(0xFF18E0FF);
        c.drawText("FIGHT ARENA",cx,y+34*scale,p);
    }

    private void drawMenu(Canvas c) {
        bg(c,0xFF070A0F);
        drawCornerDecor(c);
        logo(c,getWidth()/2f,118,1.0f);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(15); p.setColor(0xFF9EABB5);
        c.drawText("MOBILE 1V1 ACTION • SEASON 01",getWidth()/2f,182,p);
        panel(c,getWidth()/2f-245,210,getWidth()/2f+245,470,0xCC111820,0xFF263640,22);
        p.setTextSize(30); p.setColor(Color.WHITE);
        c.drawText("BATTLE ARENA",getWidth()/2f,260,p);
        p.setTextSize(13); p.setColor(0xFF8EA0AA);
        c.drawText("Choose a fighter and enter the arena.",getWidth()/2f,286,p);
        button(c,getWidth()/2f,345,330,64,"PLAY NOW",0xFF18E0FF);
        button(c,getWidth()/2f,420,330,55,"CHARACTERS",0xFF263238);
        p.setTextSize(11); p.setColor(0xFF65747C);
        c.drawText("TEGARIS82 • ORIGINAL FIGHTERS • v1.6",getWidth()/2f,510,p);
    }

    private void drawSelect(Canvas c) {
        backButton(c);
        bg(c,0xFF090D12);
        drawCornerDecor(c);
        topBrand(c);
        p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.WHITE); p.setTextSize(24);
        c.drawText("SELECT FIGHTER",getWidth()/2f,92,p);
        p.setTextSize(11); p.setColor(0xFF7E8D96);
        c.drawText("17 ORIGINAL OPERATORS • TAP TO SELECT",getWidth()/2f,111,p);
        int cols=4, cardW=150, cardH=155, gap=14;
        float total=cols*cardW+(cols-1)*gap, left=(getWidth()-total)/2f;
        for(int i=0;i<chars.length;i++) {
            int row=i/cols,col=i%cols;
            float x=left+col*(cardW+gap), y=125+row*(cardH+10);
            panel(c,x,y,x+cardW,y+cardH, i==selected?0xFF152A32:0xFF10171D,
                    i==selected?chars[i].accent:0xFF2B3740,12);
            drawMiniFighter(c,x+cardW/2,y+63,chars[i],0.82f,false);
            p.setTextAlign(Paint.Align.CENTER); p.setTextSize(15); p.setColor(Color.WHITE);
            c.drawText(chars[i].name,x+cardW/2,y+120,p);
            p.setTextSize(8); p.setColor(0xFF7E8D96);
            c.drawText("SKILL",x+cardW/2,y+133,p);
            p.setTextSize(9); p.setColor(chars[i].accent);
            c.drawText(chars[i].skill,x+cardW/2,y+144,p);
        }
        button(c,getWidth()/2f,getHeight()-42,290,50,"CONTINUE",chars[selected].accent);
    }

    private void drawFight(Canvas c) {
        backButton(c);
        drawArena(c);
        topBrand(c);
        panel(c,getWidth()/2f-105,66,getWidth()/2f+105,91,0xAA101820,0x552DD6FF,12);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(10); p.setColor(0xFFB8EFFF);
        c.drawText(arenaNames[arena],getWidth()/2f,83,p);

        drawBar(c,30,32,300,playerHP,chars[selected].name,chars[selected].accent,true);
        drawBar(c,getWidth()-330,32,300,enemyHP,chars[enemy].name,chars[enemy].accent,false);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(12); p.setColor(0xFF8EA0AA);
        c.drawText("ROUND 1",getWidth()/2f,55,p);

        drawFighter(c,getWidth()*0.30f,getHeight()*0.67f,chars[selected],true);
        drawFighter(c,getWidth()*0.70f,getHeight()*0.67f,chars[enemy],false);
        if(System.currentTimeMillis() < skillFlashUntil) {
            drawSkillEffect(c,getWidth()*0.70f,getHeight()*0.48f,chars[selected].accent,skillUltimate);
        }

        if(combo>1 && System.currentTimeMillis()<comboUntil) {
            panel(c,getWidth()/2f-85,78,getWidth()/2f+85,118,0xDD101820,chars[selected].accent,14);
            p.setTextSize(20); p.setColor(chars[selected].accent);
            c.drawText(combo+" HIT COMBO",getWidth()/2f,103,p);
        }

        p.setTextAlign(Paint.Align.LEFT); p.setTextSize(10); p.setColor(0xFFB0BEC5);
        c.drawText("ENERGY",30,getHeight()-85,p);
        p.setColor(0xFF263238); c.drawRoundRect(30,getHeight()-74,250,getHeight()-55,10,10,p);
        p.setColor(0xFFFFC107); c.drawRoundRect(30,getHeight()-74,30+220*(energy/100f),getHeight()-55,10,10,p);

        button(c,105,getHeight()-28,145,44,"PUNCH",0xFF455A64);
        button(c,270,getHeight()-28,145,44,"KICK",0xFF455A64);
        button(c,getWidth()-270,getHeight()-28,190,44,"SPECIAL",chars[selected].accent);
        button(c,getWidth()-90,getHeight()-28,130,44,"ULTIMATE",0xFFFFC107);

        updateGame();
        drawDamageTexts(c);
    }

    private void updateGame() {
        long now=System.currentTimeMillis();
        if(screen!=2) return;
        if(lastFrame==0) lastFrame=now;
        // gentle AI pressure
        if(now>aiNext && enemyHP>0 && playerHP>0) {
            aiNext=now+650+rng.nextInt(650);
            float dmg=5+rng.nextInt(7);
            playerHP=Math.max(0,playerHP-dmg);
            damage(getWidth()*0.30f,getHeight()*0.48f,(int)dmg,false);
            if(playerHP<=0) { playerWon=false; screen=3; }
        }
        particles.removeIf(q->now-q.t>500);
        damages.removeIf(q->now-q.t>650);
    }


    private void drawArena(Canvas c) {
        float w=getWidth(), h=getHeight(), floor=h*0.58f;
        int[] bases={0xFF07121A,0xFF091018,0xFF24170E,0xFF0A1720,0xFF111225,0xFF1D0D0A,0xFF10151A};
        c.drawColor(bases[arena]);
        p.setStyle(Paint.Style.FILL);
        if(arena==0) {
            // Neon city skyline
            p.setColor(0xFF0C1822); c.drawRect(0,150,w,floor,p);
            for(int i=0;i<9;i++){ float bx=i*w/8f; float bh=90+(i%4)*55; p.setColor(0xFF132734); c.drawRect(bx,floor-150-bh,bx+w/9f,floor,p); }
            p.setColor(0x5518E0FF); for(int i=0;i<28;i++) c.drawRect((i*73)%((int)w),190+(i%7)*35,((i*73)%((int)w))+18,193+(i%7)*35,p);
            p.setColor(0xFF101D25); c.drawRect(0,floor,w,h,p);
            p.setColor(0x3318E0FF); c.drawCircle(w/2,floor-45,210,p);
            p.setColor(0xFF1D3440); for(int i=0;i<12;i++) c.drawRect(i*w/12f,floor+35,i*w/12f+2,h,p);
        } else if(arena==1) {
            // Futuristic laboratory
            p.setColor(0xFF10242B); c.drawRect(0,145,w,floor,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(0x553FE8FF);
            for(int i=0;i<7;i++) c.drawLine(0,170+i*55,w,170+i*55,p);
            for(int i=0;i<11;i++) c.drawLine(i*w/10f,145,i*w/10f-80,floor,p);
            p.setStyle(Paint.Style.FILL); p.setColor(0xFF152F37); c.drawRect(0,floor,w,h,p);
            p.setColor(0x443FE8FF); c.drawCircle(w/2,floor-30,180,p);
            p.setColor(0xFF2B515B); for(int i=0;i<8;i++) c.drawRect(i*w/8f,floor+15,i*w/8f+3,h,p);
        } else if(arena==2) {
            // Desert ruins
            p.setColor(0xFF5A321C); c.drawRect(0,185,w,floor,p);
            p.setColor(0xFF8D5630); for(int i=0;i<7;i++){float bx=i*175-30; c.drawRect(bx,floor-170-(i%2)*50,bx+100,floor,p);}
            p.setColor(0xFF6E4026); c.drawRect(0,floor,w,h,p);
            p.setColor(0x443C2011); for(int i=0;i<12;i++) c.drawCircle((i*117)%((int)w),floor+25+(i%3)*55,18+(i%4)*7,p);
            p.setColor(0x66FFD08A); c.drawCircle(w*0.76f,175,58,p);
        } else if(arena==3) {
            // Ice reactor
            p.setColor(0xFF102B3A); c.drawRect(0,145,w,floor,p);
            p.setColor(0xFF7FE8FF); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
            for(int i=0;i<10;i++){float x=i*w/9f; c.drawLine(x,145,x+(i%2==0?50:-50),floor,p);}
            p.setStyle(Paint.Style.FILL); p.setColor(0xFF183C4A); c.drawRect(0,floor,w,h,p);
            p.setColor(0x553CE8FF); c.drawCircle(w/2,floor-40,220,p);
            p.setColor(0xFFB8F5FF); for(int i=0;i<9;i++) c.drawCircle(45+i*130,floor+25+(i%2)*55,4,p);
        } else if(arena==4) {
            // Sky platform
            p.setColor(0xFF18244A); c.drawRect(0,130,w,floor,p);
            p.setColor(0xFF4C6FA8); c.drawCircle(w*0.18f,180,52,p); c.drawCircle(w*0.78f,205,70,p);
            p.setColor(0xFF1B2338); c.drawRect(0,floor,w,h,p);
            p.setColor(0xFF6D7FA6); for(int i=0;i<11;i++) c.drawRect(i*w/10f,floor+5,i*w/10f+2,h,p);
            p.setColor(0x4438C8FF); c.drawCircle(w/2,floor-20,200,p);
        } else if(arena==5) {
            // Volcanic core
            p.setColor(0xFF32130D); c.drawRect(0,145,w,floor,p);
            p.setColor(0xFF7A2414); for(int i=0;i<8;i++) c.drawRect(i*160-40,floor-120-(i%3)*35,i*160+90,floor,p);
            p.setColor(0xFF2A1110); c.drawRect(0,floor,w,h,p);
            p.setColor(0xFFB83A19); for(int i=0;i<12;i++) c.drawRect(i*w/12f,floor+30,i*w/12f+5,h,p);
            p.setColor(0x55FF6D24); c.drawCircle(w/2,floor-35,190,p);
        } else {
            // MMC PONSEL SERVICE — modern HP repair workshop
            p.setColor(0xFF101A21); c.drawRect(0,130,w,floor,p);

            // Workshop wall panels and glass service counter
            p.setColor(0xFF182832); c.drawRect(0,150,w,floor,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(0x5538D7FF);
            for(int i=0;i<6;i++) c.drawLine(i*w/5f,150,i*w/5f,floor,p);
            c.drawLine(0,205,w,205,p); c.drawLine(0,270,w,270,p);
            p.setStyle(Paint.Style.FILL);

            // MMC PONSEL sign
            p.setColor(0xFF0B1116); c.drawRoundRect(w*0.26f,158,w*0.74f,218,12,12,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(0xFF18E0FF);
            c.drawRoundRect(w*0.26f,158,w*0.74f,218,12,12,p);
            p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(22); p.setColor(Color.WHITE); c.drawText("MMC PONSEL",w/2f,185,p);
            p.setTextSize(10); p.setColor(0xFF18E0FF); c.drawText("SERVICE • REPAIR • HP",w/2f,204,p);

            // Repair benches / phones / tools
            p.setColor(0xFF22343D); c.drawRect(35,292,w-35,350,p);
            p.setColor(0xFF0C1217); c.drawRect(0,floor,w,h,p);
            p.setColor(0xFF2D4650); c.drawRect(0,floor,w,floor+8,p);
            for(int i=0;i<5;i++) {
                float tx=80+i*w/5f;
                p.setColor(0xFF111A20); c.drawRoundRect(tx-28,250,tx+28,286,7,7,p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(0xFF39D7FF);
                c.drawRoundRect(tx-22,255,tx+22,281,5,5,p);
                p.setStyle(Paint.Style.FILL); p.setColor(0xFF39D7FF); c.drawCircle(tx,277,2,p);
            }
            // Screwdrivers and repair tools
            p.setStrokeWidth(5); p.setColor(0xFFFFC107);
            for(int i=0;i<7;i++) c.drawLine(55+i*105,floor+20,78+i*105,floor+58,p);
            p.setColor(0xFF18E0FF); c.drawCircle(w*0.82f,floor-8,28,p);
            p.setColor(0x3300E5FF); c.drawCircle(w*0.82f,floor-8,55,p);

            // Modern blue floor grid
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(0x332FD9FF);
            for(int i=0;i<12;i++) c.drawLine(i*w/11f,floor,w/2f+(i-5.5f)*90,h,p);
            for(int i=0;i<4;i++) c.drawLine(0,floor+35+i*55,w,floor+35+i*55,p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x3318E0FF); c.drawCircle(w/2,floor-20,210,p);
        }
        // Premium arena lighting and center stage
        p.setStyle(Paint.Style.FILL); p.setColor(0x2218E0FF); c.drawRect(0,floor-8,w,floor+8,p);
        p.setColor(0x44000000); c.drawOval(w*0.18f,floor-18,w*0.82f,floor+38,p);
    }
    private void drawBar(Canvas c,float x,float y,float w,float hp,String name,int accent,boolean left) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF202830); c.drawRoundRect(x,y,x+w,y+25,12,12,p);
        p.setColor(accent);
        float fill=w*Math.max(0,hp)/100f;
        if(left) c.drawRoundRect(x,y,x+fill,y+25,12,12,p);
        else c.drawRoundRect(x+w-fill,y,x+w,y+25,12,12,p);
        p.setTextSize(14); p.setTextAlign(left?Paint.Align.LEFT:Paint.Align.RIGHT); p.setColor(Color.WHITE);
        c.drawText(name,x+(left?6:w-6),y-7,p);
    }

    private void drawMisdahGear(Canvas c,float x,float y,float s,boolean facingRight) {
        // Laptop
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF1B252B);
        c.drawRoundRect(x-70*s,y-145*s,x-15*s,y-108*s,5*s,5*s,p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3*s);
        p.setColor(0xFF00E5FF);
        c.drawRoundRect(x-64*s,y-140*s,x-21*s,y-114*s,3*s,3*s,p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF263238);
        Path base = new Path();
        base.moveTo(x-78*s,y-107*s); base.lineTo(x-8*s,y-107*s);
        base.lineTo(x-18*s,y-99*s); base.lineTo(x-68*s,y-99*s); base.close();
        c.drawPath(base,p);

        // Small controlled robot/drone beside Misdah
        float rx = x + (facingRight ? 88 : -88)*s;
        float ry = y - 115*s;
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF263238);
        c.drawRoundRect(rx-27*s,ry-22*s,rx+27*s,ry+22*s,12*s,12*s,p);
        p.setColor(0xFF00E5FF);
        c.drawCircle(rx-10*s,ry-4*s,5*s,p);
        c.drawCircle(rx+10*s,ry-4*s,5*s,p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4*s);
        p.setColor(0xFF607D8B);
        c.drawLine(rx-38*s,ry-28*s,rx-55*s,ry-42*s,p);
        c.drawLine(rx+38*s,ry-28*s,rx+55*s,ry-42*s,p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF00E5FF);
        c.drawCircle(rx-58*s,ry-45*s,5*s,p);
        c.drawCircle(rx+58*s,ry-45*s,5*s,p);

        // Holographic control link
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2*s);
        p.setColor(0x8800E5FF);
        c.drawLine(x-20*s,y-120*s,rx-20*s,ry,p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawFighter(Canvas c,float x,float y,CharacterInfo ch,boolean facingRight) {
        drawMiniFighter(c,x,y,ch,1.65f,facingRight);
        if ("MISDAH".equals(ch.name)) drawMisdahGear(c,x,y,1.15f,facingRight);
        drawCharacterDetails(c,x,y,ch,1.65f,facingRight);
    }

    private void drawCharacterDetails(Canvas c,float x,float y,CharacterInfo ch,float s,boolean facingRight) {
        // High-resolution style: layered armor, seams, visor highlights, gloves and boots.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f,2.2f*s));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(0x99FFFFFF);
        c.drawLine(x-18*s,y-169*s,x+18*s,y-169*s,p);
        c.drawLine(x-18*s,y-107*s,x+18*s,y-107*s,p);
        p.setColor(0xAA000000);
        c.drawLine(x-23*s,y-93*s,x-8*s,y-58*s,p);
        c.drawLine(x+23*s,y-93*s,x+8*s,y-58*s,p);

        // Face/helmet detail.
        p.setColor(ch.accent);
        p.setStrokeWidth(2.5f*s);
        c.drawLine(x-20*s,y-232*s,x+20*s,y-232*s,p);
        p.setColor(0x99FFFFFF);
        c.drawLine(x-15*s,y-239*s,x+5*s,y-239*s,p);

        // Shoulder tech plates.
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xCCFFFFFF);
        c.drawCircle(x-38*s,y-154*s,3*s,p);
        c.drawCircle(x+38*s,y-154*s,3*s,p);

        // Character-specific visual identity.
        if ("BLAZE".equals(ch.name) || "FURY".equals(ch.name)) {
            p.setColor(0xFFFFC107);
            Path flame = new Path();
            flame.moveTo(x-5*s,y-183*s); flame.lineTo(x+8*s,y-198*s);
            flame.lineTo(x+5*s,y-180*s); flame.lineTo(x+15*s,y-190*s);
            flame.lineTo(x+8*s,y-170*s); flame.close();
            c.drawPath(flame,p);
        } else if ("SHADOW".equals(ch.name) || "PHANTOM".equals(ch.name)) {
            p.setColor(0xCCFFFFFF);
            p.setStrokeWidth(2*s);
            p.setStyle(Paint.Style.STROKE);
            c.drawArc(x-20*s,y-244*s,x+20*s,y-202*s,200,140,false,p);
        } else if ("STORM".equals(ch.name) || "VORTEX".equals(ch.name)) {
            p.setColor(0xCCFFFFFF);
            p.setStrokeWidth(2*s);
            p.setStyle(Paint.Style.STROKE);
            c.drawArc(x-25*s,y-186*s,x+25*s,y-90*s,205,130,false,p);
        } else if ("TITAN".equals(ch.name) || "REX".equals(ch.name)) {
            p.setColor(ch.accent);
            c.drawRect(x-29*s,y-148*s,x-22*s,y-118*s,p);
            c.drawRect(x+22*s,y-148*s,x+29*s,y-118*s,p);
        } else if ("KIRA".equals(ch.name) || "MIRA".equals(ch.name)) {
            p.setColor(0xCCFFFFFF);
            c.drawCircle(x-20*s,y-220*s,2.5f*s,p);
            c.drawCircle(x+20*s,y-220*s,2.5f*s,p);
        } else if ("MISDAH".equals(ch.name)) {
            p.setColor(0xFF00E5FF);
            c.drawRect(x-27*s,y-160*s,x-19*s,y-148*s,p);
        }
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawSkillEffect(Canvas c,float x,float y,int accent,boolean ultimate) {
        long now=System.currentTimeMillis();
        float pulse=(float)(0.5+0.5*Math.sin(now/90.0));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4);
        p.setColor(accent);
        float r=38+(ultimate?55:30)*pulse;
        c.drawCircle(x,y,r,p);
        p.setStrokeWidth(2);
        p.setColor(0x99FFFFFF);
        c.drawCircle(x,y,r*0.72f,p);
        for(int i=0;i<8;i++){
            double a=i*Math.PI/4.0+now/500.0;
            float x1=x+(float)Math.cos(a)*r*0.75f;
            float y1=y+(float)Math.sin(a)*r*0.75f;
            float x2=x+(float)Math.cos(a)*r*1.08f;
            float y2=y+(float)Math.sin(a)*r*1.08f;
            c.drawLine(x1,y1,x2,y2,p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void drawMiniFighter(Canvas c,float x,float y,CharacterInfo ch,float s,boolean facingRight) {
        p.setStyle(Paint.Style.FILL);
        // shadow
        p.setColor(0x66000000); c.drawOval(x-42*s,y-7*s,x+42*s,y+8*s,p);
        // legs / boots
        p.setColor(0xFF151A1E); c.drawRoundRect(x-27*s,y-55*s,x-10*s,y,7*s,7*s,p);
        c.drawRoundRect(x+10*s,y-55*s,x+27*s,y,7*s,7*s,p);
        // pants
        p.setColor(ch.dark); c.drawRoundRect(x-31*s,y-100*s,x+31*s,y-45*s,10*s,10*s,p);
        // torso armor/jacket
        p.setColor(ch.accent); c.drawRoundRect(x-34*s,y-175*s,x+34*s,y-95*s,16*s,16*s,p);
        p.setColor(0xCCFFFFFF); c.drawRect(x-5*s,y-166*s,x+5*s,y-104*s,p);
        // arms
        p.setStrokeWidth(16*s); p.setStrokeCap(Paint.Cap.ROUND); p.setColor(ch.dark);
        c.drawLine(x-28*s,y-155*s,x-(facingRight?70: -70)*s,y-112*s,p);
        c.drawLine(x+28*s,y-155*s,x+(facingRight?70: -70)*s,y-112*s,p);
        // gloves
        p.setColor(0xFF11161A); c.drawCircle(x-(facingRight?70:-70)*s,y-110*s,11*s,p);
        c.drawCircle(x+(facingRight?70:-70)*s,y-110*s,11*s,p);
        // neck/head
        p.setColor(0xFFB77B5A); c.drawRect(x-10*s,y-198*s,x+10*s,y-175*s,p);
        p.setColor(0xFFB77B5A); c.drawCircle(x,y-218*s,29*s,p);
        // hair/helmet
        p.setColor(ch.dark); c.drawArc(x-31*s,y-248*s,x+31*s,y-190*s,180,180,true,p);
        // visor
        p.setColor(ch.accent); c.drawRoundRect(x-27*s,y-224*s,x+27*s,y-210*s,7*s,7*s,p);
        // shoulder pads
        p.setColor(ch.accent); c.drawCircle(x-38*s,y-154*s,13*s,p); c.drawCircle(x+38*s,y-154*s,13*s,p);
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    private void button(Canvas c,float cx,float cy,float w,float h,String label,int color) {
        p.setStyle(Paint.Style.FILL); p.setColor(0xAA000000);
        c.drawRoundRect(cx-w/2+3,cy-h/2+4,cx+w/2+3,cy+h/2+4,12,12,p);
        p.setColor(color); c.drawRoundRect(cx-w/2,cy-h/2,cx+w/2,cy+h/2,12,12,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(0x99FFFFFF);
        c.drawRoundRect(cx-w/2,cy-h/2,cx+w/2,cy+h/2,12,12,p);
        p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(15); p.setColor(Color.WHITE);
        c.drawText(label,cx,cy+5,p);
    }

    private void drawResult(Canvas c) {
        backButton(c);
        bg(c,0xFF070A0F);
        drawCornerDecor(c);
        logo(c,getWidth()/2f,105,0.78f);
        panel(c,getWidth()/2f-245,170,getWidth()/2f+245,480,0xDD111820,
              playerWon?0xFF18E0FF:0xFFFF5252,24);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(44);
        p.setColor(playerWon?0xFF18E0FF:0xFFFF5252);
        c.drawText(playerWon?"VICTORY":"DEFEAT",getWidth()/2f,245,p);
        p.setTextSize(15); p.setColor(0xFF9EABB5);
        c.drawText("MATCH RESULT",getWidth()/2f,275,p);
        p.setTextSize(20); p.setColor(Color.WHITE);
        c.drawText(chars[selected].name+"   VS   "+chars[enemy].name,getWidth()/2f,320,p);
        button(c,getWidth()/2f,380,300,56,"REMATCH",chars[selected].accent);
        button(c,getWidth()/2f,445,300,50,"CHARACTER SELECT",0xFF263238);
    }

    private void drawMiniParticle(Canvas c,Particle q) {
        p.setStyle(Paint.Style.FILL); p.setColor(q.color); c.drawCircle(q.x,q.y,q.r,p);
    }

    private void damage(float x,float y,int value,boolean crit) {
        damages.add(new DamageText(x,y,"-"+value+(crit?"!":""),System.currentTimeMillis()));
    }

    private void drawDamageTexts(Canvas c) {
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(24);
        for(DamageText d:damages) { p.setColor(Color.WHITE); c.drawText(d.s,d.x,d.y,p); }
    }

    @Override public boolean onTouchEvent(android.view.MotionEvent e) {
        float x=e.getX(), y=e.getY();

        if(e.getAction()==MotionEvent.ACTION_DOWN) {
            touchDownX=x;
            touchDownY=y;
            touchDownTime=System.currentTimeMillis();
            return true;
        }

        if(e.getAction()!=MotionEvent.ACTION_UP) return true;

        float dx=x-touchDownX;
        float dy=y-touchDownY;
        long dt=System.currentTimeMillis()-touchDownTime;

        // Common mobile-game back gesture: swipe from the left edge to the right.
        if(touchDownX < 90 && dx > 110 && Math.abs(dy) < 90 && dt < 700) {
            goBack();
            return true;
        }

        // On-screen BACK button.
        if(screen != 0 && x>=18 && x<=82 && y>=70 && y<=125) {
            goBack();
            return true;
        }

        if(screen==0) {
            if(y>245 && y<330) screen=1;
            else if(y>340 && y<415) screen=1;
        } else if(screen==1) {
            int cols=4, cardW=150, gap=14;
            float total=cols*cardW+(cols-1)*gap, left=(getWidth()-total)/2f;
            for(int i=0;i<chars.length;i++) {
                int row=i/cols,col=i%cols;
                float xx=left+col*(cardW+gap), yy=125+row*165;
                if(x>=xx&&x<=xx+cardW&&y>=yy&&y<=yy+155) selected=i;
            }
            if(y>getHeight()-80) startFight();
        } else if(screen==2) {
            if(y>getHeight()-100) {
                if(x<180) attack(7,false);
                else if(x<360) attack(10,false);
                else if(x>getWidth()-180) attack(25,true);
                else attack(16,false);
            }
        } else {
            if(y>325&&y<405) startFight();
            else if(y>405) screen=1;
        }
        return true;
    }

    private void goBack() {
        if(screen==1) {
            screen=0;
        } else if(screen==2) {
            // Leave the match without changing the selected fighter.
            screen=1;
        } else if(screen==3) {
            screen=1;
        }
    }

    private void startFight() {
        arena=rng.nextInt(arenaNames.length);
        enemy=rng.nextInt(chars.length);
        if(enemy==selected) enemy=(enemy+1)%chars.length;
        playerHP=enemyHP=100; energy=0; combo=0; damages.clear();
        screen=2; aiNext=System.currentTimeMillis()+900;
    }

    private void attack(float dmg,boolean ultimate) {
        if(ultimate && energy<100) return;
        skillUltimate = ultimate;
        skillFlashUntil = System.currentTimeMillis()+420;
        if(ultimate) energy=0; else energy=Math.min(100,energy+(dmg/2));
        enemyHP=Math.max(0,enemyHP-dmg);
        combo++; comboUntil=System.currentTimeMillis()+900;
        damage(getWidth()*0.70f,getHeight()*0.46f,(int)dmg,ultimate);
        if(enemyHP<=0) { playerWon=true; screen=3; }
    }

    private static class CharacterInfo {
        String name,skill; int accent,dark;
        CharacterInfo(String n,String s,int a,int d){name=n;skill=s;accent=a;dark=d;}
    }
    private static class Particle { float x,y,r; int color; long t; }
    private static class DamageText { float x,y; String s; long t; DamageText(float a,float b,String c,long d){x=a;y=b;s=c;t=d;} }
}
