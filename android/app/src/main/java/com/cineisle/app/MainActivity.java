package com.cineisle.app;

import android.view.WindowManager;

import android.view.Window;

import android.graphics.drawable.ColorDrawable;

import android.app.Dialog;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.*;
import android.text.method.ScrollingMovementMethod;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
    private android.widget.FrameLayout fullscreenDanmakuRoot = null;
    private LinearLayout root, pageHome, pageRoom, pageCard, pageFavorites;
    private String theme = "night";
    private String serverUrl = "";
    private String token = "";
    private String roomId = "";
    private String name = "观影人A";
    private String avatar = "🐰";
    private String currentPage = "home";
    private VideoView video;
    private Button navHome, navRoom, navCard, navFavorites;
    private TextView roomTitle, roomCodeView, syncState, chatLog, noteLog, cardPreview, memberState, homeStatus, homeSub, heroBadge, fullChatLog, favoritesList, inviteSummary, importState;
    private EditText serverInput, tokenInput, nameInput, roomInput, chatInput, noteInput, quoteInput, cardNoteInput, linQuoteInput, linNoteInput, inviteMovieInput, invitePartnerInput, inviteMoodInput, inviteNoteInput;
    private Handler handler = new Handler();
    private boolean polling = false;
    private boolean applyingRemote = false;
    private boolean danmakuOn = true;
    private FrameLayout videoFrame, normalVideoFrame;
    private Dialog fullscreenDialog;
    private String fileName = "";
    private String movieTitle = "";
    private String invitePartner = "观影人 A × 观影人 B";
    private String inviteMood = "夜航";
    private String inviteNote = "今晚一起登岛看一场电影。";
    private String cardTemplate = "ticket";
    private JSONObject remoteCard = null;
    private int lastSentSecond = -1;

        private final HashSet<String> seenDanmakuKeys = new HashSet<>();
