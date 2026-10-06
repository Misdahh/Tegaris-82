package com.tegaris82.fight;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;
import java.util.*;

public class FightView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rng = new Random();
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final ArrayList<DamageText> damages = new ArrayList<>();

    private int screen = 0; // 0 menu, 1 select, 2 fight, 3 result, 4 settings, 5 outfits, 6 profile
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
    private boolean musicOn = true, sfxOn = true, vibrationOn = true;
    private String profileName = "TEGARIS PLAYER";
    private int profileLevel = 1, profileWins = 0, profileFights = 0;
    private MediaPlayer menuMusic;
    private SoundPool soundPool;
    private int sndPunch = 0, sndHit = 0, sndSkill = 0;
    private int[] outfitChoice = new int[17];
    private final int[] baseAccent = new int[17];
    private final int[] baseDark = new int[17];
    // Procedural fighting animation / movement state.
    private float playerX, enemyX;
    private float playerYOff = 0, enemyYOff = 0;
    private long playerMoveUntil = 0, enemyMoveUntil = 0;
    private long playerPoseUntil = 0, enemyPoseUntil = 0;
    private int playerPose = 0, enemyPose = 0;
    private float playerMoveFrom = 0, playerMoveTo = 0;
    private float enemyMoveFrom = 0, enemyMoveTo = 0;
    private long lastAnimFrame = 0;
    private boolean stickActive = false;
    private float stickX = 105, stickY = 0;
    private long playerAttackUntil = 0, enemyAttackUntil = 0;
    private long clashUntil = 0;
    private float clashX = 0, clashY = 0;
    private int clashColor = 0xFFFFC107;
    private long playerHitStunUntil = 0, enemyHitStunUntil = 0;
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
        for (int i=0;i<chars.length;i++) { baseAccent[i]=chars[i].accent; baseDark[i]=chars[i].dark; }
        initAudio();
    }

    private void initAudio() {
        try {
            menuMusic = MediaPlayer.create(getContext(), com.tegaris82.fight.R.raw.menu_music);
            if (menuMusic != null) { menuMusic.setLooping(true); if (musicOn) menuMusic.start(); }
            AudioAttributes aa = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            soundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(aa).build();
            sndPunch = soundPool.load(getContext(), R.raw.punch, 1);
            sndHit = soundPool.load(getContext(), R.raw.hit, 1);
            sndSkill = soundPool.load(getContext(), R.raw.skill, 1);
        } catch (Exception ignored) {}
    }

    private void playSfx(int id) { if (sfxOn && soundPool != null && id != 0) soundPool.play(id,1,1,1,0,1); }
    private void applyOutfit(int idx, int outfit) {
        outfitChoice[idx]=outfit;
        int[] colors=outfitColors(idx,outfit); chars[idx].accent=colors[0]; chars[idx].dark=colors[1];
    }

    @Override protected void onDetachedFromWindow() {
        if(menuMusic!=null){ try{menuMusic.stop();}catch(Exception ignored){} menuMusic.release(); menuMusic=null; }
        if(soundPool!=null){ soundPool.release(); soundPool=null; }
        super.onDetachedFromWindow();
    }

    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (screen == 0) drawMenu(c);
        else if (screen == 1) drawSelect(c);
        else if (screen == 2) drawFight(c);
        else if (screen == 3) drawResult(c);
        else if (screen == 4) drawSettings(c);
        else if (screen == 5) drawOutfits(c);
        else drawProfile(c);
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

    private void backButton(Canvas c) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xCC101820);
        c.drawRoundRect(18, 70, 150, 122, 14, 14, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
        p.setColor(0x6638D9FF);
        c.drawRoundRect(18, 70, 150, 122, 14, 14, p);
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(15);
        p.setColor(Color.WHITE);
        c.drawText("← BACK", 84, 103, p);
        p.setTextAlign(Paint.Align.LEFT);
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
        // Modern fighting-game home screen: layered lights, stage silhouettes and clear cards.
        c.drawColor(0xFF05080D);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF0C1720); c.drawRect(0,0,getWidth(),getHeight(),p);
        p.setColor(0x2218E0FF); c.drawCircle(getWidth()*0.18f,190,180,p);
        p.setColor(0x2218E0FF); c.drawCircle(getWidth()*0.84f,250,240,p);
        for(int i=0;i<9;i++){ float bx=i*95-30; float bh=80+(i%4)*48; p.setColor(0xFF101B23); c.drawRect(bx,235-bh,bx+70,235,p); }
        p.setColor(0xFF0A1117); c.drawRect(0,235,getWidth(),getHeight(),p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(0x332DD9FF);
        for(int i=0;i<10;i++) c.drawLine(i*getWidth()/10f,235,getWidth()/2f+(i-5)*90,getHeight(),p);
        p.setStyle(Paint.Style.FILL);
        drawCornerDecor(c); logo(c,getWidth()/2f,105,0.9f);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(13); p.setColor(0xFF9EABB5);
        c.drawText("MOBILE 1V1 • MARTIAL ARTS • SEASON 01",getWidth()/2f,164,p);
        panel(c,getWidth()/2f-250,195,getWidth()/2f+250,470,0xE6111820,0xFF2B3D48,24);
        p.setTextSize(28); p.setColor(Color.WHITE); c.drawText("BATTLE ARENA",getWidth()/2f,238,p);
        p.setTextSize(12); p.setColor(0xFF82939D); c.drawText("FIGHT • MYSTIC SKILLS • POWER CLASH",getWidth()/2f,262,p);
        button(c,getWidth()/2f,318,330,58,"PLAY NOW",0xFF18E0FF);
        button(c,getWidth()/2f-88,390,160,50,"CHARACTERS",0xFF263640);
        button(c,getWidth()/2f+88,390,160,50,"OUTFITS",0xFF263640);
        button(c,getWidth()/2f,450,160,50,"SETTINGS",0xFF18242B);
        button(c,getWidth()/2f+88,450,160,50,"PROFILE",0xFF18242B);
        p.setTextSize(10); p.setColor(0xFF64757F); c.drawText("MMC PONSEL SERVICE ARENA • 17 ORIGINAL FIGHTERS",getWidth()/2f,510,p);
    }

    private void drawSelect(Canvas c) {
        bg(c,0xFF090D12);
        backButton(c);
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
        drawArena(c);
        backButton(c);
        topBrand(c);
        panel(c,getWidth()/2f-105,66,getWidth()/2f+105,91,0xAA101820,0x552DD6FF,12);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(10); p.setColor(0xFFB8EFFF);
        c.drawText(arenaNames[arena],getWidth()/2f,83,p);

        drawBar(c,30,32,300,playerHP,chars[selected].name,chars[selected].accent,true);
        drawBar(c,getWidth()-330,32,300,enemyHP,chars[enemy].name,chars[enemy].accent,false);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(12); p.setColor(0xFF8EA0AA);
        c.drawText("ROUND 1",getWidth()/2f,55,p);

        drawFighter(c,playerX,getHeight()*0.67f+playerYOff,chars[selected],true,playerPose);
        drawFighter(c,enemyX,getHeight()*0.67f+enemyYOff,chars[enemy],false,enemyPose);
        if(System.currentTimeMillis() < skillFlashUntil) {
            drawMysticEnergy(c, getWidth()*0.50f, getHeight()*0.52f, chars[selected].accent, skillUltimate);
        }
        drawStickControls(c);
        if(System.currentTimeMillis() < skillFlashUntil) {
            drawSkillEffect(c,getWidth()*0.70f,getHeight()*0.48f,chars[selected].accent,skillUltimate);
        }
        if(System.currentTimeMillis() < clashUntil) drawPowerClash(c);

        if(combo>1 && System.currentTimeMillis()<comboUntil) {
            panel(c,getWidth()/2f-85,78,getWidth()/2f+85,118,0xDD101820,chars[selected].accent,14);
            p.setTextSize(20); p.setColor(chars[selected].accent);
            c.drawText(combo+" HIT COMBO",getWidth()/2f,103,p);
        }

        p.setTextAlign(Paint.Align.LEFT); p.setTextSize(10); p.setColor(0xFFB0BEC5);
        c.drawText("ENERGY",30,getHeight()-85,p);
        p.setColor(0xFF263238); c.drawRoundRect(30,getHeight()-74,250,getHeight()-55,10,10,p);
        p.setColor(0xFFFFC107); c.drawRoundRect(30,getHeight()-74,30+220*(energy/100f),getHeight()-55,10,10,p);

        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(9); p.setColor(0x99FFFFFF);
        c.drawText("STICK ←→ = MUNDUR / MAJU   •   □ PUNCH   × KICK   △ SPECIAL   ○ ULT",getWidth()/2f,getHeight()-108,p);

        updateGame();
        drawDamageTexts(c);
    }

    private void drawStickControls(Canvas c) {
        if(screen!=2) return;
        float cy=getHeight()-148, cx=105, r=58;
        p.setStyle(Paint.Style.FILL); p.setColor(0x44202A30); c.drawCircle(cx,cy,r+10,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(0x887E8D96); c.drawCircle(cx,cy,r,p);
        float kx=cx, ky=cy;
        if(stickActive){ kx=stickX; ky=stickY; }
        p.setStyle(Paint.Style.FILL); p.setColor(0xCC37474F); c.drawCircle(kx,ky,28,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(0xAAFFFFFF); c.drawCircle(kx,ky,28,p);
        p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(9); p.setColor(0x99FFFFFF);
        c.drawText("MOVE",cx,cy+82,p);

        float by=getHeight()-148;
        psButton(c,getWidth()-225,by,"□",0xFF26A69A);
        psButton(c,getWidth()-145,by-38,"△",0xFFFFC107);
        psButton(c,getWidth()-145,by+42,"×",0xFF42A5F5);
        psButton(c,getWidth()-65,by,"○",chars[selected].accent);
        p.setTextSize(8); p.setColor(0x99FFFFFF);
        c.drawText("PUNCH",getWidth()-225,by+45,p);
        c.drawText("ULT",getWidth()-65,by+45,p);
        c.drawText("KICK",getWidth()-145,by+90,p);
        p.setTextAlign(Paint.Align.LEFT);
    }

    private void psButton(Canvas c,float x,float y,String glyph,int color){
        p.setStyle(Paint.Style.FILL); p.setColor(0x99202A30); c.drawCircle(x,y,27,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(color); c.drawCircle(x,y,25,p);
        p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(25); p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD)); p.setColor(color);
        c.drawText(glyph,x,y+9,p);
    }

    private void drawMysticEnergy(Canvas c,float x,float y,int color,boolean ultimate){
        long now=System.currentTimeMillis();
        float pulse=35+(float)(Math.sin(now/75.0)*8);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4); p.setColor(0xAA000000| (color & 0x00FFFFFF));
        c.drawCircle(x,y,pulse,p); c.drawCircle(x,y,pulse+14,p);
        p.setStrokeWidth(2); p.setColor(0x99FFFFFF);
        for(int i=0;i<8;i++){
            double a=now/180.0+i*Math.PI/4.0;
            float x1=x+(float)Math.cos(a)*18, y1=y+(float)Math.sin(a)*18;
            float x2=x+(float)Math.cos(a)*70, y2=y+(float)Math.sin(a)*70;
            c.drawLine(x1,y1,x2,y2,p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        Path flame=new Path();
        flame.moveTo(x,y-45); flame.quadTo(x-22,y-18,x-8,y+5); flame.quadTo(x-4,y+25,x,y+38);
        flame.quadTo(x+6,y+20,x+10,y+6); flame.quadTo(x+25,y-18,x,y-45); flame.close(); c.drawPath(flame,p);
        p.setColor(0xEEFFFFFF); c.drawCircle(x,y,9,p);
        p.setTextSize(10); p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.WHITE);
        c.drawText(ultimate?"MYSTIC ULTIMATE":"MYSTIC SKILL",x,y+86,p);
    }

    private void updateGame() {
        long now=System.currentTimeMillis();
        if(screen!=2) return;
        if(lastFrame==0) lastFrame=now;
        float dt=Math.min(0.05f,(now-lastFrame)/1000f);
        lastFrame=now;

        // Smooth recovery to idle after every martial-arts action.
        if(now>playerPoseUntil) playerPose=0;
        if(now>enemyPoseUntil) enemyPose=0;
        if(now>playerMoveUntil) playerMoveUntil=0;
        if(now>enemyMoveUntil) enemyMoveUntil=0;

        // Opponent footwork: advance, retreat a little, then strike when in range.
        if(enemyHP>0 && playerHP>0) {
            float gap=Math.abs(enemyX-playerX);
            if(now>aiNext-260 && gap>205) {
                enemyMoveFrom=enemyX;
                enemyMoveTo=playerX+(enemyX>playerX?205:-205);
                enemyMoveUntil=now+420;
                enemyPose=1; // walk forward
                enemyPoseUntil=now+460;
            } else if(now>aiNext-120 && gap<135) {
                // Small defensive retreat so the fight breathes like a normal 1v1 fighter.
                enemyMoveFrom=enemyX;
                enemyMoveTo=Math.max(getWidth()*0.36f, enemyX+38);
                enemyMoveUntil=now+220;
                enemyPose=1;
                enemyPoseUntil=now+240;
            }
            if(now>aiNext) {
                aiNext=now+600+rng.nextInt(650);
                float dmg=5+rng.nextInt(7);
                int move=rng.nextInt(3);
                enemyPose=move==0?2:(move==1?3:5); // punch / kick / mystic stance
                enemyPoseUntil=now+360;
                enemyAttackUntil=now+260;
                enemyYOff=0;
                if(gap<=285) enemyAttack(dmg);
            }
        }

        if(playerMoveUntil>now) {
            float t=1f-(playerMoveUntil-now)/260f;
            t=Math.max(0,Math.min(1,t));
            playerX=playerMoveFrom+(playerMoveTo-playerMoveFrom)*t;
        }
        if(playerPose==6 && now>playerPoseUntil-250) playerYOff=0;

        if(enemyMoveUntil>now) {
            float t=1f-(enemyMoveUntil-now)/420f;
            t=Math.max(0,Math.min(1,t));
            enemyX=enemyMoveFrom+(enemyMoveTo-enemyMoveFrom)*t;
        }
        enemyX=Math.max(getWidth()*0.36f,Math.min(getWidth()*0.86f,enemyX));
        playerX=Math.max(getWidth()*0.14f,Math.min(getWidth()*0.64f,playerX));

        // Small breathing/stance bounce keeps fighters alive even while idle.
        float breathe=(float)Math.sin(now/150.0)*2.2f;
        if(playerPose==0) playerYOff=breathe;
        if(enemyPose==0) enemyYOff=-breathe;

        particles.removeIf(q->now-q.t>500);
        damages.removeIf(q->now-q.t>650);
    }

    private void spawnImpact(float x,float y,int color) {
        for(int i=0;i<8;i++) {
            Particle q=new Particle();
            q.x=x; q.y=y; q.r=2+rng.nextInt(4); q.color=color; q.t=System.currentTimeMillis();
            particles.add(q);
        }
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

    private void drawFighter(Canvas c,float x,float y,CharacterInfo ch,boolean facingRight,int pose) {
        drawAnimatedHuman(c,x,y,ch,1.65f,facingRight,pose);
        if ("MISDAH".equals(ch.name)) drawMisdahGear(c,x,y,1.15f,facingRight);
        drawCharacterDetails(c,x,y,ch,1.65f,facingRight);
    }

    // Original, procedural human fighter: head, torso, arms and legs are posed every frame.
    // This gives a fighting-game feel without copying any third-party character model.
    private void drawAnimatedHuman(Canvas c,float x,float y,CharacterInfo ch,float s,boolean right,int pose) {
        long now=System.currentTimeMillis();
        float t=(now%1000)/1000f;
        float walk=(float)Math.sin(now/150.0)*0.14f;
        float breath=(float)Math.sin(now/390.0)*1.8f;
        float bob=(float)Math.sin(now/180.0)*1.4f;
        float armA=0, armB=0, legA=0, legB=0, torsoLean=0, jump=0;
        // Blend in subtle guard motion so the fighter feels alive between attacks.
        armA += (float)Math.sin(now/240.0)*0.10f;
        armB -= (float)Math.sin(now/260.0)*0.08f;
        if(pose==1) { walk=(float)Math.sin(now/75.0)*0.75f; legA=walk; legB=-walk; armA=-walk*0.7f; armB=walk*0.7f; }
        else if(pose==2) { armA=1.25f; armB=-0.35f; torsoLean=0.10f; } // punch
        else if(pose==3) { legA=1.35f; legB=-0.15f; armA=-0.25f; armB=0.55f; torsoLean=0.08f; } // kick
        else if(pose==4) { armA=-0.9f; armB=0.9f; torsoLean=-0.08f; } // hit reaction
        else if(pose==5) {
            float spin=(float)Math.sin(now/70.0);
            armA=0.9f*spin; armB=-0.9f*spin; legA=0.8f*spin; legB=-0.8f*spin; torsoLean=0.18f;
        } // special / spinning martial-arts strike
        else if(pose==6) { jump=-26; legA=-0.35f; legB=0.35f; armA=0.8f; armB=-0.8f; }

        float dir=right?1f:-1f;
        y+=bob+jump;
        // Shadow
        p.setStyle(Paint.Style.FILL); p.setColor(0x66000000);
        c.drawOval(x-72*s,y-8*s,x+72*s,y+13*s,p);

        // Coordinates are joints; limbs are thick strokes for smooth anti-aliased motion.
        float headY=y-225*s, shoulderY=y-165*s, hipY=y-92*s;
        shoulderY += breath*s*0.45f;
        headY += breath*s*0.22f;
        float shoulderX=x+torsoLean*45*s;
        float neckY=y-194*s;
        float leftShoulder=shoulderX-31*s, rightShoulder=shoulderX+31*s;
        float leftHip=x-22*s, rightHip=x+22*s;

        // Back-side limbs with visible elbow/knee joints for a more anatomical silhouette.
        float backHandX=x-dir*(58+24*armB)*s, backHandY=y-112*s;
        float backElbowX=(leftShoulder+backHandX)*0.53f-dir*8*s;
        float backElbowY=(shoulderY+backHandY)*0.52f+7*s;
        limb(c,leftShoulder,shoulderY,backElbowX,backElbowY,18*s,ch.dark);
        limb(c,backElbowX,backElbowY,backHandX,backHandY,14*s,ch.dark);
        float backFootX=x-dir*(27+20*legA)*s, backFootY=y-5*s;
        float backKneeX=(leftHip+backFootX)*0.52f-dir*7*s;
        float backKneeY=(hipY+backFootY)*0.53f;
        limb(c,leftHip,hipY,backKneeX,backKneeY,21*s,ch.dark);
        limb(c,backKneeX,backKneeY,backFootX,backFootY,16*s,ch.dark);

        // Torso and pants.
        p.setStyle(Paint.Style.FILL); p.setColor(ch.dark);
        c.drawRoundRect(x-34*s+torsoLean*18*s,y-115*s,x+34*s+torsoLean*18*s,y-55*s,14*s,14*s,p);
        p.setColor(ch.accent);
        Path torso=new Path(); torso.moveTo(x-38*s,y-178*s); torso.lineTo(x+38*s,y-178*s);
        torso.lineTo(x+31*s,y-108*s); torso.lineTo(x-31*s,y-108*s); torso.close(); c.drawPath(torso,p);
        // 3D-style body shading: darker side plane + bright rim gives depth on 2D Canvas.
        p.setColor(0x66000000);
        Path side=new Path(); side.moveTo(x+18*s,y-177*s); side.lineTo(x+38*s,y-178*s); side.lineTo(x+31*s,y-108*s); side.lineTo(x+15*s,y-108*s); side.close(); c.drawPath(side,p);
        p.setColor(0xCCFFFFFF); c.drawRect(x-5*s,y-169*s,x+5*s,y-112*s,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2*s); p.setColor(0xAAFFFFFF);
        c.drawLine(x-31*s,y-108*s,x-38*s,y-178*s,p); c.drawLine(x+31*s,y-108*s,x+38*s,y-178*s,p);
        p.setStyle(Paint.Style.FILL);

        // Front limbs are split at elbow and knee; joints move with each attack pose.
        float frontFootX=x+dir*(30+55*legB)*s, frontFootY=y-8*s;
        float frontKneeX=(rightHip+frontFootX)*0.52f+dir*8*s;
        float frontKneeY=(hipY+frontFootY)*0.52f;
        limb(c,rightHip,hipY,frontKneeX,frontKneeY,22*s,ch.dark);
        limb(c,frontKneeX,frontKneeY,frontFootX,frontFootY,17*s,ch.dark);
        float frontHandX=x+dir*(64+38*armA)*s, frontHandY=y-122*s;
        float frontElbowX=(rightShoulder+frontHandX)*0.52f+dir*10*s;
        float frontElbowY=(shoulderY+frontHandY)*0.52f+9*s;
        limb(c,rightShoulder,shoulderY,frontElbowX,frontElbowY,19*s,ch.dark);
        limb(c,frontElbowX,frontElbowY,frontHandX,frontHandY,15*s,ch.dark);

        // Joint pads and glove/boot silhouettes.
        p.setStyle(Paint.Style.FILL); p.setColor(ch.dark);
        c.drawCircle(backElbowX,backElbowY,9*s,p); c.drawCircle(frontElbowX,frontElbowY,9*s,p);
        c.drawCircle(backKneeX,backKneeY,10*s,p); c.drawCircle(frontKneeX,frontKneeY,10*s,p);
        p.setColor(0xFF11161A);
        c.drawCircle(backHandX,backHandY,11*s,p);
        c.drawCircle(frontHandX,frontHandY,11*s,p);
        c.drawRoundRect(backFootX-14*s,backFootY-5*s,backFootX+10*s,backFootY+7*s,5*s,5*s,p);
        c.drawRoundRect(frontFootX-10*s,frontFootY-5*s,frontFootX+17*s,frontFootY+7*s,5*s,5*s,p);

        // Neck, head and hair/helmet.
        p.setColor(0xFFB77B5A); c.drawRect(x-10*s,neckY,x+10*s,neckY+22*s,p);
        p.setColor(0xFFB77B5A); c.drawCircle(x,headY,29*s,p);
        p.setColor(0x33000000); c.drawOval(x+2*s,headY-20*s,x+28*s,headY+24*s,p);
        p.setColor(ch.dark); c.drawArc(x-31*s,headY-30*s,x+31*s,headY+28*s,180,180,true,p);
        p.setColor(ch.accent); c.drawRoundRect(x-27*s,headY-6*s,x+27*s,headY+8*s,7*s,7*s,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2*s); p.setColor(0x99FFFFFF);
        c.drawLine(x-18*s,headY-1*s,x+16*s,headY-1*s,p);
        p.setStyle(Paint.Style.FILL);

        // Shoulder armor.
        p.setColor(ch.accent); c.drawCircle(leftShoulder,shoulderY,13*s,p); c.drawCircle(rightShoulder,shoulderY,13*s,p);
    }

    private void limb(Canvas c,float x1,float y1,float x2,float y2,float width,int color) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(width); p.setColor(color);
        c.drawLine(x1,y1,x2,y2,p);
        p.setStyle(Paint.Style.FILL);
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

    private void drawPowerClash(Canvas c) {
        long now=System.currentTimeMillis();
        float pulse=1f+(float)Math.sin(now/55.0)*0.12f;
        float r=48*pulse;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(7); p.setColor(clashColor);
        c.drawCircle(clashX,clashY,r,p);
        p.setStrokeWidth(3); p.setColor(0xEEFFFFFF);
        c.drawCircle(clashX,clashY,r*0.55f,p);
        for(int i=0;i<12;i++) {
            double a=i*Math.PI/6.0+now/260.0;
            float x1=clashX+(float)Math.cos(a)*r;
            float y1=clashY+(float)Math.sin(a)*r;
            float x2=clashX+(float)Math.cos(a)*(r+20);
            float y2=clashY+(float)Math.sin(a)*(r+20);
            c.drawLine(x1,y1,x2,y2,p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(18); p.setColor(Color.WHITE);
        c.drawText("POWER CLASH!",clashX,clashY-68,p);
        p.setTextSize(10); p.setColor(0xFFFFC107);
        c.drawText("TIMING + STRENGTH",clashX,clashY-50,p);
        p.setTextAlign(Paint.Align.LEFT);
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

    private void drawSettings(Canvas c) {
        bg(c,0xFF070B10); drawCornerDecor(c); topBrand(c); backButton(c);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(26); p.setColor(Color.WHITE); c.drawText("SETTINGS",getWidth()/2f,155,p);
        panel(c,getWidth()/2f-220,190,getWidth()/2f+220,430,0xE6111820,0xFF2B3D48,22);
        settingRow(c,235,"MUSIC",musicOn); settingRow(c,300,"SOUND EFFECTS",sfxOn); settingRow(c,365,"VIBRATION",vibrationOn);
        p.setTextSize(10); p.setColor(0xFF73858F); c.drawText("Audio and haptic controls for the fight experience.",getWidth()/2f,455,p);
    }
    private void settingRow(Canvas c,float y,String label,boolean on){
        p.setTextAlign(Paint.Align.LEFT); p.setTextSize(16); p.setColor(Color.WHITE); c.drawText(label,getWidth()/2f-170,y+6,p);
        button(c,getWidth()/2f+125,y,100,40,on?"ON":"OFF",on?0xFF18E0FF:0xFF263640);
        p.setTextAlign(Paint.Align.CENTER);
    }
    private void drawOutfits(Canvas c) {
        bg(c,0xFF080D12); drawCornerDecor(c); topBrand(c); backButton(c);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(25); p.setColor(Color.WHITE); c.drawText("OUTFIT GARAGE",getWidth()/2f,150,p);
        p.setTextSize(11); p.setColor(0xFF81919A); c.drawText("GANTI SET BAJU • TAP OUTFIT UNTUK MEMAKAI",getWidth()/2f,170,p);
        int[] outfits={0,1,2,3,4}; String[] names={"DEFAULT","GOLD FIST","MYSTIC","CYBER","CRIMSON"};
        for(int i=0;i<5;i++){ float x=75+i*155, y=215; panel(c,x-60,y,x+60,y+205,outfitChoice[selected]==i?0xFF162B33:0xFF10171D,outfitChoice[selected]==i?chars[selected].accent:0xFF2B3740,14);
            applyPreviewFighter(c,x,y+135,chars[selected],i); p.setTextSize(10); p.setColor(outfitChoice[selected]==i?chars[selected].accent:Color.WHITE); c.drawText(names[i],x,y+187,p);
        }
        button(c,getWidth()/2f,getHeight()-55,300,52,"DONE",chars[selected].accent);
    }
    private void applyPreviewFighter(Canvas c,float x,float y,CharacterInfo ch,int outfit){
        int oldA=ch.accent, oldD=ch.dark;
        int[] colors=outfitColors(selected,outfit); ch.accent=colors[0]; ch.dark=colors[1];
        drawMiniFighter(c,x,y,ch,0.52f,true);
        ch.accent=oldA; ch.dark=oldD;
    }
    private int[] outfitColors(int idx,int outfit){
        int a=baseAccent[idx], d=baseDark[idx];
        if(outfit==1){a=0xFFFFC107;d=0xFF2A2112;}
        else if(outfit==2){a=0xFFB388FF;d=0xFF241638;}
        else if(outfit==3){a=0xFF39D7FF;d=0xFF11262E;}
        else if(outfit==4){a=0xFFFF5252;d=0xFF321517;}
        return new int[]{a,d};
    }

    private void drawProfile(Canvas c) {
        bg(c,0xFF070B10); drawCornerDecor(c); topBrand(c); backButton(c);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(26); p.setColor(Color.WHITE);
        c.drawText("PLAYER PROFILE",getWidth()/2f,150,p);
        panel(c,getWidth()/2f-260,180,getWidth()/2f+260,500,0xE6111820,0xFF2B3D48,24);
        // 3D-style avatar badge
        float ax=getWidth()/2f, ay=255;
        p.setStyle(Paint.Style.FILL); p.setColor(0xFF101820); c.drawCircle(ax,ay,70,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5); p.setColor(0xFF18E0FF); c.drawCircle(ax,ay,70,p);
        p.setStyle(Paint.Style.FILL); p.setColor(chars[selected].dark); c.drawCircle(ax,ay,43,p);
        p.setColor(chars[selected].accent); c.drawCircle(ax,ay-8,28,p);
        p.setColor(0xFFB77B5A); c.drawCircle(ax,ay-12,17,p);
        p.setColor(0xFF101820); c.drawArc(ax-18,ay-31,ax+18,ay+4,180,180,true,p);
        p.setColor(chars[selected].accent); c.drawRoundRect(ax-16,ay-13,ax+16,ay-5,4,4,p);
        p.setTextSize(22); p.setColor(Color.WHITE); c.drawText(profileName,ax,360,p);
        p.setTextSize(12); p.setColor(0xFF18E0FF); c.drawText("LEVEL "+profileLevel+"  •  FIGHTER "+chars[selected].name,ax,383,p);
        statCard(c,ax-170,430,100,"FIGHTS",String.valueOf(profileFights));
        statCard(c,ax,430,100,"WINS",String.valueOf(profileWins));
        statCard(c,ax+170,430,100,"RANK","ROOKIE");
        p.setTextSize(10); p.setColor(0xFF72838D); c.drawText("PROFILE • STATS • SELECTED FIGHTER",ax,480,p);
    }
    private void statCard(Canvas c,float cx,float cy,float w,String label,String value){
        panel(c,cx-w/2,cy-30,cx+w/2,cy+30,0xFF121B22,0xFF2B3D48,12);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(18); p.setColor(Color.WHITE); c.drawText(value,cx,cy+3,p);
        p.setTextSize(8); p.setColor(0xFF81919A); c.drawText(label,cx,cy+20,p);
    }

    private void drawResult(Canvas c) {
        bg(c,0xFF070A0F);
        backButton(c);
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
            if(screen==2 && x<190 && y>getHeight()-225) {
                stickActive=true; stickX=x; stickY=y;
                return true;
            }
            return true;
        }

        if(screen==2 && e.getAction()==MotionEvent.ACTION_MOVE && stickActive) {
            float cx=105, cy=getHeight()-148;
            float vx=x-cx, vy=y-cy, len=(float)Math.sqrt(vx*vx+vy*vy);
            float max=58; if(len>max){vx=vx/len*max; vy=vy/len*max;}
            stickX=cx+vx; stickY=cy+vy;
            float norm=vx/max;
            float target=Math.max(getWidth()*0.14f,Math.min(getWidth()*0.64f,playerX+norm*10));
            // Keep a little fighter spacing so characters can face each other and exchange blows.
            float maxPlayer=getWidth()*0.64f;
            if(target>enemyX-92) target=enemyX-92;
            target=Math.max(getWidth()*0.14f,Math.min(maxPlayer,target));
            playerMoveFrom=playerX; playerMoveTo=target;
            playerMoveUntil=System.currentTimeMillis()+90; playerPose=1; playerPoseUntil=System.currentTimeMillis()+130;
            return true;
        }

        if(e.getAction()!=MotionEvent.ACTION_UP) return true;
        if(stickActive) { stickActive=false; stickX=105; stickY=getHeight()-148; return true; }

        float dx=x-touchDownX;
        float dy=y-touchDownY;
        long dt=System.currentTimeMillis()-touchDownTime;

        // Common mobile-game back gesture: swipe from the left edge to the right.
        if(touchDownX < 90 && dx > 110 && Math.abs(dy) < 90 && dt < 700) {
            goBack();
            return true;
        }

        // On-screen BACK button.
        if(screen != 0 && x>=18 && x<=150 && y>=70 && y<=125) {
            goBack();
            return true;
        }

        if(screen==0) {
            if(y>285 && y<350) { screen=1; }
            else if(y>365 && y<420 && x<getWidth()/2f) { screen=1; }
            else if(y>365 && y<420 && x>=getWidth()/2f) { screen=5; }
            else if(y>425 && y<490 && x<getWidth()/2f+8) { screen=4; }
            else if(y>425 && y<490 && x>=getWidth()/2f+8) { screen=6; }
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
            // Swipe left/right anywhere above the action bar = footwork. Swipe up = jump.
            if(y<getHeight()-115 && Math.abs(dx)>55 && Math.abs(dx)>Math.abs(dy)) {
                float target=Math.max(getWidth()*0.14f,Math.min(getWidth()*0.64f,playerX+dx*0.75f));
                if(target>enemyX-92) target=enemyX-92;
                target=Math.max(getWidth()*0.14f,Math.min(getWidth()*0.64f,target));
                playerMoveFrom=playerX; playerMoveTo=target;
                playerMoveUntil=System.currentTimeMillis()+260;
                playerPose=1; playerPoseUntil=System.currentTimeMillis()+300;
            } else if(y<getHeight()-115 && dy<-70) {
                playerPose=6; playerPoseUntil=System.currentTimeMillis()+520;
                playerYOff=-28;
            } else if(y>getHeight()-225) {
                float by=getHeight()-148;
                if(dist(x,y,getWidth()-225,by)<34) attack(7,false);
                else if(dist(x,y,getWidth()-145,by+42)<34) attack(10,false);
                else if(dist(x,y,getWidth()-145,by-38)<34) attack(16,false);
                else if(dist(x,y,getWidth()-65,by)<34) attack(25,true);
            }
        } else if(screen==3) {
            if(y>325&&y<405) startFight();
            else if(y>405) screen=1;
        } else if(screen==4) {
            if(y>205&&y<270){ musicOn=!musicOn; if(menuMusic!=null){ if(musicOn) menuMusic.start(); else menuMusic.pause(); } }
            else if(y>=270&&y<335) sfxOn=!sfxOn;
            else if(y>=335&&y<400) vibrationOn=!vibrationOn;
        } else if(screen==5) {
            if(y>205&&y<435){ int idx=Math.round((x-75)/155f); if(idx>=0&&idx<5){ applyOutfit(selected,idx); } }
            else if(y>getHeight()-95) screen=0;
        } else if(screen==6) {
            // Profile is intentionally read-only in this build; stats update after matches.
        }
        return true;
    }

    private void vibrate(long ms){
        try {
            Vibrator v=(Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if(v!=null && v.hasVibrator()) {
                if(Build.VERSION.SDK_INT>=26) v.vibrate(VibrationEffect.createOneShot(ms,VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(ms);
            }
        } catch(Exception ignored) {}
    }

    private float dist(float ax,float ay,float bx,float by){ float dx=ax-bx,dy=ay-by; return (float)Math.sqrt(dx*dx+dy*dy); }

    private void goBack() {
        if(screen==1) {
            screen=0;
        } else if(screen==2) {
            // Leave the match without changing the selected fighter.
            screen=1;
        } else if(screen==3) {
            screen=1;
        } else if(screen==4 || screen==5 || screen==6) {
            screen=0;
        }
    }

    private void startFight() {
        arena=rng.nextInt(arenaNames.length);
        enemy=rng.nextInt(chars.length);
        if(enemy==selected) enemy=(enemy+1)%chars.length;
        playerHP=enemyHP=100; energy=0; combo=0; damages.clear();
        playerX=getWidth()*0.30f; enemyX=getWidth()*0.70f;
        playerYOff=enemyYOff=0; playerPose=enemyPose=0;
        playerMoveUntil=enemyMoveUntil=0; playerPoseUntil=enemyPoseUntil=0;
        playerAttackUntil=enemyAttackUntil=0; playerHitStunUntil=enemyHitStunUntil=0; clashUntil=0;
        lastFrame=System.currentTimeMillis();
        profileFights++;
        screen=2; aiNext=System.currentTimeMillis()+900;
    }

    private void attack(float dmg,boolean ultimate) {
        if(ultimate && energy<100) return;
        long now=System.currentTimeMillis();
        if(now<playerHitStunUntil) return;
        skillUltimate = ultimate;
        skillFlashUntil = now+420;
        playSfx(ultimate ? sndSkill : sndPunch);
        if(vibrationOn) vibrate(ultimate ? 35 : 15);
        playerPose = ultimate ? 5 : (dmg>=15 ? 5 : (dmg>=10 ? 3 : 2));
        playerPoseUntil = now + (ultimate ? 520 : 340);
        playerAttackUntil = now + (ultimate ? 360 : 230);
        // Small forward lunge gives the punch/kick physical weight.
        float lunge = ultimate ? 34 : 18;
        float targetX=Math.min(enemyX-82,playerX+lunge);
        playerMoveFrom=playerX; playerMoveTo=Math.max(getWidth()*0.14f,targetX);
        playerMoveUntil=now+150;
        if(ultimate) energy=0; else energy=Math.min(100,energy+(dmg/2));

        float distance=Math.abs(enemyX-playerX);
        if(distance>285) {
            combo=0;
            damages.add(new DamageText(enemyX,getHeight()*0.48f,"MISS",now));
            return;
        }

        // If both fighters strike together, resolve a cinematic power clash.
        if(now<enemyAttackUntil && distance<=255) {
            clashUntil=now+500;
            clashX=(playerX+enemyX)/2f;
            clashY=getHeight()*0.52f;
            clashColor=chars[selected].accent;
            playerPose=5; enemyPose=5;
            playerPoseUntil=enemyPoseUntil=now+430;
            playerHP=Math.max(0,playerHP-2);
            enemyHP=Math.max(0,enemyHP-2);
            energy=Math.min(100,energy+12);
            damage(clashX,clashY-25,2,true);
            spawnImpact(clashX,clashY,0xFFFFC107);
            return;
        }

        enemyHP=Math.max(0,enemyHP-dmg);
        playSfx(sndHit);
        if(vibrationOn) vibrate(12);
        combo++; comboUntil=now+900;
        enemyPose=4; enemyPoseUntil=now+260; enemyHitStunUntil=now+260;
        // Knockback / spacing after a successful hit.
        float push=Math.min(70,dmg*3.2f);
        enemyMoveFrom=enemyX;
        enemyMoveTo=Math.min(getWidth()*0.86f,enemyX+push);
        enemyMoveUntil=now+170;
        spawnImpact(enemyX,getHeight()*0.53f,chars[selected].accent);
        damage(enemyX,getHeight()*0.48f,(int)dmg,ultimate);
        if(enemyHP<=0) { playerWon=true; profileWins++; profileLevel=1+profileWins/5; screen=3; }
    }

    private void enemyAttack(float dmg) {
        long now=System.currentTimeMillis();
        if(now<enemyHitStunUntil || playerHP<=0 || enemyHP<=0) return;
        enemyAttackUntil=now+260;
        float distance=Math.abs(enemyX-playerX);
        if(distance>285) return;
        // Enemy also lunges, then hits with knockback so both sides feel physical.
        float targetX=Math.max(playerX+82,enemyX-22);
        enemyMoveFrom=enemyX; enemyMoveTo=targetX; enemyMoveUntil=now+130;
        if(now<playerAttackUntil && distance<=255) {
            clashUntil=now+500;
            clashX=(playerX+enemyX)/2f;
            clashY=getHeight()*0.52f;
            clashColor=chars[enemy].accent;
            playerPose=enemyPose=5;
            playerPoseUntil=enemyPoseUntil=now+430;
            playerHP=Math.max(0,playerHP-2);
            enemyHP=Math.max(0,enemyHP-2);
            energy=Math.min(100,energy+10);
            damage(clashX,clashY-25,2,true);
            spawnImpact(clashX,clashY,0xFFFFC107);
            return;
        }
        playerHP=Math.max(0,playerHP-dmg);
        playSfx(sndHit);
        if(vibrationOn) vibrate(12);
        playerPose=4; playerPoseUntil=now+260; playerHitStunUntil=now+260;
        playerMoveFrom=playerX;
        playerMoveTo=Math.max(getWidth()*0.14f,playerX-pushBack(dmg));
        playerMoveUntil=now+170;
        damage(playerX,getHeight()*0.48f,(int)dmg,false);
        spawnImpact(playerX,getHeight()*0.53f,chars[enemy].accent);
        if(playerHP<=0) { playerWon=false; screen=3; }
    }

    private float pushBack(float dmg){ return Math.min(70,dmg*3.2f); }

    private static class Particle { float x,y,r; int color; long t; }
    private static class DamageText { float x,y; String s; long t; DamageText(float x,float y,String s,long t){this.x=x;this.y=y;this.s=s;this.t=t;} }
    private static class CharacterInfo {
        String name,skill; int accent,dark; CharacterInfo(String n,String s,int a,int d){name=n;skill=s;accent=a;dark=d;}
    }
}