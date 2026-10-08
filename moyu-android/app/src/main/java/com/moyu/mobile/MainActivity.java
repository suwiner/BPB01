package com.moyu.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Patterns;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity {
  private static final int FILE_PICKER = 415;
  private static final int IMPORT_BOOKMARKS = 416;
  private static final int EXPORT_BOOKMARKS = 417;
  private static final int MAX_TABS = 16;
  private int accent(){return "blue".equals(pref.getString("theme","mono"))?0xff3268d3:(dark?Color.WHITE:0xff202020);}
  private static final String[] BLOCKED_DOMAINS = {
      "doubleclick.net","googlesyndication.com","google-analytics.com","googleadservices.com",
      "adsrvr.org","taboola.com","outbrain.com","scorecardresearch.com","adnxs.com"
  };
  private final ArrayList<Tab> tabs = new ArrayList<>();
  private SharedPreferences pref;
  private int index = 0;
  private boolean dark;
  private EditText addressBar;
  private ProgressBar progress;
  private Dialog sheet;
  private ValueCallback<Uri[]> upload;
  private View video;
  private WebChromeClient.CustomViewCallback videoCallback;

  private static final class Tab {
    final WebView web;
    String url = "";
    String title = "新标签页";
    boolean home = true;
    boolean desktop = false;
    Tab(WebView w) { web=w; }
  }

  @Override public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    pref = getSharedPreferences("moyu_mobile_preferences",MODE_PRIVATE);
    dark = "dark".equals(pref.getString("theme","mono"));
    MoyuReminderReceiver.createChannel(this);
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    WebView.setWebContentsDebuggingEnabled(false);
    try {
      JSONArray restore = new JSONArray(pref.getString("tabs","[]"));
      for(int i=0; i<Math.min(MAX_TABS,restore.length()); i++) {
        Tab tab=createTab();
        String url=restore.optString(i,"");
        if(validUrl(url)) { tab.home=false; tab.url=url; tab.web.loadUrl(url); }
      }
    } catch(Exception ignored) {}
    if(tabs.isEmpty()) createTab();
    index=Math.max(0,Math.min(pref.getInt("selected",0),tabs.size()-1));
    render();
    acceptLink(getIntent());
  }

  @Override protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    acceptLink(intent);
  }

  private void acceptLink(Intent intent) {
    if(intent!=null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData()!=null) {
      openTab(intent.getData().toString());
    }
  }
  private int d(float n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
  private int bg() {return dark?0xff121212:("blue".equals(pref.getString("theme","mono"))?0xfff7f9fd:0xfffafafa);}
  private int card() {return dark?0xff232323:Color.WHITE;}
  private int ink() {return dark?0xfffafafa:0xff1e1e1e;}
  private int muted() {return dark?0xffaaaaaa:0xff777777;}
  private int border() {return dark?0xff393939:0xffe9e9e9;}
  private GradientDrawable shape(int color,int radius,int stroke) {
    GradientDrawable x=new GradientDrawable();x.setColor(color);x.setCornerRadius(d(radius));
    if(stroke!=Color.TRANSPARENT)x.setStroke(d(1),stroke);
    return x;
  }
  private TextView label(String value,int size,int color,boolean medium) {
    TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);
    v.setGravity(Gravity.CENTER_VERTICAL);if(medium)v.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));
    return v;
  }
  private void message(String txt) { Toast.makeText(this,txt,Toast.LENGTH_SHORT).show(); }
  private void detach(View v) {if(v!=null && v.getParent() instanceof ViewGroup)((ViewGroup)v.getParent()).removeView(v);}
  private Tab active() {return tabs.get(index);}
  private boolean validUrl(String url) {return url!=null && (url.startsWith("https://")||url.startsWith("http://"));}
  private void saveSession() {
    JSONArray a=new JSONArray();
    for(Tab t:tabs) a.put(t.home?"":t.url);
    pref.edit().putString("tabs",a.toString()).putInt("selected",index).apply();
  }
  @Override protected void onPause() {super.onPause();saveSession();}
  @Override protected void onDestroy() {
    if(sheet!=null)sheet.dismiss();
    if(upload!=null){upload.onReceiveValue(null);upload=null;}
    for(Tab t:tabs){detach(t.web);t.web.destroy();}
    tabs.clear();super.onDestroy();
  }

  private Tab createTab() {
    final WebView web=new WebView(this);
    final Tab tab=new Tab(web);
    tabs.add(tab);
    WebSettings settings=web.getSettings();
    settings.setJavaScriptEnabled(pref.getBoolean("javascript",true));
    settings.setLoadsImagesAutomatically(pref.getBoolean("images",true));
    settings.setTextZoom(pref.getInt("text_zoom",100));
    settings.setDomStorageEnabled(true);
    settings.setSupportZoom(true);
    settings.setBuiltInZoomControls(true);
    settings.setDisplayZoomControls(false);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(true);
    settings.setJavaScriptCanOpenWindowsAutomatically(false);
    settings.setSupportMultipleWindows(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    settings.setMediaPlaybackRequiresUserGesture(true);
    if(Build.VERSION.SDK_INT>=26) settings.setSafeBrowsingEnabled(true);
    CookieManager.getInstance().setAcceptCookie(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
    web.setBackgroundColor(card());
    web.setWebViewClient(new WebViewClient() {
      @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req) {
        if(req.isForMainFrame()||!pref.getBoolean("tracker_block",true))return null;
        String host=req.getUrl().getHost();
        if(host==null)return null;
        host=host.toLowerCase(Locale.ROOT);
        for(String blocked:BLOCKED_DOMAINS) {
          if(host.equals(blocked)||host.endsWith("."+blocked)) {
            return new android.webkit.WebResourceResponse("text/plain","UTF-8",
                new java.io.ByteArrayInputStream(new byte[0]));
          }
        }
        return null;
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req) {
        if(!req.isForMainFrame())return false;
        Uri uri=req.getUrl();String s=uri.getScheme()==null?"":uri.getScheme().toLowerCase(Locale.ROOT);
        if("http".equals(s)||"https".equals(s))return false;
        if("tel".equals(s)||"mailto".equals(s)){
          try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception e){message("没有可以打开此链接的应用");}
        }
        return true;
      }
      @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon) {
        if(validUrl(url)){tab.url=url;tab.home=false;}
        if(!tabs.isEmpty() && active()==tab){
          if(addressBar!=null&&!addressBar.hasFocus()) addressBar.setText(url);
          if(progress!=null)progress.setVisibility(View.VISIBLE);
        }
      }
      @Override public void onPageFinished(WebView v,String url) {
        if(validUrl(url)){tab.url=url;recordHistory(tab.title,url);}
        if(!tabs.isEmpty() && active()==tab){
          if(addressBar!=null&&!addressBar.hasFocus()) addressBar.setText(url);
          if(progress!=null)progress.setVisibility(View.GONE);
        }
        saveSession();
      }
      @Override public void onReceivedSslError(WebView v,SslErrorHandler handler,SslError error) {
        handler.cancel();message("安全证书异常，已阻止访问");
      }
    });
    web.setWebChromeClient(new WebChromeClient(){
      @Override public void onReceivedTitle(WebView v,String title){
        if(title!=null&&!title.trim().isEmpty())tab.title=title;
      }
      @Override public void onProgressChanged(WebView v,int percent){
        if(!tabs.isEmpty()&&active()==tab&&progress!=null) {
          progress.setProgress(percent);
          progress.setVisibility(percent>=100?View.GONE:View.VISIBLE);
        }
      }
      @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams params){
        if(upload!=null)upload.onReceiveValue(null);
        upload=cb;
        try{startActivityForResult(params.createIntent(),FILE_PICKER);return true;}
        catch(Exception ex){upload=null;cb.onReceiveValue(null);message("文件选择器不可用");return false;}
      }
      @Override public void onPermissionRequest(PermissionRequest request){request.deny();}
      @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback cb){
        cb.invoke(origin,false,false);
      }
      @Override public void onShowCustomView(View v,CustomViewCallback cb) {
        if(video!=null){cb.onCustomViewHidden();return;}
        video=v;videoCallback=cb;
        FrameLayout decor=(FrameLayout)getWindow().getDecorView();
        decor.addView(v,new FrameLayout.LayoutParams(-1,-1));
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
      }
      @Override public void onHideCustomView(){exitVideo();}
    });
    web.setDownloadListener((url,userAgent,disposition,mime,length)->download(url,userAgent,disposition,mime));
    web.setOnLongClickListener(v->{
      WebView.HitTestResult hit=web.getHitTestResult();
      String link=hit==null?null:hit.getExtra();
      if(link==null||!validUrl(link))return false;
      final String chosen=link;
      new AlertDialog.Builder(MainActivity.this).setItems(
        new String[]{"在新标签页打开","复制链接","下载链接"},
        (dg,which)->{
          if(which==0)openTab(chosen);
          if(which==1)copyText(chosen);
          if(which==2)download(chosen,web.getSettings().getUserAgentString(),null,null);
        }).show();
      return true;
    });
    return tab;
  }

  private void exitVideo(){
    if(video==null)return;
    detach(video);video=null;
    WebChromeClient.CustomViewCallback cb=videoCallback;
    videoCallback=null;
    if(cb!=null)cb.onCustomViewHidden();
    bars();
  }
  @Override protected void onActivityResult(int code,int result,Intent data){
    super.onActivityResult(code,result,data);
    if(code==FILE_PICKER&&upload!=null){
      upload.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));
      upload=null;
    }
    if(result==RESULT_OK&&data!=null&&data.getData()!=null){
      if(code==IMPORT_BOOKMARKS)importBookmarks(data.getData());
      if(code==EXPORT_BOOKMARKS)exportBookmarks(data.getData());
    }
  }
  private String interpret(String input) {
    String s=input==null?"":input.trim();
    if(s.isEmpty())return "";
    String lc=s.toLowerCase(Locale.ROOT);
    if(lc.startsWith("https://")||lc.startsWith("http://"))return s;
    if(!s.contains(" ") && Patterns.WEB_URL.matcher(s).matches())return "https://"+s;
    try{
      String q=URLEncoder.encode(s,"UTF-8");
      return "baidu".equals(pref.getString("engine","google"))
        ? "https://www.baidu.com/s?wd="+q : "https://www.google.com/search?q="+q;
    }catch(Exception e){return "https://www.google.com/";}
  }
  private void go(String input){
    String url=interpret(input);if(url.isEmpty())return;
    Tab t=active();t.home=false;t.url=url;t.web.loadUrl(url);
    ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(t.web.getWindowToken(),0);
    render();saveSession();
  }
  private void openTab(String url){
    if(tabs.size()>=MAX_TABS){message("最多打开16个标签页");return;}
    createTab();index=tabs.size()-1;
    render();
    if(url!=null&&!url.trim().isEmpty())go(url);
    saveSession();
  }
  private void closeTab(int i){
    if(i<0||i>=tabs.size())return;
    Tab t=tabs.remove(i);detach(t.web);t.web.destroy();
    if(tabs.isEmpty())createTab();
    if(i<index)index--;
    index=Math.max(0,Math.min(index,tabs.size()-1));
    saveSession();render();
  }
  private void bars(){
    getWindow().setStatusBarColor(bg());
    getWindow().setNavigationBarColor(card());
    int flags=dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    getWindow().getDecorView().setSystemUiVisibility(flags);
  }
  private void render(){
    if(tabs.isEmpty())return;
    Tab t=active();detach(t.web);
    dark="dark".equals(pref.getString("theme","mono"));
    if(t.home)addressBar=null;
    bars();
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg());
    LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
    head.setPadding(d(18),d(7),d(14),d(7));
    if(t.home) {
      android.widget.ImageView logo=new android.widget.ImageView(this);
      logo.setImageResource(R.drawable.brand_logo);
      logo.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
      logo.setAdjustViewBounds(true);
      logo.setContentDescription("墨鱼浏览器 LOGO");
      logo.setPadding(d(2),d(2),d(2),d(2));
      head.addView(logo,new LinearLayout.LayoutParams(d(43),d(43)));
      TextView brand=label("墨鱼浏览器",18,ink(),true);
      LinearLayout.LayoutParams bl=new LinearLayout.LayoutParams(0,-1,1);bl.leftMargin=d(11);
      head.addView(brand,bl);
      head.addView(iconButton("plus","新建标签页",()->openTab("")),new LinearLayout.LayoutParams(d(44),d(46)));
    } else {
      LinearLayout inputWrap=new LinearLayout(this);inputWrap.setGravity(Gravity.CENTER_VERTICAL);
      inputWrap.setBackground(shape(card(),16,border()));inputWrap.setPadding(d(12),0,d(8),0);
      inputWrap.addView(new Glyph("search",muted()),new LinearLayout.LayoutParams(d(21),d(21)));
      addressBar=new EditText(this);
      addressBar.setSingleLine(true);addressBar.setText(t.url);
      addressBar.setTextSize(13);addressBar.setTextColor(ink());addressBar.setHintTextColor(muted());
      addressBar.setHint("搜索或输入网址");addressBar.setSelectAllOnFocus(true);
      addressBar.setBackgroundColor(Color.TRANSPARENT);addressBar.setPadding(d(9),0,d(2),0);
      addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
      addressBar.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
      addressBar.setOnEditorActionListener((v,action,event)->{
        if(action==EditorInfo.IME_ACTION_GO||(event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER)){
          go(v.getText().toString());return true;
        }return false;
      });
      inputWrap.addView(addressBar,new LinearLayout.LayoutParams(0,d(46),1));
      head.addView(inputWrap,new LinearLayout.LayoutParams(0,d(46),1));
      head.addView(iconButton(hasBookmark(t.url)?"starfill":"star","收藏网页",this::toggleBookmark),new LinearLayout.LayoutParams(d(43),d(44)));
      head.addView(iconButton("refresh","刷新网页",()->t.web.reload()),new LinearLayout.LayoutParams(d(37),d(44)));
    }
    root.addView(head,new LinearLayout.LayoutParams(-1,d(63)));
    progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    progress.setMax(100);progress.setProgressTintList(ColorStateList.valueOf(accent()));
    progress.setProgressBackgroundTintList(ColorStateList.valueOf(bg()));
    root.addView(progress,new LinearLayout.LayoutParams(-1,d(2)));
    progress.setVisibility(t.home||t.web.getProgress()>=100?View.GONE:View.VISIBLE);
    FrameLayout frame=new FrameLayout(this);
    if(t.home)frame.addView(home(),new FrameLayout.LayoutParams(-1,-1));
    else frame.addView(t.web,new FrameLayout.LayoutParams(-1,-1));
    root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
    View line=new View(this);line.setBackgroundColor(border());
    root.addView(line,new LinearLayout.LayoutParams(-1,d(1)));
    LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setBackgroundColor(card());
    navButton(nav,"home","首页",t.home,()->{t.home=true;t.title="新标签页";render();saveSession();});
    navButton(nav,"back","后退",false,()->{if(!t.home&&t.web.canGoBack())t.web.goBack();else if(!t.home){t.home=true;render();}});
    navButton(nav,"forward","前进",false,()->{if(!t.home&&t.web.canGoForward())t.web.goForward();});
    navButton(nav,"tabs","标签 "+tabs.size(),false,this::showTabs);
    navButton(nav,"menu","菜单",false,this::showMenu);
    root.addView(nav,new LinearLayout.LayoutParams(-1,d(68)));
    setContentView(root);
  }

  private View iconButton(String icon,String description,Runnable click){
    FrameLayout b=new FrameLayout(this);b.setContentDescription(description);b.setClickable(true);
    b.addView(new Glyph(icon,ink()),new FrameLayout.LayoutParams(d(23),d(23),Gravity.CENTER));
    b.setOnClickListener(v->click.run());return b;
  }
  private void navButton(LinearLayout nav,String glyph,String title,boolean selected,Runnable click){
    LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);
    cell.setGravity(Gravity.CENTER);cell.setClickable(true);cell.setContentDescription(title);
    cell.addView(new Glyph(glyph,selected?accent():muted()),new LinearLayout.LayoutParams(d(24),d(24)));
    TextView caption=label(title,10,selected?accent():muted(),selected);
    caption.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(-1,d(20));l.topMargin=d(5);
    cell.addView(caption,l);
    cell.setOnClickListener(v->click.run());
    nav.addView(cell,new LinearLayout.LayoutParams(0,-1,1));
  }
  private View home(){
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
    LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(d(24),d(52),d(24),d(24));
    TextView kicker=label("M O Y U    B R O W S E R",11,accent(),true);kicker.setGravity(Gravity.CENTER);
    body.addView(kicker,new LinearLayout.LayoutParams(-1,d(24)));
    TextView headline=label("探索，从这里开始。",26,ink(),true);headline.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams hl=new LinearLayout.LayoutParams(-1,d(57));hl.topMargin=d(9);body.addView(headline,hl);
    TextView sub=label("更轻 · 更快 · 更专注",13,muted(),false);sub.setGravity(Gravity.CENTER);
    body.addView(sub,new LinearLayout.LayoutParams(-1,d(27)));
    LinearLayout searchBox=new LinearLayout(this);searchBox.setGravity(Gravity.CENTER_VERTICAL);
    searchBox.setBackground(shape(card(),20,border()));searchBox.setPadding(d(16),0,d(14),0);
    searchBox.addView(new Glyph("search",accent()),new LinearLayout.LayoutParams(d(23),d(23)));
    EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(15);
    search.setTextColor(ink());search.setHintTextColor(muted());
    search.setHint("搜索内容或输入网址");search.setBackgroundColor(Color.TRANSPARENT);search.setPadding(d(12),0,0,0);
    search.setImeOptions(EditorInfo.IME_ACTION_GO);
    search.setOnEditorActionListener((v,action,event)->{
      if(action==EditorInfo.IME_ACTION_GO||(event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER)){
        go(v.getText().toString());return true;
      }return false;
    });
    searchBox.addView(search,new LinearLayout.LayoutParams(0,d(62),1));
    LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(-1,d(62));sl.topMargin=d(42);
    body.addView(searchBox,sl);
    if(pref.getBoolean("shortcuts",true)){
      LinearLayout labelRow=new LinearLayout(this);
      labelRow.setGravity(Gravity.CENTER_VERTICAL);
      TextView caption=label("常用网站",14,ink(),true);
      labelRow.addView(caption,new LinearLayout.LayoutParams(0,d(36),1));
      TextView edit=label("管理  ›",12,muted(),false);
      edit.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
      edit.setOnClickListener(v->manageShortcuts());
      labelRow.addView(edit,new LinearLayout.LayoutParams(d(82),d(38)));
      LinearLayout.LayoutParams cl=new LinearLayout.LayoutParams(-1,d(38));cl.topMargin=d(43);
      body.addView(labelRow,cl);
      JSONArray links=homeShortcuts();
      LinearLayout grid=null;
      for(int i=0;i<links.length()&&i<12;i++){
        if(i%4==0){
          grid=new LinearLayout(this);grid.setGravity(Gravity.CENTER);
          LinearLayout.LayoutParams gl=new LinearLayout.LayoutParams(-1,d(110));gl.topMargin=d(4);
          body.addView(grid,gl);
        }
        JSONObject ob=links.optJSONObject(i);
        if(ob!=null&&grid!=null){
          String name=ob.optString("title","网站"),url=ob.optString("url","");
          shortcut(grid,name.substring(0,1),name,url,accent());
        }
      }
      if(grid!=null)for(int missing=links.length()%4;missing>0&&missing<4;missing++){
        grid.addView(new View(this),new LinearLayout.LayoutParams(0,-1,1));
      }
    }
    TextView bottom=label("专注每一次探索",12,muted(),false);bottom.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams foot=new LinearLayout.LayoutParams(-1,d(36));foot.topMargin=d(62);
    body.addView(bottom,foot);
    scroll.addView(body);return scroll;
  }
  private void shortcut(LinearLayout row,String letter,String name,String url,int color) {
    LinearLayout box=new LinearLayout(this);box.setGravity(Gravity.CENTER);box.setOrientation(LinearLayout.VERTICAL);
    TextView icon=label(letter,letter.length()>2?13:20,ink(),true);icon.setGravity(Gravity.CENTER);
    icon.setBackground(shape(card(),17,border()));
    box.addView(icon,new LinearLayout.LayoutParams(d(59),d(59)));
    TextView txt=label(name,11,muted(),false);txt.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams tl=new LinearLayout.LayoutParams(-1,d(26));tl.topMargin=d(9);
    box.addView(txt,tl);box.setOnClickListener(v->go(url));
    row.addView(box,new LinearLayout.LayoutParams(0,-1,1));
  }

  private JSONArray getArray(String key) {
    try{return new JSONArray(pref.getString(key,"[]"));}catch(Exception ignored){return new JSONArray();}
  }
  private JSONObject entry(String title,String url){
    JSONObject o=new JSONObject();
    try{o.put("title",title==null||title.isEmpty()?url:title);
        o.put("url",url);o.put("when",System.currentTimeMillis());}catch(Exception ignored){}
    return o;
  }
  private void recordHistory(String title,String url){
    if(!validUrl(url))return;
    JSONArray old=getArray("history");JSONArray fresh=new JSONArray();fresh.put(entry(title,url));
    for(int i=0;i<old.length()&&fresh.length()<120;i++){
      JSONObject item=old.optJSONObject(i);
      if(item!=null&&!url.equals(item.optString("url")))fresh.put(item);
    }
    pref.edit().putString("history",fresh.toString()).apply();
  }
  private boolean hasBookmark(String url){
    if(!validUrl(url))return false;
    JSONArray a=getArray("bookmarks");
    for(int i=0;i<a.length();i++)if(url.equals(a.optJSONObject(i)==null?"":a.optJSONObject(i).optString("url")))return true;
    return false;
  }
  private void toggleBookmark(){
    Tab t=active();
    if(t.home||!validUrl(t.url)){message("请先打开网页");return;}
    JSONArray a=getArray("bookmarks"),out=new JSONArray();
    boolean removed=false;
    for(int i=0;i<a.length();i++){
      JSONObject item=a.optJSONObject(i);
      if(item!=null&&t.url.equals(item.optString("url")))removed=true;
      else if(item!=null)out.put(item);
    }
    if(!removed)out.put(entry(t.title,t.url));
    pref.edit().putString("bookmarks",out.toString()).apply();
    message(removed?"已取消收藏":"已添加到书签");render();
  }
  private void removeEntry(String key,String url) {
    JSONArray a=getArray(key),out=new JSONArray();
    for(int i=0;i<a.length();i++){
      JSONObject o=a.optJSONObject(i);
      if(o!=null&&!url.equals(o.optString("url")))out.put(o);
    }
    pref.edit().putString(key,out.toString()).apply();
  }
  private LinearLayout vertical(){
    LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;
  }
  private void panel(String title,LinearLayout content){
    if(sheet!=null)sheet.dismiss();
    Dialog dialog=new Dialog(this);
    LinearLayout outer=vertical();outer.setBackground(shape(card(),24,Color.TRANSPARENT));
    outer.setPadding(d(22),d(20),d(22),d(18));
    LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
    TextView t=label(title,20,ink(),true);
    heading.addView(t,new LinearLayout.LayoutParams(0,d(38),1));
    heading.addView(iconButton("close","关闭",dialog::dismiss),new LinearLayout.LayoutParams(d(38),d(38)));
    outer.addView(heading);
    ScrollView sv=new ScrollView(this);sv.setVerticalScrollBarEnabled(false);
    sv.addView(content);
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);
    sp.topMargin=d(13);outer.addView(sv,sp);
    dialog.setContentView(outer);
    Window window=dialog.getWindow();
    if(window!=null){
      window.setBackgroundDrawableResource(android.R.color.transparent);
      window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
      WindowManager.LayoutParams attr=window.getAttributes();
      attr.width=-1;attr.height=-2;attr.gravity=Gravity.BOTTOM;attr.dimAmount=0.34f;
      window.setAttributes(attr);
      window.setLayout(-1,-2);
    }
    dialog.setOnDismissListener(x->{if(sheet==dialog)sheet=null;});
    sheet=dialog;dialog.show();
    if(dialog.getWindow()!=null) {
      dialog.getWindow().setLayout(-1,-2);
      dialog.getWindow().setGravity(Gravity.BOTTOM);
    }
  }
  private void panelRow(LinearLayout target,String glyph,String title,String sub,Runnable click) {
    LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(d(5),d(10),d(5),d(10));
    row.addView(new Glyph(glyph,accent()),new LinearLayout.LayoutParams(d(25),d(25)));
    LinearLayout words=vertical();
    TextView name=label(title,15,ink(),true);
    words.addView(name,new LinearLayout.LayoutParams(-1,d(25)));
    if(sub!=null&&!sub.isEmpty()){
      TextView extra=label(sub,11,muted(),false);extra.setSingleLine(true);
      extra.setEllipsize(android.text.TextUtils.TruncateAt.END);
      words.addView(extra,new LinearLayout.LayoutParams(-1,d(21)));
    }
    LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);wp.leftMargin=d(14);row.addView(words,wp);
    TextView arrow=label("›",23,muted(),false);arrow.setGravity(Gravity.CENTER);
    row.addView(arrow,new LinearLayout.LayoutParams(d(22),d(36)));
    row.setOnClickListener(v->{if(sheet!=null)sheet.dismiss();click.run();});
    target.addView(row,new LinearLayout.LayoutParams(-1,-2));
    View sep=new View(this);sep.setBackgroundColor(border());
    target.addView(sep,new LinearLayout.LayoutParams(-1,d(1)));
  }
  private void showMenu(){
    LinearLayout list=vertical();
    panelRow(list,"tools","实用工具箱","电子便签、提醒、日报周报、翻译与分类导航",this::openTools);
    panelRow(list,"plus","新建标签页","重新开始探索",()->openTab(""));
    panelRow(list,"tabs","标签页管理","快速查看、切换与关闭标签",this::showTabs);
    panelRow(list,"star","收藏当前网页","快速保存正在浏览的网页",this::toggleBookmark);
    panelRow(list,"bookmark","书签管理","浏览、导入与导出书签",this::bookmarkMenu);
    panelRow(list,"history","历史记录","最近浏览的网站",()->showRecords("history","历史记录"));
    panelRow(list,"download","下载管理","查看已下载的文件",this::openDownloads);
    panelRow(list,"file","网页操作","查找、桌面模式、翻译及保存 PDF",this::showPageActions);
    panelRow(list,"share","分享网页","通过其他应用发送链接",this::share);
    panelRow(list,"settings","偏好设置","黑白主题、字号、搜索与隐私",this::showSettings);
    panel("快捷菜单",list);
  }
  private void showTabs(){
    LinearLayout list=vertical();
    panelRow(list,"plus","添加新标签页","",()->openTab(""));
    for(int i=0;i<tabs.size();i++){
      final int at=i;Tab t=tabs.get(i);
      LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
      row.setPadding(d(4),d(10),d(4),d(10));
      TextView marker=label(t.home?"⌂":String.valueOf(i+1),17,i==index?accent():muted(),true);
      marker.setGravity(Gravity.CENTER);marker.setBackground(shape(bg(),11,Color.TRANSPARENT));
      row.addView(marker,new LinearLayout.LayoutParams(d(44),d(44)));
      LinearLayout titleBox=vertical();
      TextView name=label(t.home?"新标签页":t.title,15,ink(),true);
      name.setSingleLine();name.setEllipsize(android.text.TextUtils.TruncateAt.END);
      titleBox.addView(name,new LinearLayout.LayoutParams(-1,d(24)));
      TextView url=label(t.home?"墨鱼浏览器":t.url,11,muted(),false);
      url.setSingleLine();url.setEllipsize(android.text.TextUtils.TruncateAt.END);
      titleBox.addView(url,new LinearLayout.LayoutParams(-1,d(22)));
      LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.leftMargin=d(10);row.addView(titleBox,bp);
      row.addView(iconButton("close","关闭标签页",()->{if(sheet!=null)sheet.dismiss();closeTab(at);showTabs();}),
        new LinearLayout.LayoutParams(d(40),d(46)));
      row.setOnClickListener(v->{if(sheet!=null)sheet.dismiss();index=at;render();saveSession();});
      list.addView(row);
      View sep=new View(this);sep.setBackgroundColor(border());list.addView(sep,new LinearLayout.LayoutParams(-1,d(1)));
    }
    panel("标签页 · "+tabs.size(),list);
  }
  private void showRecords(String key,String title) {
    LinearLayout list=vertical();JSONArray a=getArray(key);
    if(a.length()==0) {
      TextView empty=label("这里还没有内容",14,muted(),false);empty.setGravity(Gravity.CENTER);
      list.addView(empty,new LinearLayout.LayoutParams(-1,d(92)));
    }
    for(int i=0;i<a.length();i++){
      JSONObject item=a.optJSONObject(i);if(item==null)continue;
      String url=item.optString("url"),name=item.optString("title",url);
      LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
      LinearLayout texts=vertical();
      TextView top=label(name,15,ink(),true);top.setSingleLine();top.setEllipsize(android.text.TextUtils.TruncateAt.END);
      texts.addView(top,new LinearLayout.LayoutParams(-1,d(26)));
      TextView below=label(url,11,muted(),false);below.setSingleLine();
      below.setEllipsize(android.text.TextUtils.TruncateAt.END);
      texts.addView(below,new LinearLayout.LayoutParams(-1,d(24)));
      line.addView(texts,new LinearLayout.LayoutParams(0,d(61),1));
      line.addView(iconButton("close","删除记录",()->{
        removeEntry(key,url);if(sheet!=null)sheet.dismiss();showRecords(key,title);
      }),new LinearLayout.LayoutParams(d(38),d(48)));
      line.setOnClickListener(v->{if(sheet!=null)sheet.dismiss();go(url);});
      list.addView(line);
      View separator=new View(this);separator.setBackgroundColor(border());
      list.addView(separator,new LinearLayout.LayoutParams(-1,d(1)));
    }
    if("history".equals(key)&&a.length()>0)panelRow(list,"close","清空历史记录","仅删除本机浏览记录",()->new AlertDialog.Builder(this)
      .setTitle("清空历史记录？").setNegativeButton("取消",null)
      .setPositiveButton("清空",(dialog,which)->{pref.edit().remove("history").apply();message("浏览记录已清空");}).show());
    panel(title,list);
  }
  private void showSettings(){
    LinearLayout list=vertical();
    panelRow(list,"search","默认搜索引擎", "baidu".equals(pref.getString("engine","google"))?"百度":"Google",()->
      new AlertDialog.Builder(this).setTitle("选择搜索引擎")
        .setSingleChoiceItems(new String[]{"Google","百度"},"baidu".equals(pref.getString("engine","google"))?1:0,(dlg,choice)->{
          pref.edit().putString("engine",choice==1?"baidu":"google").apply();dlg.dismiss();showSettings();
        }).setNegativeButton("取消",null).show());
    String theme=pref.getString("theme","mono");
    panelRow(list,"moon","外观主题",theme.equals("dark")?"黑色夜间":(theme.equals("blue")?"淡蓝":"黑白极简（默认）"),()->
      new AlertDialog.Builder(this).setTitle("选择主题")
        .setSingleChoiceItems(new String[]{"黑白极简（默认）","深色黑白","柔和蓝白"},
            theme.equals("dark")?1:(theme.equals("blue")?2:0),(dg,choice)->{
              pref.edit().putString("theme",choice==1?"dark":(choice==2?"blue":"mono")).apply();
              dg.dismiss();dark=choice==1;render();showSettings();
            }).setNegativeButton("取消",null).show());
    panelRow(list,"home","首页常用网站",pref.getBoolean("shortcuts",true)?"显示（支持自定义）":"已隐藏",()->{
      boolean old=pref.getBoolean("shortcuts",true);
      pref.edit().putBoolean("shortcuts",!old).apply();render();showSettings();
    });
    panelRow(list,"bookmark","编辑常用网站","可新增、更名、删除或恢复初始网址",this::manageShortcuts);
    panelRow(list,"search","网页文字大小",pref.getInt("text_zoom",100)+"%",()->{
      int[] sizes={85,100,110,125,140,160};
      String[] choices={"85%","100%","110%","125%","140%","160%"};
      int selected=1;for(int i=0;i<sizes.length;i++)if(sizes[i]==pref.getInt("text_zoom",100))selected=i;
      new AlertDialog.Builder(this).setTitle("网页文字缩放")
        .setSingleChoiceItems(choices,selected,(dg,selectedIndex)->{
          int chosen=sizes[selectedIndex];pref.edit().putInt("text_zoom",chosen).apply();
          for(Tab tab:tabs)tab.web.getSettings().setTextZoom(chosen);
          dg.dismiss();showSettings();
        }).setNegativeButton("取消",null).show();
    });
    panelRow(list,"privacy","基础追踪域名拦截",pref.getBoolean("tracker_block",true)?"已启用":"已关闭",()->{
      pref.edit().putBoolean("tracker_block",!pref.getBoolean("tracker_block",true)).apply();
      showSettings();
    });
    panelRow(list,"privacy","JavaScript",pref.getBoolean("javascript",true)?"已启用":"已关闭",()->{
      boolean enabled=!pref.getBoolean("javascript",true);
      pref.edit().putBoolean("javascript",enabled).apply();
      for(Tab tab:tabs)tab.web.getSettings().setJavaScriptEnabled(enabled);
      showSettings();
    });
    panelRow(list,"file","网页图片",pref.getBoolean("images",true)?"已启用":"已关闭",()->{
      boolean enabled=!pref.getBoolean("images",true);
      pref.edit().putBoolean("images",enabled).apply();
      for(Tab tab:tabs)tab.web.getSettings().setLoadsImagesAutomatically(enabled);
      showSettings();
    });
    panelRow(list,"close","清除网站数据","删除缓存、Cookie 和浏览记录",()->new AlertDialog.Builder(this)
      .setTitle("清除网站数据？")
      .setMessage("此操作将清除本机网站缓存、Cookie、访问历史；不会删除书签。")
      .setNegativeButton("取消",null)
      .setPositiveButton("清除",(dlg,which)->clearSiteData()).show());
    TextView footer=label("墨鱼浏览器 Android 2.0.0\n独立原生工具箱 · 本地隐私存储 · Android WebView",12,muted(),false);
    footer.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams fl=new LinearLayout.LayoutParams(-1,d(78));fl.topMargin=d(18);
    list.addView(footer,fl);
    panel("偏好设置",list);
  }
  private void clearSiteData(){
    for(Tab t:tabs){t.web.clearCache(true);t.web.clearHistory();}
    CookieManager.getInstance().removeAllCookies(null);
    CookieManager.getInstance().flush();
    WebStorage.getInstance().deleteAllData();
    pref.edit().remove("history").apply();
    message("网站数据已清除");
  }

  /** User-editable home shortcuts. Saved locally; four per row for narrow screens. */
  private JSONArray homeShortcuts(){
    if(pref.contains("home_links"))return getArray("home_links");
    JSONArray initial=new JSONArray();
    String[][] defaults={
      {"百度","https://www.baidu.com"},{"Google","https://www.google.com"},
      {"哔哩哔哩","https://www.bilibili.com"},{"GitHub","https://github.com"}
    };
    for(String[] p:defaults)initial.put(entry(p[0],p[1]));
    pref.edit().putString("home_links",initial.toString()).apply();
    return initial;
  }
  private void manageShortcuts(){
    LinearLayout list=vertical();
    panelRow(list,"plus","增加常用网站","最多十二个，首页每行四个",()->editShortcut(-1));
    JSONArray shortcuts=homeShortcuts();
    for(int i=0;i<shortcuts.length();i++){
      JSONObject ob=shortcuts.optJSONObject(i);if(ob==null)continue;
      final int at=i;
      panelRow(list,"bookmark",ob.optString("title"),ob.optString("url"),()->
        new AlertDialog.Builder(this).setTitle("管理常用网站").setItems(new String[]{"编辑网站","删除网站"},
          (dialog,choice)->{
            if(choice==0)editShortcut(at);
            else {
              JSONArray all=homeShortcuts(),next=new JSONArray();
              for(int j=0;j<all.length();j++)if(j!=at)next.put(all.optJSONObject(j));
              pref.edit().putString("home_links",next.toString()).apply();
              render();manageShortcuts();
            }
          }).show());
    }
    panelRow(list,"refresh","恢复默认网站","重置为四个常用地址",()->
      new AlertDialog.Builder(this).setTitle("恢复默认常用网站？")
        .setNegativeButton("取消",null)
        .setPositiveButton("恢复",(dialog,which)->{
          pref.edit().remove("home_links").apply();render();manageShortcuts();
        }).show());
    panel("常用网站管理",list);
  }
  private void editShortcut(int editAt){
    JSONArray links=homeShortcuts();
    if(editAt<0&&links.length()>=12){message("最多添加12个网站");return;}
    JSONObject old=editAt>=0?links.optJSONObject(editAt):null;
    LinearLayout fields=vertical();fields.setPadding(d(18),d(4),d(18),d(5));
    EditText name=new EditText(this);
    name.setSingleLine();name.setTextColor(ink());name.setTextSize(15);
    name.setHint("网站名称");name.setText(old==null?"":old.optString("title"));
    fields.addView(name);
    EditText url=new EditText(this);
    url.setSingleLine();url.setTextColor(ink());url.setTextSize(14);
    url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
    url.setHint("https://example.com");url.setText(old==null?"":old.optString("url"));
    fields.addView(url);
    AlertDialog dlg=new AlertDialog.Builder(this).setTitle(editAt>=0?"编辑网站":"新增网站")
      .setView(fields).setNegativeButton("取消",null)
      .setPositiveButton("保存",null).create();
    dlg.setOnShowListener(x->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
      String title=name.getText().toString().trim(),link=interpret(url.getText().toString().trim());
      if(title.isEmpty()||!validUrl(link)){message("请输入名称和正确网址");return;}
      JSONArray updated=new JSONArray();
      for(int i=0;i<links.length();i++)updated.put(i==editAt?entry(title,link):links.optJSONObject(i));
      if(editAt<0)updated.put(entry(title,link));
      pref.edit().putString("home_links",updated.toString()).apply();
      dlg.dismiss();render();manageShortcuts();
    }));
    dlg.show();
  }

  private void openTools(){startActivity(new Intent(this,ToolsActivity.class));}
  private void bookmarkMenu(){
    LinearLayout list=vertical();
    panelRow(list,"bookmark","查看我的书签","本机收藏的网站",()->showRecords("bookmarks","我的书签"));
    panelRow(list,"download","从文件导入书签","支持 JSON 备份和 Netscape HTML",this::selectBookmarkImport);
    panelRow(list,"share","导出书签","生成 JSON 文件，可供下次导入",this::selectBookmarkExport);
    panel("书签管理",list);
  }
  private void selectBookmarkImport(){
    Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    in.setType("*/*");in.addCategory(Intent.CATEGORY_OPENABLE);
    in.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/html","application/json","text/plain"});
    try{startActivityForResult(in,IMPORT_BOOKMARKS);}catch(Exception e){message("无法打开系统文件选择器");}
  }
  private void selectBookmarkExport(){
    Intent out=new Intent(Intent.ACTION_CREATE_DOCUMENT);
    out.setType("application/json");
    out.addCategory(Intent.CATEGORY_OPENABLE);
    out.putExtra(Intent.EXTRA_TITLE,"moyu-bookmarks.json");
    try{startActivityForResult(out,EXPORT_BOOKMARKS);}catch(Exception e){message("无法创建书签文件");}
  }
  private void exportBookmarks(Uri uri){
    try(java.io.OutputStream stream=getContentResolver().openOutputStream(uri)){
      if(stream==null)throw new java.io.IOException();
      stream.write(getArray("bookmarks").toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
      message("书签已导出");
    }catch(Exception e){message("书签导出失败");}
  }
  private void importBookmarks(Uri uri){
    try(java.io.InputStream stream=getContentResolver().openInputStream(uri)){
      if(stream==null)throw new java.io.IOException();
      java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
      byte[] buffer=new byte[4096];int n;
      while((n=stream.read(buffer))!=-1){
        if(bytes.size()+n>3_000_000)throw new java.io.IOException("文件过大");
        bytes.write(buffer,0,n);
      }
      String raw=new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
      JSONArray imported=new JSONArray();
      if(raw.trim().startsWith("[")||raw.trim().startsWith("{")){
        JSONArray data=raw.trim().startsWith("[")?new JSONArray(raw):new JSONObject(raw).optJSONArray("bookmarks");
        if(data==null)throw new IllegalArgumentException("JSON format");
        for(int i=0;i<data.length()&&i<1000;i++){
          JSONObject item=data.optJSONObject(i);if(item==null)continue;
          String url=item.optString("url");if(!validUrl(url))continue;
          imported.put(entry(item.optString("title",url),url));
        }
      }else{
        java.util.regex.Matcher links=java.util.regex.Pattern.compile(
          "<a\\\\b[^>]*?href\\\\s*=\\\\s*[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>",
          java.util.regex.Pattern.CASE_INSENSITIVE|java.util.regex.Pattern.DOTALL).matcher(raw);
        while(links.find()&&imported.length()<1000){
          String url=links.group(1).replace("&amp;","&");
          if(!validUrl(url))continue;
          String title=android.text.Html.fromHtml(links.group(2),android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim();
          imported.put(entry(title.isEmpty()?url:title,url));
        }
      }
      JSONArray old=getArray("bookmarks"),merged=new JSONArray();
      java.util.HashSet<String> seen=new java.util.HashSet<>();
      for(int i=0;i<old.length();i++){
        JSONObject item=old.optJSONObject(i);
        if(item!=null&&validUrl(item.optString("url"))&&seen.add(item.optString("url")))merged.put(item);
      }
      int added=0;
      for(int i=0;i<imported.length();i++){
        JSONObject item=imported.optJSONObject(i);
        if(item!=null&&seen.add(item.optString("url"))){merged.put(item);added++;}
      }
      pref.edit().putString("bookmarks",merged.toString()).apply();
      message("成功导入"+added+"个新书签");
    }catch(Exception e){message("书签格式不支持或读取失败");}
  }
  private void showPageActions(){
    LinearLayout list=vertical();
    Tab tab=active();
    panelRow(list,"search","网页内查找","按关键词定位网页文本",this::findInPage);
    panelRow(list,"desktop","切换电脑网页",tab.desktop?"当前：电脑版":"当前：手机版",this::toggleDesktop);
    panelRow(list,"file","保存网页为 PDF","通过 Android 系统打印保存",this::printPage);
    panelRow(list,"share","复制网页链接","复制到系统剪贴板",()->copyText(active().url));
    panelRow(list,"tools","翻译当前网页","打开 Google 翻译网页版",this::translatePage);
    panel("网页操作",list);
  }
  private void findInPage(){
    if(active().home){message("请先打开网页");return;}
    EditText term=new EditText(this);term.setSingleLine();term.setHint("网页内搜索");
    term.setPadding(d(22),d(4),d(22),d(4));
    WebView view=active().web;
    new AlertDialog.Builder(this).setTitle("网页内查找").setView(term)
      .setPositiveButton("查找",(dlg,which)->view.findAllAsync(term.getText().toString()))
      .setNeutralButton("下一处",(dlg,which)->view.findNext(true))
      .setNegativeButton("关闭",(dlg,which)->view.clearMatches()).show();
  }
  private void toggleDesktop(){
    Tab tab=active();
    if(tab.home){message("请先打开网页");return;}
    tab.desktop=!tab.desktop;
    tab.web.getSettings().setUserAgentString(tab.desktop?
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36":null);
    tab.web.getSettings().setUseWideViewPort(true);
    tab.web.getSettings().setLoadWithOverviewMode(tab.desktop);
    tab.web.reload();
    message(tab.desktop?"已启用电脑网页":"已恢复手机网页");
  }
  private void printPage(){
    Tab tab=active();if(tab.home){message("请先打开网页");return;}
    android.print.PrintManager pm=(android.print.PrintManager)getSystemService(Context.PRINT_SERVICE);
    if(pm==null){message("设备不支持打印服务");return;}
    try{
      pm.print("墨鱼浏览器 - "+tab.title,tab.web.createPrintDocumentAdapter("墨鱼网页"),
        new android.print.PrintAttributes.Builder().build());
    }catch(Exception e){message("无法生成打印文件");}
  }
  private void translatePage(){
    if(active().home||!validUrl(active().url)){message("请先打开网页");return;}
    try{openTab("https://translate.google.com/translate?sl=auto&tl=zh-CN&u="+
      java.net.URLEncoder.encode(active().url,"UTF-8"));}
    catch(Exception e){message("网页翻译暂不可用");}
  }
  private void copyText(String text){
    if(text==null||text.isEmpty()){message("没有可复制的链接");return;}
    ((android.content.ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE))
      .setPrimaryClip(android.content.ClipData.newPlainText("墨鱼浏览器",text));
    message("已复制到剪贴板");
  }
  private void share(){
    Tab t=active();
    if(t.home||!validUrl(t.url)){message("请先打开网页");return;}
    Intent out=new Intent(Intent.ACTION_SEND);out.setType("text/plain");
    out.putExtra(Intent.EXTRA_TEXT,t.title+"\n"+t.url);
    startActivity(Intent.createChooser(out,"分享网页"));
  }
  private void openDownloads(){
    try{startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));}
    catch(Exception e){message("系统下载管理器不可用");}
  }
  private void download(String url,String userAgent,String disposition,String mime){
    if(!validUrl(url)){message("此下载地址暂不支持");return;}
    try{
      DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url));
      r.setTitle(URLUtil.guessFileName(url,disposition,mime));
      r.setDescription("墨鱼浏览器文件下载");
      r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
      if(mime!=null)r.setMimeType(mime);
      if(userAgent!=null)r.addRequestHeader("User-Agent",userAgent);
      String cookies=CookieManager.getInstance().getCookie(url);
      if(cookies!=null)r.addRequestHeader("Cookie",cookies);
      if(Build.VERSION.SDK_INT>=29)r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
        URLUtil.guessFileName(url,disposition,mime));
      ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
      message("已加入下载任务");
    }catch(Exception e){message("下载失败，请检查链接或存储空间");}
  }
  @Override public void onBackPressed(){
    if(video!=null){exitVideo();return;}
    if(sheet!=null){sheet.dismiss();return;}
    Tab t=active();
    if(!t.home&&t.web.canGoBack())t.web.goBack();
    else if(!t.home){t.home=true;render();}
    else if(tabs.size()>1){closeTab(index);}
    else super.onBackPressed();
  }

  private final class Glyph extends View {
    private final String kind;private final int color;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    Glyph(String kind,int color){super(MainActivity.this);this.kind=kind;this.color=color;}
    @Override protected void onDraw(Canvas c) {
      super.onDraw(c);int save=c.save();c.scale(getWidth()/24f,getHeight()/24f);
      p.setColor(color);p.setStrokeWidth(1.8f);p.setStyle(Paint.Style.STROKE);
      p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
      switch(kind){
        case "home":{
          Path path=new Path();path.moveTo(3,10);path.lineTo(12,3);path.lineTo(21,10);
          path.moveTo(5,9);path.lineTo(5,21);path.lineTo(19,21);path.lineTo(19,9);
          path.moveTo(10,21);path.lineTo(10,14);path.lineTo(14,14);path.lineTo(14,21);
          c.drawPath(path,p);break;
        }
        case "back":c.drawLine(19,12,5,12,p);c.drawLine(11,6,5,12,p);c.drawLine(5,12,11,18,p);break;
        case "forward":c.drawLine(5,12,19,12,p);c.drawLine(13,6,19,12,p);c.drawLine(19,12,13,18,p);break;
        case "search":c.drawCircle(10.5f,10.5f,6.6f,p);c.drawLine(15.6f,15.6f,21,21,p);break;
        case "plus":c.drawLine(12,4,12,20,p);c.drawLine(4,12,20,12,p);break;
        case "close":c.drawLine(5,5,19,19,p);c.drawLine(19,5,5,19,p);break;
        case "refresh":{
          c.drawArc(4,4,20,20,45,285,false,p);c.drawLine(20,7,20,13,p);c.drawLine(20,13,15,12,p);break;
        }
        case "tabs":c.drawRoundRect(3,3,18,19,3,3,p);c.drawLine(8,22,21,22,p);c.drawLine(21,22,21,8,p);break;
        case "menu":c.drawCircle(5,12,1.4f,p);c.drawCircle(12,12,1.4f,p);c.drawCircle(19,12,1.4f,p);break;
        case "star":case "starfill":{
          Path star=new Path();
          for(int i=0;i<10;i++){
            double a=-Math.PI/2+i*Math.PI/5;
            float r=i%2==0?10:4.5f;float x=12+(float)Math.cos(a)*r,y=12+(float)Math.sin(a)*r;
            if(i==0)star.moveTo(x,y);else star.lineTo(x,y);
          }
          star.close();if("starfill".equals(kind))p.setStyle(Paint.Style.FILL);
          c.drawPath(star,p);break;
        }
        case "bookmark":c.drawRoundRect(5,3,19,21,2,2,p);c.drawLine(8,10,16,10,p);c.drawLine(8,14,14,14,p);break;
        case "history":c.drawCircle(12,12,9,p);c.drawLine(12,7,12,12,p);c.drawLine(12,12,16,14,p);break;
        case "download":c.drawLine(12,3,12,15,p);c.drawLine(7,10,12,15,p);c.drawLine(12,15,17,10,p);c.drawLine(4,20,20,20,p);break;
        case "share":c.drawRoundRect(4,10,20,21,2,2,p);c.drawLine(12,3,12,15,p);c.drawLine(7,8,12,3,p);c.drawLine(12,3,17,8,p);break;
        case "settings":c.drawCircle(12,12,3.3f,p);c.drawCircle(12,12,9,p);c.drawLine(12,1,12,4,p);c.drawLine(12,20,12,23,p);c.drawLine(1,12,4,12,p);c.drawLine(20,12,23,12,p);break;
        case "moon":c.drawArc(3,2,21,21,60,250,false,p);c.drawLine(7,18,12,21,p);break;
        default:c.drawCircle(12,12,8,p);
      }
      c.restoreToCount(save);
    }
  }
}