private final Runnable poller = new Runnable() {
        @Override public void run() {
            if (polling && roomId.length() > 0) {
                fetchRoom();
                handler.postDelayed(this, 2500);
            }
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        loadPrefs();
        buildUI();
        showPage("home");
    }

    private void loadPrefs() {
        android.content.SharedPreferences sp = getSharedPreferences("cineisle", 0);
        serverUrl = sp.getString("serverUrl", "");
        token = sp.getString("token", "");
        name = sp.getString("name", "观影人A");
        avatar = sp.getString("avatar", "🐰");
        theme = sp.getString("theme", "night");
        invitePartner = sp.getString("invitePartner", "观影人 A × 观影人 B");
        inviteMood = sp.getString("inviteMood", "夜航");
        inviteNote = sp.getString("inviteNote", "今晚一起登岛看一场电影。");
        cardTemplate = sp.getString("cardTemplate", "ticket");
    }

    private void savePrefs() {
        getSharedPreferences("cineisle", 0).edit()
            .putString("serverUrl", serverUrl)
            .putString("token", token)
            .putString("name", name)
            .putString("avatar", avatar)
            .putString("theme", theme)
            .putString("invitePartner", invitePartner)
            .putString("inviteMood", inviteMood)
            .putString("inviteNote", inviteNote)
            .putString("cardTemplate", cardTemplate)
            .apply();
    }

    private int bg() {
        if (theme.equals("cream")) return color("#FCFBF7");
        if (theme.equals("galaxy")) return color("#130F28");
        if (theme.equals("matcha")) return color("#EFF6EC");
        if (theme.equals("film")) return color("#12100D");
        if (theme.equals("dusk")) return color("#231729");
        return color("#0D1325");
    }
    private int card() {
        if (theme.equals("cream")) return color("#FFFFFFFF");
        if (theme.equals("matcha")) return color("#FFFFFFFF");
        if (theme.equals("galaxy")) return color("#201A3A");
        if (theme.equals("film")) return color("#1D1912");
        if (theme.equals("dusk")) return color("#2D2034");
        return color("#161F38");
    }
    private int cardSoft() {
        if (theme.equals("cream")) return color("#F4F2ED");
        if (theme.equals("matcha")) return color("#E7F0E1");
        if (theme.equals("galaxy")) return color("#2A214B");
        if (theme.equals("film")) return color("#2A251B");
        if (theme.equals("dusk")) return color("#3A2940");
        return color("#202A47");
    }
    private int ink() {
        if (theme.equals("cream") || theme.equals("matcha")) return color("#26314D");
        return color("#F6F8FF");
    }
    private int muted() {
        if (theme.equals("cream")) return color("#7C869F");
        if (theme.equals("matcha")) return color("#738174");
        if (theme.equals("film")) return color("#C8BFA9");
        if (theme.equals("dusk")) return color("#D0B9D6");
        return color("#AFB8D8");
    }
    private int accent() {
        if (theme.equals("cream")) return color("#8F88F3");
        if (theme.equals("galaxy")) return color("#A980FF");
        if (theme.equals("matcha")) return color("#87B68D");
        if (theme.equals("film")) return color("#C7A86B");
        if (theme.equals("dusk")) return color("#D394D8");
        return color("#88A6FF");
    }
    private int accent2() {
        if (theme.equals("cream")) return color("#F2B8C6");
        if (theme.equals("galaxy")) return color("#6AD0FF");
        if (theme.equals("matcha")) return color("#C7E2B8");
        if (theme.equals("film")) return color("#8B6D3F");
        if (theme.equals("dusk")) return color("#6EA8FF");
        return color("#C497FF");
    }
    private int color(String s) { return Color.parseColor(s); }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    private TextView tv(String text, int sp, int style) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(ink());
        v.setTypeface(Typeface.DEFAULT, style);
        v.setLineSpacing(dp(2), 1.04f);
        return v;
    }

    private TextView small(String text) {
        TextView v = tv(text, 12, Typeface.NORMAL);
        v.setTextColor(muted());
        return v;
    }

    private GradientDrawable round(int c, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(dp(r));
        g.setStroke(dp(1), theme.equals("cream") || theme.equals("matcha") ? color("#1C576587") : color("#29FFFFFF"));
        return g;
    }

    private GradientDrawable grad(int[] colors, float r) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        g.setCornerRadius(dp(r));
        return g;
    }

    private Button btn(String text, boolean primary) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : ink());
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(primary ? grad(new int[]{accent(), accent2()}, 18) : round(cardSoft(), 18));
        b.setPadding(dp(8), 0, dp(8), 0);
        return b;
    }

    private EditText input(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setSingleLine(true);
        e.setTextColor(ink());
        e.setHintTextColor(muted());
        e.setTextSize(14);
        e.setBackground(round(cardSoft(), 18));
        e.setPadding(dp(14), 0, dp(14), 0);
        return e;
    }

    private ScrollView scroll(View child) {
        ScrollView s = new ScrollView(this);
        s.setFillViewport(true);
        s.addView(child);
        return s;
    }

    private LinearLayout vbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout hbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void add(ViewGroup parent, View child, int w, int h, int mt) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
        lp.setMargins(0, dp(mt), 0, 0);
        parent.addView(child, lp);
    }

    private TextView chip(String text) {
        TextView v = small(text);
        v.setTextColor(ink());
        v.setBackground(round(cardSoft(), 18));
        v.setPadding(dp(12), dp(8), dp(12), dp(8));
        return v;
    }

    private LinearLayout panel() {
        LinearLayout p = vbox();
        p.setPadding(dp(16), dp(16), dp(16), dp(16));
        p.setBackground(round(card(), 28));
        p.setElevation(dp(3));
        return p;
    }

    private int topInset() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    private void buildUI() {
        root = vbox();
        root.setBackgroundColor(bg());
        setContentView(root);

        FrameLayout frame = new FrameLayout(this);
        root.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));

        pageHome = buildHome();
        pageRoom = buildRoom();
        pageCard = buildCard();
        pageFavorites = buildFavorites();
        frame.addView(pageHome);
        frame.addView(pageRoom);
        frame.addView(pageCard);
        frame.addView(pageFavorites);

        LinearLayout navWrap = vbox();
        navWrap.setPadding(dp(14), dp(6), dp(14), dp(14));
        LinearLayout nav = hbox();
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setBackground(round(card(), 30));
        nav.setElevation(dp(5));
        navWrap.addView(nav, new LinearLayout.LayoutParams(-1, dp(70)));
        root.addView(navWrap);

        navHome = btn("首页", true);
        navRoom = btn("放映厅", false);
        navCard = btn("票根", false);
        navFavorites = btn("档案馆", false);
        nav.addView(navHome, new LinearLayout.LayoutParams(0, dp(50), 1));
        nav.addView(navRoom, new LinearLayout.LayoutParams(0, dp(50), 1));
        nav.addView(navCard, new LinearLayout.LayoutParams(0, dp(50), 1));
        nav.addView(navFavorites, new LinearLayout.LayoutParams(0, dp(50), 1));
        navHome.setOnClickListener(v -> showPage("home"));
        navRoom.setOnClickListener(v -> showPage("room"));
        navCard.setOnClickListener(v -> showPage("card"));
        navFavorites.setOnClickListener(v -> showPage("favorites"));
        updateNav("home");
    }

    private LinearLayout buildHome() {
        LinearLayout wrap = vbox();
        wrap.setPadding(dp(16), topInset() + dp(8), dp(16), dp(12));
        LinearLayout c = vbox();
        wrap.addView(scroll(c), new LinearLayout.LayoutParams(-1, -1));

        LinearLayout topBar = hbox();
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand = vbox();

        FrameLayout logo = new FrameLayout(this);
        TextView logoShadow = new TextView(this);
        logoShadow.setText("映屿 CineIsle");
        logoShadow.setTextSize(30);
        logoShadow.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));
        logoShadow.setTextColor(color("#55FFFFFF"));
        logoShadow.setLetterSpacing(0.025f);
        logoShadow.setTranslationX(dp(2));
        logoShadow.setTranslationY(dp(2));
        logo.addView(logoShadow, new FrameLayout.LayoutParams(-1, dp(44)));

        TextView logoText = new TextView(this);
        logoText.setText("映屿 CineIsle");
        logoText.setTextSize(30);
        logoText.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));
        logoText.setTextColor(ink());
        logoText.setLetterSpacing(0.025f);
        logoText.setShadowLayer(dp(2), 0, dp(1), theme.equals("cream") || theme.equals("matcha") ? color("#77FFFFFF") : color("#553B82F6"));
        logo.addView(logoText, new FrameLayout.LayoutParams(-1, dp(44)));

        brand.addView(logo, new LinearLayout.LayoutParams(-1, dp(46)));

        TextView sub = small("CineIsle · a private island for watching together");
        sub.setLetterSpacing(0.04f);
        brand.addView(sub);
        topBar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        Button gear = btn("设置", false);
        topBar.addView(gear, new LinearLayout.LayoutParams(dp(88), dp(44)));
        c.addView(topBar);

        LinearLayout heroBox = panel();
        heroBox.setPadding(dp(0), dp(0), dp(0), dp(0));
        LinearLayout hero = vbox();
        hero.setBackground(grad(heroColors(), 30));
        hero.setPadding(dp(20), dp(22), dp(20), dp(22));
        heroBadge = chip("CineIsle");
        heroBadge.setBackground(round(color("#22FFFFFF"), 18));
        heroBadge.setTextColor(Color.WHITE);
        hero.addView(heroBadge, new LinearLayout.LayoutParams(-2, -2));
        TextView heroTitle = new TextView(this);
        heroTitle.setText("今晚登岛，\n把电影变成共同记忆。");
        heroTitle.setTextColor(Color.WHITE);
        heroTitle.setTextSize(24);
        heroTitle.setTypeface(Typeface.DEFAULT_BOLD);
        heroTitle.setLineSpacing(dp(4), 1.05f);
        add(hero, heroTitle, -1, -2, 16);
        homeSub = new TextView(this);
        homeSub.setTextColor(color("#EBF0FF"));
        homeSub.setTextSize(13);
        homeSub.setText("观影邀请卡、同步放映、弹幕时间轴、金句摘录和三套票根模板，都在这里汇成一座观影小岛。");
        add(hero, homeSub, -1, -2, 10);
        LinearLayout heroBottom = hbox();
        heroBottom.setGravity(Gravity.CENTER_VERTICAL);
        TextView mood = new TextView(this);
        mood.setText(avatar + "  " + name + " 正在筹备今晚的观影岛");
        mood.setTextColor(Color.WHITE);
        mood.setTextSize(13);
        heroBottom.addView(mood, new LinearLayout.LayoutParams(0, -2, 1));
        TextView chip2 = new TextView(this);
        chip2.setText("6 themes");
        chip2.setTextColor(Color.WHITE);
        chip2.setTextSize(12);
        chip2.setPadding(dp(10), dp(7), dp(10), dp(7));
        chip2.setBackground(round(color("#22FFFFFF"), 18));
        heroBottom.addView(chip2);
        add(hero, heroBottom, -1, -2, 16);
        heroBox.addView(hero, new LinearLayout.LayoutParams(-1, -2));
        add(c, heroBox, -1, -2, 18);

        LinearLayout quick = panel();
        quick.addView(tv("观影前 · 登岛邀请", 18, Typeface.BOLD));
        quick.addView(small("先写好今晚看什么、和谁看、是什么氛围，再一键生成观影岛。"));
        add(c, quick, -1, -2, 14);

        LinearLayout row1 = hbox();
        row1.addView(actionCard("创建观影岛", "带着邀请卡生成房间", "创建", true, v -> { collectInvitation(); collectSettings(); createRoom(); }), new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams r1b = new LinearLayout.LayoutParams(0, -2, 1); r1b.setMargins(dp(10),0,0,0);
        row1.addView(actionCard("登岛加入", "输入房间号马上会合", "加入", false, v -> { collectSettings(); joinRoom(roomInput.getText().toString()); }), r1b);
        add(c, row1, -1, -2, 10);

        LinearLayout row2 = hbox();
        row2.addView(actionCard("导入影片", "本地文件不会上传", "导入", false, v -> pickVideo()), new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams r2b = new LinearLayout.LayoutParams(0, -2, 1); r2b.setMargins(dp(10),0,0,0);
        row2.addView(actionCard("回到放映厅", "继续这场电影", "继续", false, v -> showPage("room")), r2b);
        add(c, row2, -1, -2, 10);

        LinearLayout invitePanel = panel();
        invitePanel.addView(tv("今晚的观影邀请卡", 17, Typeface.BOLD));
        invitePanel.addView(small("这里写的内容会进入房间、观影邀请卡和最终票根。"));
        inviteMovieInput = input("电影名 / 房间标题", movieTitle);
        invitePartnerInput = input("观影人，如 A × B", invitePartner);
        inviteMoodInput = input("今晚氛围，如 夜航 / 雨天 / 奶油 / 深蓝", inviteMood);
        inviteNoteInput = input("开场备注", inviteNote);
        inviteNoteInput.setSingleLine(false);
        add(invitePanel, inviteMovieInput, -1, 48, 12);
        add(invitePanel, invitePartnerInput, -1, 48, 10);
        add(invitePanel, inviteMoodInput, -1, 48, 10);
        add(invitePanel, inviteNoteInput, -1, 82, 10);
        Button saveInvite = btn("保存邀请卡", true);
        saveInvite.setOnClickListener(v -> { collectInvitation(); toast("观影邀请卡已保存"); renderCard(); });
        add(invitePanel, saveInvite, -1, 46, 12);
        add(c, invitePanel, -1, -2, 14);

        LinearLayout roomPanel = panel();
        roomPanel.addView(tv("快速加入房间", 17, Typeface.BOLD));
        roomPanel.addView(small("你可以在这里直接输入房间号，像敲开一扇小小放映室的门。"));
        roomInput = input("输入房间号，如 QXQ8KU", roomId);
        add(roomPanel, roomInput, -1, 48, 12);
        homeStatus = small(roomId.length() > 0 ? "当前房间：" + roomId : "当前还没有加入房间");
        add(roomPanel, homeStatus, -1, -2, 10);
        add(c, roomPanel, -1, -2, 14);

        LinearLayout currentPanel = panel();
        currentPanel.addView(tv("今晚的放映状态", 17, Typeface.BOLD));
        TextView line1 = chip(movieTitle.length() > 0 ? "片名 · " + movieTitle : "片名 · 等待导入影片");
        TextView line2 = chip(serverUrl.length() > 0 ? "后端已配置" : "还没有配置后端");
        add(currentPanel, line1, -1, -2, 10);
        add(currentPanel, line2, -1, -2, 8);
        add(c, currentPanel, -1, -2, 14);

        gear.setOnClickListener(v -> openSettingsSheet());
        return wrap;
    }

    private LinearLayout actionCard(String title, String desc, String buttonText, boolean primary, View.OnClickListener listener) {
        LinearLayout p = panel();
        p.setMinimumHeight(dp(142));
        TextView t = tv(title, 17, Typeface.BOLD);
        TextView d = small(desc);
        d.setTextSize(13);
        p.addView(t);
        add(p, d, -1, -2, 6);
        Button b = btn(buttonText, primary);
        b.setOnClickListener(listener);
        add(p, b, -1, 42, 18);
        return p;
    }

    private void openSettingsSheet() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout outer = vbox();
        outer.setPadding(dp(16), dp(16), dp(16), dp(16));
        outer.setBackground(round(card(), 30));

        LinearLayout head = hbox();
        TextView t = tv("设置抽屉", 22, Typeface.BOLD);
        TextView close = chip("关闭");
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(close);
        outer.addView(head);
        outer.addView(small("把后端、昵称、主题和头像都收在这里，主页只留给一起看电影的氛围。"));

        serverInput = input("后端地址，如 https://xxx.onrender.com", serverUrl);
        tokenInput = input("MCP Token，可不填", token);
        nameInput = input("昵称", name);
        add(outer, serverInput, -1, 48, 14);
        add(outer, tokenInput, -1, 48, 10);
        add(outer, nameInput, -1, 48, 10);

        LinearLayout avatarRow = hbox();
        avatarRow.setGravity(Gravity.CENTER);
        String[] avs = {"🐰","🎬","🌙","🍿","☁️"};
        for (String a: avs) {
            Button ab = btn(a, a.equals(avatar));
            ab.setTextSize(20);
            ab.setOnClickListener(v -> {
                avatar = ((Button)v).getText().toString();
                updateHeroTexts();
                openSettingsSheetRefresh(dialog);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1);
            lp.setMargins(dp(4), dp(12), dp(4), 0);
            avatarRow.addView(ab, lp);
        }
        add(outer, tv("头像", 16, Typeface.BOLD), -1, -2, 16);
        outer.addView(avatarRow);

        add(outer, tv("主题皮肤", 16, Typeface.BOLD), -1, -2, 16);
        LinearLayout skinGrid = vbox();
        outer.addView(skinGrid, new LinearLayout.LayoutParams(-1, -2));
        String[][] themes = {{"cream","奶油白"},{"night","夜航蓝"},{"galaxy","星河紫"},{"matcha","雾岛绿"},{"film","胶片黑"},{"dusk","暮光紫"}};
        LinearLayout skinRow = null;
        for (int i = 0; i < themes.length; i++) {
            if (i % 2 == 0) {
                skinRow = hbox();
                skinGrid.addView(skinRow, new LinearLayout.LayoutParams(-1, -2));
            }
            final String key = themes[i][0];
            Button tb = btn(themes[i][1], key.equals(theme));
            tb.setOnClickListener(v -> {
                theme = key;
                savePrefs();
                dialog.dismiss();
                rebuild();
                if (roomId.length() > 0) fetchRoom();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1);
            lp.setMargins(i % 2 == 0 ? 0 : dp(8), dp(8), 0, 0);
            skinRow.addView(tb, lp);
        }

        Button save = btn("保存并应用", true);
        save.setOnClickListener(v -> {
            serverUrl = normalizeServer(serverInput.getText().toString());
            token = tokenInput.getText().toString().trim();
            name = nameInput.getText().toString().trim();
            if (name.length() == 0) name = "观影人";
            savePrefs();
            dialog.dismiss();
            rebuild();
        });
        add(outer, save, -1, 48, 18);

        ScrollView sc = scroll(outer);
        dialog.setContentView(sc);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private void openSettingsSheetRefresh(Dialog old) {
        if (old != null && old.isShowing()) old.dismiss();
        openSettingsSheet();
    }

    private int[] heroColors() {
        if (theme.equals("cream")) return new int[]{color("#E7DBFF"), color("#C9B9FF")};
        if (theme.equals("galaxy")) return new int[]{color("#4D3B87"), color("#1A133E")};
        if (theme.equals("matcha")) return new int[]{color("#A7D0A2"), color("#7CAC86")};
        if (theme.equals("film")) return new int[]{color("#3B2F22"), color("#0F0D0A")};
        if (theme.equals("dusk")) return new int[]{color("#6E3C7F"), color("#1A1530")};
        return new int[]{color("#3F4F97"), color("#111A34")};
    }

    private LinearLayout buildRoom() {
        LinearLayout wrap = vbox();
        wrap.setPadding(dp(16), topInset() + dp(6), dp(16), dp(12));
        LinearLayout c = vbox();
        wrap.addView(scroll(c), new LinearLayout.LayoutParams(-1, -1));

        LinearLayout header = panel();
        header.setBackground(grad(heroColors(), 28));
        header.setPadding(dp(18), dp(18), dp(18), dp(18));
        roomTitle = new TextView(this);
        roomTitle.setText("映屿 CineIsle");
        roomTitle.setTextColor(Color.WHITE);
        roomTitle.setTextSize(24);
        roomTitle.setTypeface(Typeface.DEFAULT_BOLD);
        roomCodeView = new TextView(this);
        roomCodeView.setText("还没有进入房间 · 可以先导入本地影片");
        roomCodeView.setTextColor(color("#E9EDFF"));
        roomCodeView.setTextSize(13);
        header.addView(roomTitle);
        add(header, roomCodeView, -1, -2, 8);
        LinearLayout headerBottom = hbox();
        syncState = chip("未连接房间");
        syncState.setTextColor(Color.WHITE);
        syncState.setBackground(round(color("#22FFFFFF"), 18));
        headerBottom.addView(syncState);
        memberState = new TextView(this);
        memberState.setTextColor(color("#F1F5FF"));
        memberState.setText("0 人在线");
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-2, -2); mlp.setMargins(dp(10), 0, 0, 0);
        headerBottom.addView(memberState, mlp);
        add(header, headerBottom, -1, -2, 14);
        add(c, header, -1, -2, 4);

        LinearLayout inviteMini = panel();
        inviteMini.addView(tv("观影邀请卡", 17, Typeface.BOLD));
        inviteSummary = small(invitationText());
        inviteSummary.setTextColor(ink());
        inviteSummary.setBackground(round(cardSoft(), 20));
        inviteSummary.setPadding(dp(14), dp(12), dp(14), dp(12));
        add(inviteMini, inviteSummary, -1, -2, 10);
        importState = chip("本地影片未导入 · 等待准备");
        add(inviteMini, importState, -1, -2, 10);
        add(c, inviteMini, -1, -2, 14);

        videoFrame = new FrameLayout(this);
        normalVideoFrame = videoFrame;
        videoFrame.setBackground(round(color("#090D18"), 28));
        video = new VideoView(this);
        try { video.setZOrderOnTop(false); video.setZOrderMediaOverlay(false); } catch(Exception ignored) {}
        videoFrame.addView(video, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout overlay = vbox();
        overlay.setGravity(Gravity.CENTER);
        overlay.setTag("video_overlay");
        TextView empty = tv("在这里放映今晚的电影", 20, Typeface.BOLD);
        empty.setTextColor(Color.WHITE);
        empty.setGravity(Gravity.CENTER);
        empty.setTag("empty");
        TextView hint = new TextView(this);
        hint.setText("先导入本地影片，再和对方同步播放。\n弹幕、金句、笔记都会长成一条时间轴。♡");
        hint.setGravity(Gravity.CENTER);
        hint.setTextSize(13);
        hint.setTextColor(color("#C9D1F3"));
        overlay.addView(empty);
        add(overlay, hint, -1, -2, 8);
        videoFrame.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        add(c, videoFrame, -1, 250, 16);

        MediaController mc = new MediaController(this);
        video.setMediaController(mc);

        LinearLayout actions = hbox();
        Button pick = btn("导入影片", true);
        Button sync = btn("同步进度", false);
        Button danmaku = btn("弹幕 ON", false);
        Button fullscreen = btn("横屏", false);
        actions.addView(pick, new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams ax = new LinearLayout.LayoutParams(0, dp(46), 1); ax.setMargins(dp(8),0,0,0);
        actions.addView(sync, ax);
        LinearLayout.LayoutParams dx = new LinearLayout.LayoutParams(0, dp(46), 1); dx.setMargins(dp(8),0,0,0);
        actions.addView(danmaku, dx);
        LinearLayout.LayoutParams fx = new LinearLayout.LayoutParams(0, dp(46), 1); fx.setMargins(dp(8),0,0,0);
        actions.addView(fullscreen, fx);
        add(c, actions, -1, 46, 12);

        LinearLayout chatP = panel();
        chatP.addView(tv("岛上留言与弹幕雨", 18, Typeface.BOLD));
        chatP.addView(small("聊天像留言，弹幕像漂过银幕的小纸条，都会进入本场观影时间轴。"));
        chatLog = tv("还没有聊天。第一句可以留给今晚的电影。", 13, Typeface.NORMAL);
        chatLog.setTextColor(ink());
        chatLog.setMovementMethod(new ScrollingMovementMethod());
        chatLog.setMinHeight(dp(150));
        chatLog.setBackground(round(cardSoft(), 20));
        chatLog.setPadding(dp(14), dp(14), dp(14), dp(14));
        add(chatP, chatLog, -1, 170, 10);
        chatInput = input("今晚想和对方说什么？", "");
        add(chatP, chatInput, -1, 48, 10);
        LinearLayout cr = hbox();
        Button sendChat = btn("聊天", true);
        Button sendDm = btn("发弹幕", false);
        cr.addView(sendChat, new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams dmLp = new LinearLayout.LayoutParams(0, dp(44), 1); dmLp.setMargins(dp(8),0,0,0);
        cr.addView(sendDm, dmLp);
        add(chatP, cr, -1, 44, 8);
        add(c, chatP, -1, -2, 14);

        LinearLayout noteP = panel();
        noteP.addView(tv("时间轴笔记", 18, Typeface.BOLD));
        noteP.addView(small("记录某一幕、摘一句台词，最后自动落进票根和档案馆。"));
        noteInput = input("这一幕想记下什么？也可以直接写一句台词", "");
        noteInput.setSingleLine(false);
        add(noteP, noteInput, -1, 88, 10);
        Button addNote = btn("添加笔记", true);
        Button addQuote = btn("摘一句", false);
        LinearLayout noteButtons = hbox();
        noteButtons.addView(addNote, new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(0, dp(44), 1); qlp.setMargins(dp(8),0,0,0);
        noteButtons.addView(addQuote, qlp);
        add(noteP, noteButtons, -1, 44, 10);
        noteLog = tv("", 13, Typeface.NORMAL);
        noteLog.setTextColor(ink());
        noteLog.setMovementMethod(new ScrollingMovementMethod());
        noteLog.setBackground(round(cardSoft(), 20));
        noteLog.setPadding(dp(14), dp(14), dp(14), dp(14));
        add(noteP, noteLog, -1, 180, 10);
        add(c, noteP, -1, -2, 14);

        pick.setOnClickListener(v -> pickVideo());
        sync.setOnClickListener(v -> sendPlayback(true));
        danmaku.setOnClickListener(v -> { danmakuOn = !danmakuOn; danmaku.setText(danmakuOn ? "弹幕 ON" : "弹幕 OFF"); });
        fullscreen.setOnClickListener(v -> openCinemaFullscreen());
        sendChat.setOnClickListener(v -> sendMessage(false));
        sendDm.setOnClickListener(v -> sendMessage(true));
        addNote.setOnClickListener(v -> sendNote());
        addQuote.setOnClickListener(v -> sendQuoteLine());

        video.setOnPreparedListener(mp -> {
            View e = videoFrame.findViewWithTag("video_overlay");
            if (e != null) e.setVisibility(View.GONE);
            sendMovieInfo();
        });
        video.setOnCompletionListener(mp -> sendPlayback(true));
        video.setOnClickListener(v -> sendPlayback(false));

        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!applyingRemote && roomId.length() > 0 && video.getDuration() > 0) {
                    int sec = video.getCurrentPosition() / 1000;
                    if (sec != lastSentSecond) {
                        lastSentSecond = sec;
                        sendPlayback(false);
                    }
                }
                handler.postDelayed(this, 3000);
            }
        }, 3000);

        return wrap;
    }

    private LinearLayout buildCard() {
        LinearLayout wrap = vbox();
        wrap.setPadding(dp(16), topInset() + dp(8), dp(16), dp(12));
        LinearLayout c = vbox();
        wrap.addView(scroll(c), new LinearLayout.LayoutParams(-1, -1));

        LinearLayout head = hbox();
        TextView title = tv("票根工坊", 30, Typeface.NORMAL);
        title.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button refresh = btn("刷新", false);
        head.addView(refresh, new LinearLayout.LayoutParams(dp(86), dp(42)));
        c.addView(head);
        c.addView(small("三套模板：电影票根、片尾回执、观影明信片。选好模板再保存到档案馆。"));

        LinearLayout p = panel();
        p.setPadding(dp(14), dp(14), dp(14), dp(14));

        cardPreview = tv("等待生成。", 14, Typeface.NORMAL);
        cardPreview.setTextColor(ink());
        cardPreview.setPadding(dp(18), dp(18), dp(18), dp(18));
        cardPreview.setBackground(ticketBg());
        add(p, cardPreview, -1, -2, 0);

        TextView formTitle = tv("票根内容", 18, Typeface.BOLD);
        add(p, formTitle, -1, -2, 16);
        add(p, small("生成时会把观影邀请卡、台词、感想和时间轴笔记一起放进模板。"), -1, -2, 4);

        LinearLayout templateRow = hbox();
        Button tplTicket = btn("电影票根", cardTemplate.equals("ticket"));
        Button tplReceipt = btn("片尾回执", cardTemplate.equals("receipt"));
        Button tplPostcard = btn("观影明信片", cardTemplate.equals("postcard"));
        templateRow.addView(tplTicket, new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams trp1 = new LinearLayout.LayoutParams(0, dp(44), 1); trp1.setMargins(dp(8),0,0,0);
        templateRow.addView(tplReceipt, trp1);
        LinearLayout.LayoutParams trp2 = new LinearLayout.LayoutParams(0, dp(44), 1); trp2.setMargins(dp(8),0,0,0);
        templateRow.addView(tplPostcard, trp2);
        add(p, templateRow, -1, 44, 12);
        tplTicket.setOnClickListener(v -> setCardTemplate("ticket"));
        tplReceipt.setOnClickListener(v -> setCardTemplate("receipt"));
        tplPostcard.setOnClickListener(v -> setCardTemplate("postcard"));

        quoteInput = input("观影人A最喜欢的台词", "");
        linQuoteInput = input("观影人B最喜欢的台词", "");
        cardNoteInput = input("观影人A的观后感", "");
        linNoteInput = input("观影人B的观后感", "");
        cardNoteInput.setSingleLine(false);
        linNoteInput.setSingleLine(false);
        add(p, quoteInput, -1, 48, 12);
        add(p, linQuoteInput, -1, 48, 10);
        add(p, cardNoteInput, -1, 88, 10);
        add(p, linNoteInput, -1, 88, 10);

        LinearLayout row1 = hbox();
        Button make = btn("生成小卡片", true);
        Button saveRoom = btn("保存到房间", false);
        row1.addView(make, new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, dp(46), 1); slp.setMargins(dp(8),0,0,0);
        row1.addView(saveRoom, slp);
        add(p, row1, -1, 46, 12);

        LinearLayout row2 = hbox();
        Button collect = btn("存入档案馆", false);
        Button copy = btn("复制文案", false);
        row2.addView(collect, new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, dp(46), 1); clp.setMargins(dp(8),0,0,0);
        row2.addView(copy, clp);
        add(p, row2, -1, 46, 8);
        add(c, p, -1, -2, 16);

        refresh.setOnClickListener(v -> { if (roomId.length() > 0) { fetchRoom(); toast("正在刷新小卡片"); } else toast("先进入房间"); });
        make.setOnClickListener(v -> showTicketDialog());
        saveRoom.setOnClickListener(v -> saveCardToRoom());
        collect.setOnClickListener(v -> addFavorite(currentTicketText()));
        copy.setOnClickListener(v -> copyText(cardPreview.getText().toString()));

        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { renderCard(); }
            public void afterTextChanged(Editable e) {}
        };
        quoteInput.addTextChangedListener(watcher);
        linQuoteInput.addTextChangedListener(watcher);
        cardNoteInput.addTextChangedListener(watcher);
        linNoteInput.addTextChangedListener(watcher);
        return wrap;
    }

    private LinearLayout buildFavorites() {
        LinearLayout wrap = vbox();
        wrap.setPadding(dp(16), topInset() + dp(8), dp(16), dp(12));
        LinearLayout c = vbox();
        wrap.addView(scroll(c), new LinearLayout.LayoutParams(-1, -1));
        TextView title = tv("档案馆", 30, Typeface.BOLD);
        c.addView(title);
        c.addView(small("这里会收着每一次观影留下的票根、回执和明信片。"));
        LinearLayout p = panel();
        favoritesList = tv("档案馆还空着。看完一场电影，就把票根存进来。", 14, Typeface.NORMAL);
        favoritesList.setTextColor(ink());
        favoritesList.setPadding(dp(16), dp(16), dp(16), dp(16));
        favoritesList.setBackground(round(cardSoft(), 22));
        add(p, favoritesList, -1, -2, 0);
        Button clear = btn("清空档案馆", false);
        add(p, clear, -1, 44, 12);
        clear.setOnClickListener(v -> {
            getSharedPreferences("cineisle", 0).edit().remove("favorites").apply();
            refreshFavorites();
            toast("已清空档案馆");
        });
        add(c, p, -1, -2, 16);
        return wrap;
    }

    private GradientDrawable ticketBg() {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, cardCardColors());
        g.setCornerRadius(dp(28));
        g.setStroke(dp(1), theme.equals("cream") || theme.equals("matcha") ? color("#D9E4D6") : color("#33FFFFFF"));
        return g;
    }

    private String safeText(EditText e) {
        return e == null ? "" : e.getText().toString().trim();
    }

    private String currentTicketText() {
        collectInvitationSilently();
        String title = movieTitle.length() > 0 ? movieTitle : "今晚的影片";
        String room = roomId.length() > 0 ? roomId : "------";
        String partner = invitePartner.length() > 0 ? invitePartner : "观影人 A × 观影人 B";
        String mood = inviteMood.length() > 0 ? inviteMood : themeLabel();
        String invite = inviteNote.length() > 0 ? inviteNote : "今晚一起登岛看一场电影。";
        String zq = safeText(quoteInput);
        String lq = safeText(linQuoteInput);
        String zn = safeText(cardNoteInput);
        String ln = safeText(linNoteInput);
        if (zq.length() == 0) zq = remoteCard != null ? remoteCard.optString("quote", "这一幕被我们一起看见了。") : "这一幕被我们一起看见了。";
        if (lq.length() == 0) lq = remoteCard != null ? remoteCard.optString("linQuote", "爱是真的，留下却不一定是爱的唯一形式。") : "爱是真的，留下却不一定是爱的唯一形式。";
        if (zn.length() == 0) zn = remoteCard != null ? remoteCard.optString("zhiNote", "一起看电影这件事，本身就像把普通晚上藏进了一张小票根。") : "一起看电影这件事，本身就像把普通晚上藏进了一张小票根。";
        if (ln.length() == 0) ln = remoteCard != null ? remoteCard.optString("note", "这部电影把陪伴、理解、占有、边界和告别都放进一段温柔又遗憾的关系里。") : "这部电影把陪伴、理解、占有、边界和告别都放进一段温柔又遗憾的关系里。";
        String notes = (noteLog == null || noteLog.getText().length()==0) ? "还没有添加时间轴笔记。" : noteLog.getText().toString();
        if (cardTemplate.equals("receipt")) {
            return "CineIsle · 片尾回执\n" +
                    "━━━━━━━━━━━━━━━━\n" +
                    "影片｜" + title + "\n" +
                    "观影人｜" + partner + "\n" +
                    "氛围｜" + mood + "\n" +
                    "房间｜" + room + "\n\n" +
                    "开场备注\n" + invite + "\n\n" +
                    "片尾留下的一句话\n「" + zq + "」\n\n" +
                    "双人回执\n" + zn + "\n\n" + ln + "\n\n" +
                    "— after the credits · CineIsle";
        }
        if (cardTemplate.equals("postcard")) {
            return "CineIsle · 观影明信片\n" +
                    "━━━━━━━━━━━━━━━━\n" +
                    "寄给｜" + partner + "\n" +
                    "来自｜" + title + "\n" +
                    "邮戳｜" + mood + " · " + room + "\n\n" +
                    "今夜摘句\n「" + zq + "」\n「" + lq + "」\n\n" +
                    "明信片正文\n" + zn + "\n" + ln + "\n\n" +
                    "— from our private island";
        }
        return "CineIsle · Movie Ticket\n" +
                "━━━━━━━━━━━━━━━━\n" +
                "片名｜" + title + "\n" +
                "房间｜" + room + "\n" +
                "观影人｜" + partner + "\n" +
                "氛围｜" + mood + "\n" +
                "主题｜" + themeLabel() + "\n\n" +
                "邀请卡\n" + invite + "\n\n" +
                "观影人A喜欢的台词\n「" + zq + "」\n\n" +
                "观影人B喜欢的台词\n「" + lq + "」\n\n" +
                "观影人A的观后感\n" + zn + "\n\n" +
                "观影人B的观后感\n" + ln + "\n\n" +
                "时间轴笔记\n" + notes + "\n\n" +
                "— watch together · CineIsle";
}

    private String themeLabel() {
        if (theme.equals("cream")) return "奶油白";
        if (theme.equals("galaxy")) return "星河紫";
        if (theme.equals("matcha")) return "雾岛绿";
        if (theme.equals("film")) return "胶片黑";
        if (theme.equals("dusk")) return "暮光紫";
        return "夜航蓝";
    }

    private LinearLayout ticketView(String text) {
        LinearLayout box = vbox();
        box.setPadding(dp(22), dp(22), dp(22), dp(22));
        box.setBackground(ticketBg());
        TextView tag = small(cardTemplate.equals("receipt") ? "CineIsle · 片尾回执" : cardTemplate.equals("postcard") ? "CineIsle · 观影明信片" : "CineIsle · 纪念票根");
        tag.setTextColor(muted());
        box.addView(tag);
        TextView big = tv(cardTemplate.equals("receipt") ? "After Credits" : cardTemplate.equals("postcard") ? "Postcard" : "Movie Ticket", 25, Typeface.BOLD);
        big.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));
        add(box, big, -1, -2, 6);
        TextView content = tv(text, 14, Typeface.NORMAL);
        content.setTextColor(ink());
        content.setLineSpacing(dp(3), 1.08f);
        add(box, content, -1, -2, 12);
        TextView foot = small((invitePartner.length() > 0 ? invitePartner : "观影人 A × 观影人 B") + " · watch together");
        foot.setGravity(Gravity.CENTER);
        add(box, foot, -1, -2, 12);
        return box;
    }

    private void showTicketDialog() {
        renderCard();
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout outer = vbox();
        outer.setPadding(dp(16), dp(16), dp(16), dp(16));
        outer.setBackground(round(card(), 30));
        LinearLayout head = hbox();
        head.addView(tv("生成观影卡片", 22, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = chip("关闭");
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(close);
        outer.addView(head);
        outer.addView(small("按当前模板生成，可保存成图片，也可以存入档案馆。"));
        LinearLayout ticket = ticketView(currentTicketText());
        add(outer, ticket, -1, -2, 14);
        LinearLayout row = hbox();
        Button save = btn("保存图片", true);
        Button fav = btn("存档", false);
        row.addView(save, new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, dp(46), 1); flp.setMargins(dp(8),0,0,0);
        row.addView(fav, flp);
        add(outer, row, -1, 46, 12);
        save.setOnClickListener(v -> saveTicketImage(ticket));
        fav.setOnClickListener(v -> addFavorite(currentTicketText()));
        dialog.setContentView(scroll(outer));
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.CENTER);
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void saveTicketImage(View view) {
        try {
            Bitmap bm = bitmapFromView(view);
            String file = "cineisle_ticket_" + System.currentTimeMillis() + ".png";
            if (Build.VERSION.SDK_INT >= 29) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, file);
                values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CineIsle");
                Uri uri = getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception("无法创建图片文件");
                try(OutputStream os = getContentResolver().openOutputStream(uri)) { bm.compress(Bitmap.CompressFormat.PNG, 100, os); }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CineIsle");
                dir.mkdirs();
                File out = new File(dir, file);
                try(FileOutputStream fos = new FileOutputStream(out)) { bm.compress(Bitmap.CompressFormat.PNG, 100, fos); }
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(out)));
            }
            toast("小卡片已保存到相册");
        } catch(Exception e) { toast("保存失败：" + e.getMessage()); }
    }

    private Bitmap bitmapFromView(View v) {
        int width = getResources().getDisplayMetrics().widthPixels - dp(48);
        int wSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        v.measure(wSpec, hSpec);
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        Bitmap bm = Bitmap.createBitmap(v.getMeasuredWidth(), Math.max(1, v.getMeasuredHeight()), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        v.draw(c);
        return bm;
    }

    private void addFavorite(String text) {
        try {
            android.content.SharedPreferences sp = getSharedPreferences("cineisle", 0);
            JSONArray arr = new JSONArray(sp.getString("favorites", "[]"));
            JSONObject item = new JSONObject();
            item.put("id", System.currentTimeMillis()+"");
            item.put("title", movieTitle.length() > 0 ? movieTitle : "今晚的影片");
            item.put("room", roomId);
            item.put("text", text);
            item.put("at", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA).format(new java.util.Date()));
            arr.put(item);
            sp.edit().putString("favorites", arr.toString()).apply();
            refreshFavorites();
            toast("已存入档案馆");
        } catch(Exception e) { toast("收藏失败"); }
    }

    private void refreshFavorites() {
        if (favoritesList == null) return;
        try {
            JSONArray arr = new JSONArray(getSharedPreferences("cineisle", 0).getString("favorites", "[]"));
            if (arr.length() == 0) { favoritesList.setText("档案馆还空着。看完一场电影，就把票根存进来。"); return; }
            StringBuilder sb = new StringBuilder();
            for (int i = arr.length()-1; i >= 0; i--) {
                JSONObject it = arr.getJSONObject(i);
                sb.append("🎞 ").append(it.optString("title", "观影档案")).append("\n");
                sb.append(it.optString("at", "")).append(" · 房间 ").append(it.optString("room", "------")).append("\n");
                sb.append(it.optString("text", "")).append("\n\n");
                if (i > 0) sb.append("————————————\n\n");
            }
            favoritesList.setText(sb.toString());
        } catch(Exception e) { favoritesList.setText("收藏读取失败。"); }
    }

    private void saveCardToRoom() {
        if (roomId.length() == 0) { toast("先进入房间"); return; }
        if (serverUrl.length() == 0) { toast("先配置后端地址"); return; }
        final String zq = safeText(quoteInput);
        final String lq = safeText(linQuoteInput);
        final String zn = safeText(cardNoteInput);
        final String ln = safeText(linNoteInput);
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("title", movieTitle.length() > 0 ? movieTitle : "CineIsle放映记录");
                body.put("rating", 4.5);
                body.put("template", cardTemplate);
                body.put("partner", invitePartner);
                body.put("mood", inviteMood);
                body.put("inviteNote", inviteNote);
                body.put("quote", zq.length() > 0 ? zq : (lq.length() > 0 ? lq : "这一幕被我们一起看见了。"));
                body.put("note", ln.length() > 0 ? ln : zn);
                body.put("zhiQuote", zq);
                body.put("linQuote", lq);
                body.put("zhiNote", zn);
                body.put("linNote", ln);
                postJson("/api/rooms/" + roomId + "/card", body, true);
                runOnUiThread(() -> toast("已保存到房间小卡片"));
                fetchRoom();
            } catch(Exception e) { runOnUiThread(() -> toast("保存失败：" + e.getMessage())); }
        }).start();
    }

    private int[] cardCardColors() {
        if (theme.equals("cream")) return new int[]{color("#FFF6FA"), color("#FBFAFF")};
        if (theme.equals("galaxy")) return new int[]{color("#2A214B"), color("#18122E")};
        if (theme.equals("matcha")) return new int[]{color("#F8FFF5"), color("#EEF7EB")};
        if (theme.equals("film")) return new int[]{color("#2A2419"), color("#17130E")};
        if (theme.equals("dusk")) return new int[]{color("#35233F"), color("#1B1530")};
        return new int[]{color("#1A223B"), color("#11172A")};
    }

    private void rebuild() {
        root.removeAllViews();
        buildUI();
        updateHeroTexts();
        showPage(roomId.length() > 0 ? "room" : "home");
        renderCard();
    }

    private void updateHeroTexts() {
        if (homeStatus != null) homeStatus.setText(roomId.length() > 0 ? "当前房间：" + roomId : "当前还没有加入房间");
        if (heroBadge != null) heroBadge.setText("CineIsle · " + themeLabel());
        if (inviteSummary != null) inviteSummary.setText(invitationText());
        if (importState != null) importState.setText(fileName.length() > 0 ? "本机已导入 · " + movieTitle : "本地影片未导入 · 等待准备");
    }

    private void showPage(String page) {
        currentPage = page;
        pageHome.setVisibility(page.equals("home") ? View.VISIBLE : View.GONE);
        pageRoom.setVisibility(page.equals("room") ? View.VISIBLE : View.GONE);
        pageCard.setVisibility(page.equals("card") ? View.VISIBLE : View.GONE);
        pageFavorites.setVisibility(page.equals("favorites") ? View.VISIBLE : View.GONE);
        updateNav(page);
        if (page.equals("room") && roomId.length() > 0) startPolling();
        if (page.equals("card") && roomId.length() > 0) fetchRoom();
        if (page.equals("favorites")) refreshFavorites();
    }

    private void updateNav(String page) {
        if (navHome == null || navRoom == null || navCard == null || navFavorites == null) return;
        styleNav(navHome, page.equals("home"));
        styleNav(navRoom, page.equals("room"));
        styleNav(navCard, page.equals("card"));
        styleNav(navFavorites, page.equals("favorites"));
    }

    private void styleNav(Button b, boolean selected) {
        b.setTextColor(selected ? Color.WHITE : ink());
        b.setBackground(selected ? grad(new int[]{accent(), accent2()}, 20) : round(cardSoft(), 20));
        b.setElevation(selected ? dp(3) : 0);
    }

    private void collectSettings() {
        serverUrl = normalizeServer(serverUrl);
        if (name.length() == 0) name = "观影人";
        savePrefs();
    }

    private void collectInvitation() {
        collectInvitationSilently();
        savePrefs();
        updateHeroTexts();
    }

    private void collectInvitationSilently() {
        if (inviteMovieInput != null && safeText(inviteMovieInput).length() > 0) movieTitle = safeText(inviteMovieInput);
        if (invitePartnerInput != null && safeText(invitePartnerInput).length() > 0) invitePartner = safeText(invitePartnerInput);
        if (inviteMoodInput != null && safeText(inviteMoodInput).length() > 0) inviteMood = safeText(inviteMoodInput);
        if (inviteNoteInput != null && safeText(inviteNoteInput).length() > 0) inviteNote = safeText(inviteNoteInput);
    }

    private String invitationText() {
        String title = movieTitle.length() > 0 ? movieTitle : "等待写入片名";
        String partner = invitePartner.length() > 0 ? invitePartner : "观影人 A × 观影人 B";
        String mood = inviteMood.length() > 0 ? inviteMood : themeLabel();
        String note = inviteNote.length() > 0 ? inviteNote : "今晚一起登岛看一场电影。";
        return "影片｜" + title + "\n"
                + "观影人｜" + partner + "\n"
                + "氛围｜" + mood + "\n"
                + "开场备注｜" + note;
    }

    private void setCardTemplate(String tpl) {
        cardTemplate = tpl;
        savePrefs();
        toast(tpl.equals("receipt") ? "已切换片尾回执" : tpl.equals("postcard") ? "已切换观影明信片" : "已切换电影票根");
        rebuild();
        showPage("card");
    }

    private String normalizeServer(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.endsWith("/")) s = s.substring(0, s.length()-1);
        return s;
    }

    private void createRoom() {
        if (serverUrl.length() == 0) { toast("先在设置里填写后端地址"); openSettingsSheet(); return; }
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("name", name);
                body.put("theme", theme);
                if (movieTitle.length() > 0) body.put("title", movieTitle);
                body.put("partner", invitePartner);
                body.put("mood", inviteMood);
                body.put("inviteNote", inviteNote);
                JSONObject res = postJson("/api/rooms", body, false);
                JSONObject room = res.getJSONObject("room");
                runOnUiThread(() -> joinRoom(room.optString("id")));
            } catch (Exception e) { runOnUiThread(() -> toast("创建失败：" + e.getMessage())); }
        }).start();
    }

    private void joinRoom(String id) {
        roomId = id.trim().toUpperCase();
        if (roomId.length() == 0) { toast("先输入房间号"); return; }
        roomCodeView.setText("房间号 · " + roomId);
        roomTitle.setText(movieTitle.length() > 0 ? "正在放映 · " + movieTitle : "映屿 CineIsle");
        updateHeroTexts();
        showPage("room");
        startPolling();
        fetchRoom();
        toast("已进入房间 " + roomId);
    }

    private void startPolling() {
        if (!polling) {
            polling = true;
            handler.post(poller);
        }
    }

    private void pickVideo() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("video/*");
        startActivityForResult(i, 1001);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch(Exception ignored) {}
            fileName = getName(uri);
            movieTitle = fileName.replaceFirst("\\.[^.]+$", "");
            video.setVideoURI(uri);
            View e = videoFrame.findViewWithTag("video_overlay");
            if (e != null) e.setVisibility(View.GONE);
            video.requestFocus();
            roomTitle.setText((roomId.length() > 0 ? "正在放映 · " : "正在预览 · ") + movieTitle);
            showPage("room");
            toast("影片已导入，本地文件不会上传");
            sendMovieInfo();
            updateHeroTexts();
            renderCard();
        }
    }

    private String getName(Uri uri) {
        String result = "本地影片";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = cursor.getString(idx);
            }
        } catch(Exception ignored) {}
        return result;
    }

    private void sendMovieInfo() {
        if (roomId.length() == 0 || serverUrl.length() == 0 || fileName.length() == 0) return;
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("actor", name);
                body.put("title", movieTitle);
                body.put("fileName", fileName);
                body.put("duration", video.getDuration() / 1000.0);
                body.put("partner", invitePartner);
                body.put("mood", inviteMood);
                body.put("inviteNote", inviteNote);
                postJson("/api/rooms/" + roomId + "/playback", body, true);
            } catch(Exception ignored) {}
        }).start();
    }

    private void sendPlayback(boolean force) {
        if (roomId.length() == 0 || serverUrl.length() == 0 || applyingRemote) return;
        int pos = video.getCurrentPosition();
        boolean paused = !video.isPlaying();
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("actor", name);
                body.put("currentTime", pos / 1000.0);
                body.put("duration", Math.max(0, video.getDuration() / 1000.0));
                body.put("paused", paused);
                if (movieTitle.length() > 0) body.put("title", movieTitle);
                if (fileName.length() > 0) body.put("fileName", fileName);
                body.put("partner", invitePartner);
                body.put("mood", inviteMood);
                body.put("inviteNote", inviteNote);
                postJson("/api/rooms/" + roomId + "/playback", body, true);
            } catch(Exception ignored) {}
        }).start();
    }

    private void sendMessage(boolean dm) {
        String text = chatInput.getText().toString().trim();
        if (text.length() == 0) return;
        if (sendMessageText(text, dm)) chatInput.setText("");
    }

    private boolean sendMessageText(String text, boolean dm) {
        if (text == null) return false;
        text = text.trim();
        if (text.length() == 0) return false;
        if (roomId.length() == 0) { toast("先进入房间"); return false; }
        final String out = dm ? "弹幕：" + text : text;
        appendChat(name, out);
        if (dm && danmakuOn) showDanmaku(text);
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("name", name);
                body.put("text", out);
                postJson("/api/rooms/" + roomId + "/message", body, true);
            } catch(Exception e) { runOnUiThread(() -> toast("发送失败")); }
        }).start();
        return true;
    }

    private void sendNote() {
        String text = noteInput.getText().toString().trim();
        if (text.length() == 0) return;
        if (roomId.length() == 0) { toast("先进入房间"); return; }
        noteInput.setText("");
        appendNote(name, text, video.getCurrentPosition()/1000);
        postTimelineNote(text, "note");
    }

    private void sendQuoteLine() {
        String text = noteInput.getText().toString().trim();
        if (text.length() == 0) { toast("先写一句台词或金句"); return; }
        if (roomId.length() == 0) { toast("先进入房间"); return; }
        noteInput.setText("");
        String out = "金句｜" + text;
        appendNote(name, out, video.getCurrentPosition()/1000);
        if (quoteInput != null && safeText(quoteInput).length() == 0) quoteInput.setText(text);
        postTimelineNote(out, "quote");
    }

    private void postTimelineNote(String text, String type) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("name", name);
                body.put("text", text);
                body.put("type", type);
                body.put("time", video.getCurrentPosition()/1000.0);
                postJson("/api/rooms/" + roomId + "/note", body, true);
            } catch(Exception e) { runOnUiThread(() -> toast("时间轴发送失败")); }
        }).start();
    }

    private void fetchRoom() {
        if (serverUrl.length() == 0 || roomId.length() == 0) return;
        new Thread(() -> {
            try {
                JSONObject res = getJson("/api/rooms/" + roomId);
                JSONObject room = res.getJSONObject("room");
                runOnUiThread(() -> applyRoom(room));
            } catch(Exception ignored) {}
        }).start();
    }

    private void applyRoom(JSONObject room) {
        try {
            roomCodeView.setText("房间号 " + room.optString("id", roomId));
            if (room.optString("title").length() > 0 && !room.optString("title").equals("未命名影片")) {
                movieTitle = room.optString("title", movieTitle);
                roomTitle.setText("正在放映 · " + room.optString("title"));
            }
            if (room.optString("partner").length() > 0) invitePartner = room.optString("partner", invitePartner);
            if (room.optString("mood").length() > 0) inviteMood = room.optString("mood", inviteMood);
            if (room.optString("inviteNote").length() > 0) inviteNote = room.optString("inviteNote", inviteNote);
            if (inviteSummary != null) inviteSummary.setText(invitationText());
            if (importState != null) importState.setText((fileName.length() > 0 ? "本机已导入" : "本机未导入") + " · 片长 " + formatTime((int)room.optDouble("duration",0)));
            JSONArray members = room.optJSONArray("members");
            memberState.setText((members == null ? 0 : members.length()) + " 人在线");
            double t = room.optDouble("currentTime", 0);
            boolean paused = room.optBoolean("paused", true);
            syncState.setText((paused ? "已同步暂停" : "同步播放中") + " · " + formatTime((int)t));
            if (video.getDuration() > 0) {
                int remoteMs = (int)(t * 1000);
                if (Math.abs(video.getCurrentPosition() - remoteMs) > 1800) {
                    applyingRemote = true;
                    safeSeekTo(video, remoteMs);
                    handler.postDelayed(() -> applyingRemote = false, 800);
                }
                if (!paused && !video.isPlaying()) video.start();
                if (paused && video.isPlaying()) video.pause();
            }
            chatLog.setText("");
            JSONArray msgs = room.optJSONArray("messages");
            if (msgs != null) {
                for (int i = Math.max(0, msgs.length()-30); i < msgs.length(); i++) {
                    JSONObject m = msgs.getJSONObject(i);
                    String msgName = m.optString("name","观影人");
                    String msgText = m.optString("text","");
                    appendChat(msgName, msgText);

                    if (msgText.startsWith("弹幕：") && danmakuOn) {
                        String key = m.optString("id", "") + "|" + m.optString("at", "") + "|" + msgText;
                        if (!seenDanmakuKeys.contains(key)) {
                            seenDanmakuKeys.add(key);
                            showDanmaku(msgText.replaceFirst("^弹幕：", ""));
                        }
                    }
                }
            }
            if (chatLog.getText().length() == 0) chatLog.setText("还没有聊天。第一句可以留给今晚的电影。");
            noteLog.setText("");
            JSONArray notes = room.optJSONArray("notes");
            if (notes != null) {
                for (int i = Math.max(0, notes.length()-20); i < notes.length(); i++) {
                    JSONObject n = notes.getJSONObject(i);
                    appendNote(n.optString("name","观影人"), n.optString("text",""), (int)n.optDouble("time",0));
                }
            }
            JSONObject c = room.optJSONObject("card");
            if (c != null) {
                remoteCard = c;
                if (quoteInput != null && safeText(quoteInput).length() == 0) quoteInput.setText(c.optString("zhiQuote", c.optString("quote", "")));
                if (linQuoteInput != null && safeText(linQuoteInput).length() == 0) linQuoteInput.setText(c.optString("linQuote", ""));
                if (cardNoteInput != null && safeText(cardNoteInput).length() == 0) cardNoteInput.setText(c.optString("zhiNote", ""));
                if (linNoteInput != null && safeText(linNoteInput).length() == 0) linNoteInput.setText(c.optString("linNote", c.optString("note", "")));
                if (c.optString("template").length() > 0) cardTemplate = c.optString("template", cardTemplate);
            }
            renderCard();
        } catch(Exception ignored) {}
    }

    private JSONObject getJson(String path) throws Exception {
        URL url = new URL(serverUrl + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        String s = read(c);
        return new JSONObject(s);
    }

    private JSONObject postJson(String path, JSONObject body, boolean auth) throws Exception {
        URL url = new URL(serverUrl + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        if (auth && token.length() > 0) c.setRequestProperty("Authorization", "Bearer " + token);
        try(OutputStream os = c.getOutputStream()) {
            os.write(body.toString().getBytes("UTF-8"));
        }
        String s = read(c);
        return new JSONObject(s);
    }

    private String read(HttpURLConnection c) throws Exception {
        InputStream is = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private void appendChat(String who, String text) {
        if (text != null && text.startsWith("弹幕：")) {
            String danmakuKey = (who == null ? "" : who) + "|" + text;
            if (seenDanmakuKeys.add(danmakuKey)) {
                showDanmaku(text);
            }
        }

        String line = who + "： " + text;
        String old = chatLog.getText().toString();
        if (old.startsWith("还没有聊天")) old = "";
        chatLog.setText(old + (old.length()>0 ? "\n" : "") + line);
        if (fullChatLog != null) {
            String f = fullChatLog.getText().toString();
            if (f.startsWith("还没有聊天")) f = "";
            fullChatLog.setText(f + (f.length()>0 ? "\n" : "") + line);
        }
    }

    private void appendNote(String who, String text, int sec) {
        String old = noteLog.getText().toString();
        noteLog.setText(old + (old.length()>0 ? "\n\n" : "") + "◦ [" + formatTime(sec) + "] " + who + "\n" + text);
        renderCard();
    }






    private long lastSafeSeekAt = 0L;

    private void safeSeekTo(android.widget.VideoView target, int targetMs) {
        if (target == null) return;

        try {
            int currentMs = target.getCurrentPosition();
            int diffMs = Math.abs(currentMs - targetMs);
            boolean playing = target.isPlaying();
            long now = System.currentTimeMillis();

            // 播放中最怕频繁回拉：30 秒以内的进度差都让电影自然播放，不 seek
            if (playing && diffMs < 30000) {
                return;
            }

            // 暂停状态下，小误差也不用动
            if (!playing && diffMs < 1500) {
                return;
            }

            // 避免短时间连续 seek 导致画面抽搐
            if (now - lastSafeSeekAt < 5000 && diffMs < 60000) {
                return;
            }

            lastSafeSeekAt = now;
            target.seekTo(targetMs);
        } catch (Exception ignored) {}
    }

    private void showDanmaku(String text) {
        if (!danmakuOn) return;
        if (text == null) return;

        text = text.replaceFirst("^弹幕：", "").trim();
        if (text.length() == 0) return;

        final String finalText = text;

        runOnUiThread(() -> {
            try {
                // 横屏 Dialog 模式：直接加到横屏根布局上，避免被 Dialog 盖住
                if (fullscreenDanmakuRoot != null) {
                    final android.widget.FrameLayout layer = fullscreenDanmakuRoot;

                    final android.widget.TextView tv = new android.widget.TextView(this);
                    tv.setText(finalText);
                    tv.setTextColor(android.graphics.Color.WHITE);
                    tv.setTextSize(18);
                    tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    tv.setSingleLine(true);
                    tv.setPadding(dp(16), dp(8), dp(16), dp(8));
                    tv.setBackgroundColor(0xAA000000);

                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                        tv.setElevation(dp(120));
                    }

                    int w = layer.getWidth();
                    int h = layer.getHeight();
                    if (w <= 0) w = getResources().getDisplayMetrics().widthPixels;
                    if (h <= 0) h = getResources().getDisplayMetrics().heightPixels;

                    int y = dp(40) + new java.util.Random().nextInt(Math.max(1, h / 2));

                    android.widget.FrameLayout.LayoutParams lp =
                            new android.widget.FrameLayout.LayoutParams(
                                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                            );
                    lp.leftMargin = 0;
                    lp.topMargin = y;

                    tv.setTranslationX(w + dp(40));
                    layer.addView(tv, lp);
                    tv.bringToFront();

                    tv.post(() -> {
                        tv.animate()
                                .translationX(-tv.getWidth() - dp(100))
                                .setDuration(6500)
                                .withEndAction(() -> {
                                    try {
                                        layer.removeView(tv);
                                    } catch (Exception ignored) {}
                                })
                                .start();
                    });

                    return;
                }

                // 竖屏普通模式：使用全屏透明 PopupWindow，已经验证可见
                final android.view.View rootView = getWindow().getDecorView();

                final int screenW = getResources().getDisplayMetrics().widthPixels;
                final int screenH = getResources().getDisplayMetrics().heightPixels;

                final android.widget.FrameLayout layer = new android.widget.FrameLayout(this);
                layer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                layer.setClipChildren(false);
                layer.setClipToPadding(false);

                final android.widget.TextView tv = new android.widget.TextView(this);
                tv.setText(finalText);
                tv.setTextColor(android.graphics.Color.WHITE);
                tv.setTextSize(18);
                tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                tv.setSingleLine(true);
                tv.setPadding(dp(16), dp(8), dp(16), dp(8));
                tv.setBackgroundColor(0xAA000000);

                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    tv.setElevation(dp(100));
                    layer.setElevation(dp(100));
                }

                int y = dp(140);
                if (videoFrame != null && videoFrame.getHeight() > 0) {
                    int[] loc = new int[2];
                    videoFrame.getLocationInWindow(loc);
                    int topMin = loc[1] + dp(20);
                    int topMax = loc[1] + Math.max(dp(50), videoFrame.getHeight() / 2);
                    y = topMin + new java.util.Random().nextInt(Math.max(1, topMax - topMin));
                } else {
                    y = dp(120) + new java.util.Random().nextInt(Math.max(1, screenH / 3));
                }

                android.widget.FrameLayout.LayoutParams tvLp =
                        new android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                        );
                tvLp.leftMargin = 0;
                tvLp.topMargin = y;

                tv.setTranslationX(screenW + dp(40));
                layer.addView(tv, tvLp);

                final android.widget.PopupWindow popup = new android.widget.PopupWindow(
                        layer,
                        android.view.WindowManager.LayoutParams.MATCH_PARENT,
                        android.view.WindowManager.LayoutParams.MATCH_PARENT,
                        false
                );

                popup.setTouchable(false);
                popup.setFocusable(false);
                popup.setOutsideTouchable(false);
                popup.setClippingEnabled(false);
                popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    popup.setElevation(dp(100));
                }

                popup.showAtLocation(rootView, android.view.Gravity.NO_GRAVITY, 0, 0);

                tv.post(() -> {
                    tv.animate()
                            .translationX(-tv.getWidth() - dp(80))
                            .setDuration(6500)
                            .withEndAction(() -> {
                                try {
                                    popup.dismiss();
                                } catch (Exception ignored) {}
                            })
                            .start();
                });
            } catch (Exception e) {
                toast("弹幕显示失败：" + e.getMessage());
            }
        });
    }






    private void openCinemaFullscreen() {
        if (video == null || videoFrame == null) {
            toast("先进入房间页");
            return;
        }

        final FrameLayout originFrame = videoFrame;
        final ViewGroup oldParent = (ViewGroup) video.getParent();

        if (oldParent == null) {
            toast("先导入影片");
            return;
        }

        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } catch (Exception ignored) {}

        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setCanceledOnTouchOutside(false);

        FrameLayout root = new FrameLayout(this);
        fullscreenDanmakuRoot = root;
        root.setBackgroundColor(Color.BLACK);
        root.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        FrameLayout host = new FrameLayout(this);
        host.setBackgroundColor(Color.BLACK);
        host.setClipChildren(false);
        host.setClipToPadding(false);

        oldParent.removeView(video);
        host.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        videoFrame = host;

        root.addView(host, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout panel = vbox();
        panel.setPadding(dp(10), dp(10), dp(10), dp(10));
        panel.setBackground(round(color("#CC111827"), 22));

        TextView title = tv("观影弹幕间", 15, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        panel.addView(title);

        EditText input = input("边看边说点什么…", "");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(color("#CBD5E1"));
        add(panel, input, -1, 42, 8);

        LinearLayout row = hbox();
        Button chat = btn("聊天", true);
        Button dm = btn("弹幕", false);
        Button close = btn("退出", false);

        row.addView(chat, new LinearLayout.LayoutParams(0, dp(40), 1));

        LinearLayout.LayoutParams lpDm = new LinearLayout.LayoutParams(0, dp(40), 1);
        lpDm.setMargins(dp(6), 0, 0, 0);
        row.addView(dm, lpDm);

        LinearLayout.LayoutParams lpClose = new LinearLayout.LayoutParams(0, dp(40), 1);
        lpClose.setMargins(dp(6), 0, 0, 0);
        row.addView(close, lpClose);

        add(panel, row, -1, 40, 8);

        chat.setOnClickListener(v -> {
            if (chatInput != null) {
                chatInput.setText(input.getText().toString());
                sendMessage(false);
                input.setText("");
            }
        });

        dm.setOnClickListener(v -> {
            if (chatInput != null) {
                chatInput.setText(input.getText().toString());
                sendMessage(true);
                input.setText("");
            }
        });

        close.setOnClickListener(v -> d.dismiss());

        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(dp(320), -2, Gravity.RIGHT | Gravity.BOTTOM);
        pp.setMargins(0, 0, dp(10), dp(10));
        root.addView(panel, pp);

        final android.widget.TextView fullscreenChatToggle = new android.widget.TextView(this);
        fullscreenChatToggle.setText("收起弹幕间");
        fullscreenChatToggle.setTextColor(android.graphics.Color.WHITE);
        fullscreenChatToggle.setTextSize(13);
        fullscreenChatToggle.setGravity(android.view.Gravity.CENTER);
        fullscreenChatToggle.setPadding(dp(12), dp(7), dp(12), dp(7));

        android.graphics.drawable.GradientDrawable fullscreenChatToggleBg = new android.graphics.drawable.GradientDrawable();
        fullscreenChatToggleBg.setColor(0xAA111827);
        fullscreenChatToggleBg.setCornerRadius(dp(18));
        fullscreenChatToggle.setBackground(fullscreenChatToggleBg);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            fullscreenChatToggle.setElevation(dp(130));
        }

        android.widget.FrameLayout.LayoutParams fullscreenChatToggleLp =
                new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.view.Gravity.TOP | android.view.Gravity.RIGHT
                );
        fullscreenChatToggleLp.setMargins(0, dp(14), dp(14), 0);
        root.addView(fullscreenChatToggle, fullscreenChatToggleLp);
        fullscreenChatToggle.bringToFront();

        fullscreenChatToggle.setOnClickListener(v -> {
            if (panel.getVisibility() == android.view.View.VISIBLE) {
                panel.setVisibility(android.view.View.GONE);
                fullscreenChatToggle.setText("打开弹幕间");
            } else {
                panel.setVisibility(android.view.View.VISIBLE);
                fullscreenChatToggle.setText("收起弹幕间");
                panel.bringToFront();
                fullscreenChatToggle.bringToFront();
            }
        });


        d.setContentView(root);

        d.setOnDismissListener(x -> {
            fullscreenDanmakuRoot = null;
            try {
                ViewGroup p = (ViewGroup) video.getParent();
                if (p != null) p.removeView(video);
                originFrame.addView(video, 0, new FrameLayout.LayoutParams(-1, -1));
                videoFrame = originFrame;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } catch (Exception ignored) {}
        });

        d.show();

        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            w.setLayout(-1, -1);
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void renderCard() {
        if (cardPreview != null) {
            cardPreview.setText(currentTicketText());
            cardPreview.setBackground(ticketBg());
        }
    }

    private String formatTime(int sec) {
        sec = Math.max(0, sec);
        return (sec/60) + ":" + String.format("%02d", sec%60);
    }

    private void copyText(String s) {
        android.content.ClipboardManager cm = (android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("cineisle", s));
        toast("已复制");
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
