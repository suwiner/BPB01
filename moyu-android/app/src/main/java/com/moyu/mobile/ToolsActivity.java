package com.moyu.mobile;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Mobile-native tools: kept in a second-level screen, never crowding the new-tab page. */
public final class ToolsActivity extends Activity {
    private SharedPreferences store;
    private boolean dark, blue;
    private LinearLayout body;
    private String page = "tools";
    private static final String NOTES = "moyu_mobile_notes_v2";
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        store = getSharedPreferences("moyu_mobile_preferences",MODE_PRIVATE);
        dark = "dark".equals(store.getString("theme","mono"));
        blue = "blue".equals(store.getString("theme","mono"));
        MoyuReminderReceiver.createChannel(this);
        showHome();
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private int bg(){return dark?0xff131313:0xfffafafa;}
    private int card(){return dark?0xff222222:Color.WHITE;}
    private int textColor(){return dark?Color.WHITE:0xff191919;}
    private int secondary(){return dark?0xffb0b0b0:0xff777777;}
    private int border(){return dark?0xff383838:0xffe8e8e8;}
    private int accent(){return blue?0xff3266d6:(dark?Color.WHITE:0xff1c1c1c);}
    private GradientDrawable rounded(int color, int radius){
        GradientDrawable shape=new GradientDrawable();shape.setColor(color);
        shape.setCornerRadius(dp(radius));shape.setStroke(dp(1),border());return shape;
    }
    private TextView t(String value,int size,boolean bold,int color){
        TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        return v;
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private void page(String name,String title,String tagline){
        page=name;
        dark="dark".equals(store.getString("theme","mono"));
        blue="blue".equals(store.getString("theme","mono"));
        getWindow().setStatusBarColor(bg());getWindow().setNavigationBarColor(bg());
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout outer=vertical();outer.setBackgroundColor(bg());
        LinearLayout toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(14),dp(8),dp(20),dp(8));
        TextView back=t("‹",34,false,textColor());back.setGravity(Gravity.CENTER);back.setContentDescription("返回");
        toolbar.addView(back,new LinearLayout.LayoutParams(dp(50),dp(48)));
        back.setOnClickListener(v->showHome());
        TextView heading=t(title,19,true,textColor());toolbar.addView(heading,new LinearLayout.LayoutParams(0,dp(48),1));
        outer.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(66)));
        View line=new View(this);line.setBackgroundColor(border());outer.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.setVerticalScrollBarEnabled(false);
        body=vertical();body.setPadding(dp(22),dp(23),dp(22),dp(50));
        if(tagline!=null&&!tagline.isEmpty()){
            TextView sub=t(tagline,13,false,secondary());sub.setLineSpacing(dp(2),1f);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(23);body.addView(sub,lp);
        }
        sv.addView(body);outer.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(outer);
    }
    private void section(String value){
        TextView label=t(value,13,true,secondary());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(34));p.topMargin=dp(14);body.addView(label,p);
    }
    private void item(String symbol,String title,String subtitle,Runnable action){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(14),dp(10),dp(12),dp(10));
        row.setBackground(rounded(card(),17));
        TextView icon=t(symbol,20,true,textColor());icon.setGravity(Gravity.CENTER);
        icon.setBackground(rounded(bg(),12));
        row.addView(icon,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout words=vertical();
        TextView first=t(title,15,true,textColor());words.addView(first,new LinearLayout.LayoutParams(-1,dp(25)));
        if(subtitle!=null&&!subtitle.isEmpty()){
            TextView second=t(subtitle,11,false,secondary());second.setSingleLine();
            second.setEllipsize(TextUtils.TruncateAt.END);
            words.addView(second,new LinearLayout.LayoutParams(-1,dp(20)));
        }
        LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);wp.leftMargin=dp(13);row.addView(words,wp);
        TextView right=t("›",25,false,secondary());right.setGravity(Gravity.CENTER);
        row.addView(right,new LinearLayout.LayoutParams(dp(20),dp(40)));
        row.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(11);body.addView(row,rp);
    }
    private Button button(String title,Runnable action){
        Button b=new Button(this);b.setAllCaps(false);b.setText(title);b.setTextSize(14);
        b.setTextColor(dark?Color.BLACK:Color.WHITE);
        b.setBackground(rounded(accent(),14));b.setOnClickListener(v->action.run());
        return b;
    }
    private EditText input(String hint, String value, boolean multi){
        EditText e=new EditText(this);e.setTextSize(15);e.setTextColor(textColor());e.setHintTextColor(secondary());e.setHint(hint);
        e.setText(value);e.setBackground(rounded(card(),15));e.setPadding(dp(15),dp(11),dp(15),dp(11));
        if(multi){e.setGravity(Gravity.TOP);e.setMinLines(5);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}
        else{e.setSingleLine();e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}
        return e;
    }
    private void put(View v,int top){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);body.addView(v,lp);
    }
    private JSONArray load(String key){try{return new JSONArray(store.getString(key,"[]"));}catch(Exception ex){return new JSONArray();}}
    private void saveArray(String key,JSONArray arr){store.edit().putString(key,arr.toString()).apply();}
    private JSONObject newNote(String id,String title,String contents,String type,long due,int every,boolean done){
        JSONObject ob=new JSONObject();
        try{ob.put("id",id);ob.put("title",title);ob.put("body",contents);ob.put("type",type);
            ob.put("due",due);ob.put("every",every);ob.put("done",done);ob.put("updated",System.currentTimeMillis());}catch(Exception ignored){}
        return ob;
    }
    private void persistNote(JSONObject updated){
        JSONArray old=load(NOTES),out=new JSONArray();
        String id=updated.optString("id");boolean found=false;
        for(int i=0;i<old.length();i++){
            JSONObject ob=old.optJSONObject(i);
            if(ob==null)continue;
            if(id.equals(ob.optString("id"))){out.put(updated);found=true;}else out.put(ob);
        }
        if(!found)out.put(updated);
        saveArray(NOTES,out);
        MoyuReminderReceiver.schedule(this,updated);
    }
    private void eraseNote(JSONObject deleted){
        String id=deleted.optString("id");
        JSONArray old=load(NOTES),out=new JSONArray();
        for(int i=0;i<old.length();i++){
            JSONObject ob=old.optJSONObject(i);
            if(ob!=null&&!id.equals(ob.optString("id")))out.put(ob);
        }
        saveArray(NOTES,out);
        MoyuReminderReceiver.cancel(this,id);
    }

    private void showHome(){
        page("tools","工具箱","工具回归，但不占用浏览器首页。按工作、效率与生活分类，需要时再打开。");
        section("工作与记录");
        item("✎","电子便签","本地保存 · 新建、修改和删除",()->showNotes("note"));
        item("☑","待办与提醒","完成时间 · 未完成循环提醒 · 可关闭",()->showNotes("todo"));
        item("▤","工作日报","每天记录，可编辑并设置提醒",()->showReport(false,0));
        item("▦","工作周报","一键汇总本周日报，仍可修改",()->showReport(true,0));
        section("网页与效率");
        item("文","免费文字翻译","中英互译，无需在浏览器内登录",this::translate);
        item("⌕","AI 资讯","打开官方 AI 发布源",this::news);
        item("▦","分类网址导航","设计、编程、电商等八大分类",this::directory);
        item("＝","计算器","原生离线计算",this::calculator);
        section("生活与账户");
        item("☁","天气查询","打开天气预报页面",()->open("https://www.accuweather.com/"));
        item("日","农历与节气","查农历与二十四节气",()->open("https://www.baidu.com/s?wd=%E4%BB%8A%E6%97%A5%E5%86%9C%E5%8E%86%E8%8A%82%E6%B0%94"));
        item("▣","账号与密码自动填充","由 Android 系统密码服务保护",this::autofillSettings);
    }
    private void showNotes(String type){
        boolean task="todo".equals(type);
        page(type,task?"待办与提醒":"电子便签",task?"可设置完成时间与重复提醒；完成或关闭后不再提醒。":"所有便签仅保存在当前设备，可随时编辑。");
        put(button(task?"＋ 新建待办":"＋ 新建便签",()->editNote(type,null)),0);
        JSONArray notes=load(NOTES);int count=0;
        for(int i=notes.length()-1;i>=0;i--){
            JSONObject ob=notes.optJSONObject(i);
            if(ob==null||!type.equals(ob.optString("type")))continue;
            count++;
            String id=ob.optString("id");
            String name=ob.optString("title","未命名");
            boolean done=ob.optBoolean("done");
            long due=ob.optLong("due");
            String detail=task?(done?"已完成":(due>0?"完成时间 "+new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date(due)):"未设置完成时间")):ob.optString("body");
            item(task?(done?"☑":"□"):"✎",name,detail,()->noteActions(id,type));
        }
        if(count==0){TextView empty=t("这里还没有内容。",13,false,secondary());empty.setGravity(Gravity.CENTER);put(empty,36);}
    }
    private JSONObject findNote(String id){
        JSONArray a=load(NOTES);for(int i=0;i<a.length();i++){
            JSONObject ob=a.optJSONObject(i);
            if(ob!=null&&id.equals(ob.optString("id")))return ob;
        }return null;
    }
    private void noteActions(String id,String type){
        JSONObject ob=findNote(id);if(ob==null)return;
        boolean task="todo".equals(type);
        String[] actions=task?new String[]{"编辑内容 / 提醒","标记"+(ob.optBoolean("done")?"未完成":"已完成"),"删除"}:
            new String[]{"编辑便签","复制内容","删除"};
        new AlertDialog.Builder(this).setTitle(ob.optString("title")).setItems(actions,(dialog,which)->{
            if(which==0)editNote(type,ob);
            if(which==1){
                if(task){
                    try{ob.put("done",!ob.optBoolean("done"));}catch(Exception ignored){}
                    persistNote(ob);showNotes(type);
                }else copy(ob.optString("body"));
            }
            if(which==2)new AlertDialog.Builder(this).setTitle("删除这条记录？")
                .setNegativeButton("取消",null).setPositiveButton("删除",(d2,w)->{eraseNote(ob);showNotes(type);}).show();
        }).show();
    }
    private void editNote(String type,JSONObject source){
        final boolean task="todo".equals(type);
        final String id=source==null?UUID.randomUUID().toString():source.optString("id");
        final boolean done=source!=null&&source.optBoolean("done");
        final long[] due={source==null?0:source.optLong("due")};
        final int[] every={source==null?0:source.optInt("every")};
        LinearLayout form=vertical();form.setPadding(dp(14),dp(7),dp(14),0);
        EditText title=input(task?"待办事项":"便签标题",source==null?"":source.optString("title"),false);
        form.addView(title);
        EditText content=input(task?"补充说明（选填）":"写下需要记录的内容",source==null?"":source.optString("body"),true);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(148));cp.topMargin=dp(12);form.addView(content,cp);
        if(task){
            TextView time=t("完成时间："+fmtDue(due[0])+"    ›",14,true,textColor());
            time.setPadding(dp(10),dp(8),dp(10),dp(8));
            LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(52));tp.topMargin=dp(8);form.addView(time,tp);
            time.setOnClickListener(v->pickTime(due,()->time.setText("完成时间："+fmtDue(due[0])+"    ›")));
            TextView repeat=t("未完成提醒："+fmtRepeat(every[0])+"    ›",14,true,textColor());
            repeat.setPadding(dp(10),dp(8),dp(10),dp(8));
            form.addView(repeat,new LinearLayout.LayoutParams(-1,dp(48)));
            repeat.setOnClickListener(v->{
                int[] minutes={0,15,30,60,180,1440};
                String[] labels={"关闭循环提醒","每15分钟","每30分钟","每1小时","每3小时","每24小时"};
                new AlertDialog.Builder(this).setTitle("未完成时重复提醒")
                    .setSingleChoiceItems(labels,indexOf(minutes,every[0]),(dg,choice)->{
                        every[0]=minutes[choice];repeat.setText("未完成提醒："+fmtRepeat(every[0])+"    ›");dg.dismiss();
                    }).setNegativeButton("取消",null).show();
            });
        }
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        new AlertDialog.Builder(this).setTitle(source==null?"新建"+(task?"待办":"便签"):"编辑"+(task?"待办":"便签"))
            .setView(scroll).setNegativeButton("取消",null)
            .setPositiveButton("保存",(dialog,which)->{
                String name=title.getText().toString().trim();
                if(name.isEmpty())name=task?"未命名待办":"未命名便签";
                JSONObject saved=newNote(id,name,content.getText().toString(),type,due[0],every[0],done);
                persistNote(saved);if(task&&due[0]>0&&!done)askNotificationPermission();
                showNotes(type);
            }).show();
    }
    private int indexOf(int[] arr,int value){for(int i=0;i<arr.length;i++)if(arr[i]==value)return i;return 0;}
    private String fmtDue(long value){return value==0?"未设置":new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.CHINA).format(new Date(value));}
    private String fmtRepeat(int value){return value==0?"关闭":(value<60?"每"+value+"分钟":value==1440?"每天":"每"+(value/60)+"小时");}
    private void pickTime(long[] value,Runnable refresh){
        Calendar start=Calendar.getInstance();
        if(value[0]>0)start.setTimeInMillis(value[0]);
        DatePickerDialog dp=new DatePickerDialog(this,(v,y,m,day)->{
            TimePickerDialog tp=new TimePickerDialog(this,(time,hour,minute)->{
                Calendar c=Calendar.getInstance();
                c.set(y,m,day,hour,minute,0);c.set(Calendar.MILLISECOND,0);
                value[0]=c.getTimeInMillis();refresh.run();
            },start.get(Calendar.HOUR_OF_DAY),start.get(Calendar.MINUTE),true);tp.show();
        },start.get(Calendar.YEAR),start.get(Calendar.MONTH),start.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }
    private void askNotificationPermission(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},712);
        }
    }

    private void showReport(boolean weekly,int offset) {
        Calendar date=Calendar.getInstance();
        date.add(weekly?Calendar.WEEK_OF_YEAR:Calendar.DATE,offset);
        String key=weekly?weekKey(date):"daily_"+dayFormat.format(date.getTime());
        page(weekly?"weekly":"daily",weekly?"工作周报":"工作日报",
            weekly?"周报从本周已保存的日报提取，可一键汇总并自由编辑。":"记录工作进展。日报和周报的提醒均可单独开关。");
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);
        TextView prev=t("‹ 上一"+(weekly?"周":"天"),14,true,textColor());
        TextView current=t(weekly?weekLabel(date):dayFormat.format(date.getTime()),15,true,textColor());current.setGravity(Gravity.CENTER);
        TextView next=t("下一"+(weekly?"周":"天")+" ›",14,true,textColor());next.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        nav.addView(prev,new LinearLayout.LayoutParams(0,dp(44),1));
        nav.addView(current,new LinearLayout.LayoutParams(0,dp(44),2));
        nav.addView(next,new LinearLayout.LayoutParams(0,dp(44),1));
        prev.setOnClickListener(v->showReport(weekly,offset-1));next.setOnClickListener(v->showReport(weekly,offset+1));
        put(nav,0);
        EditText editor=input(weekly?"本周工作、进展、问题、下周计划":"今日完成、问题、明日计划",store.getString(key,""),true);
        editor.setMinLines(13);editor.setGravity(Gravity.TOP);put(editor,14);
        put(button("保存"+(weekly?"周报":"日报"),()->{
            store.edit().putString(key,editor.getText().toString()).apply();toast("已保存到本机");
        }),13);
        if(weekly)put(button("一键汇总本周日报",()->{
            StringBuilder sb=new StringBuilder();
            Calendar start=(Calendar)date.clone();
            start.setFirstDayOfWeek(Calendar.MONDAY);start.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
            for(int i=0;i<7;i++){
                String dt=dayFormat.format(start.getTime());
                String raw=store.getString("daily_"+dt,"").trim();
                if(!raw.isEmpty())sb.append(dt).append("\n").append(raw).append("\n\n");
                start.add(Calendar.DATE,1);
            }
            if(sb.length()==0){toast("本周尚无日报可汇总");return;}
            editor.setText(sb.toString().trim());toast("已汇总，请编辑后保存");
        }),8);
        put(button("复制到剪贴板",()->copy(editor.getText().toString())),8);
        String remember=weekly?"weekly_reminder":"daily_reminder";
        boolean enabled=store.getBoolean(remember,false);
        item("◷",(weekly?"周报":"日报")+"提醒："+(enabled?"已打开":"已关闭"),
            enabled?"通知时间 "+(weekly?"每周日":"每天")+" "+String.format(Locale.CHINA,"%02d:%02d",
                store.getInt(remember+"_hour",weekly?18:17),store.getInt(remember+"_minute",0)):"需要时再启用，不强制提醒",
            ()->new AlertDialog.Builder(this).setTitle(weekly?"周报提醒":"日报提醒")
                .setItems(new String[]{"打开提醒并设置时间","关闭提醒"},(dlg,option)->{
                    if(option==1){
                        store.edit().putBoolean(remember,false).apply();
                        MoyuReminderReceiver.cancelReport(this,weekly);showReport(weekly,offset);
                    }else{
                        int[] hour={store.getInt(remember+"_hour",weekly?18:17)};
                        int[] minute={store.getInt(remember+"_minute",0)};
                        new TimePickerDialog(this,(picker,h,m)->{
                            hour[0]=h;minute[0]=m;
                            store.edit().putBoolean(remember,true)
                                .putInt(remember+"_hour",h).putInt(remember+"_minute",m).apply();
                            MoyuReminderReceiver.scheduleReport(this,weekly);
                            askNotificationPermission();showReport(weekly,offset);
                        },hour[0],minute[0],true).show();
                    }
                }).show());
    }
    private String weekKey(Calendar date) {
        Calendar c=(Calendar)date.clone();c.setFirstDayOfWeek(Calendar.MONDAY);c.setMinimalDaysInFirstWeek(4);
        return "weekly_"+c.getWeekYear()+"_"+c.get(Calendar.WEEK_OF_YEAR);
    }
    private String weekLabel(Calendar date) {
        Calendar c=(Calendar)date.clone();c.setFirstDayOfWeek(Calendar.MONDAY);c.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
        String a=dayFormat.format(c.getTime());c.add(Calendar.DATE,6);
        return a+" 至 "+dayFormat.format(c.getTime());
    }
    private void copy(String text){
        ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("墨鱼浏览器",text));
        toast("已复制");
    }
    private void open(String url){
        Intent i=new Intent(this,MainActivity.class);
        i.setAction(Intent.ACTION_VIEW);i.setData(Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }
    private void translate() {
        page("translate","免费翻译","输入要翻译的文字，点击按钮后在浏览器中打开翻译结果。需要网络连接。");
        EditText text=input("在这里输入中文、英文或其他语言", "",true);
        text.setMinLines(7);put(text,0);
        put(button("自动识别 → 中文",()->translateText(text.getText().toString(),"zh-CN")),16);
        put(button("自动识别 → 英文",()->translateText(text.getText().toString(),"en")),10);
    }
    private void translateText(String raw,String language) {
        if(raw.trim().isEmpty()){toast("请输入文字");return;}
        try{open("https://translate.google.com/?sl=auto&tl="+language+
            "&text="+URLEncoder.encode(raw.trim(),"UTF-8")+"&op=translate");}
        catch(Exception e){toast("暂时无法打开翻译");}
    }
    private void news(){
        page("news","AI 资讯","访问官方发布源获取最新信息，不展示缓存或未经核实的伪实时新闻。");
        item("O","OpenAI News","官方模型与产品公告",()->open("https://openai.com/news/"));
        item("A","Anthropic News","Claude 与研究动态",()->open("https://www.anthropic.com/news"));
        item("G","Google DeepMind","研究、模型与科学进展",()->open("https://deepmind.google/discover/blog/"));
        item("M","Microsoft Research","研究与技术资讯",()->open("https://www.microsoft.com/en-us/research/blog/"));
    }
    private void directory() {
        page("directory","分类网址导航","分类收纳常用网站。点击即可在墨鱼浏览器中打开；首页始终保持精简。");
        String[][] entries={
            {"AI 工具","ChatGPT","https://chatgpt.com","Claude","https://claude.ai","Gemini","https://gemini.google.com","DeepSeek","https://chat.deepseek.com","Perplexity","https://www.perplexity.ai"},
            {"设计创意","Figma","https://www.figma.com","Canva","https://www.canva.com","Behance","https://www.behance.net","Dribbble","https://dribbble.com","Iconify","https://icon-sets.iconify.design"},
            {"编程开发","GitHub","https://github.com","Stack Overflow","https://stackoverflow.com","MDN","https://developer.mozilla.org","Vercel","https://vercel.com","Hugging Face","https://huggingface.co"},
            {"电商运营","淘宝","https://www.taobao.com","京东","https://www.jd.com","1688","https://www.1688.com","抖音","https://www.douyin.com","阿里巴巴国际","https://www.alibaba.com"},
            {"办公协作","Notion","https://www.notion.so","飞书","https://www.feishu.cn","腾讯文档","https://docs.qq.com","Google Drive","https://drive.google.com","石墨文档","https://shimo.im"},
            {"视频媒体","哔哩哔哩","https://www.bilibili.com","YouTube","https://www.youtube.com","剪映","https://www.capcut.cn","Vimeo","https://vimeo.com","Unsplash","https://unsplash.com"},
            {"学习研究","Wikipedia","https://www.wikipedia.org","Coursera","https://www.coursera.org","arXiv","https://arxiv.org","Google Scholar","https://scholar.google.com","中国大学 MOOC","https://www.icourse163.org"},
            {"科技资讯","36氪","https://36kr.com","少数派","https://sspai.com","TechCrunch","https://techcrunch.com","The Verge","https://www.theverge.com","Hacker News","https://news.ycombinator.com"}
        };
        for(String[] group:entries){
            section(group[0]);
            for(int i=1;i+1<group.length;i+=2){
                final String name=group[i],url=group[i+1];
                item("↗",name,Uri.parse(url).getHost(),()->open(url));
            }
        }
    }
    private void calculator() {
        page("calculator","计算器","支持加减乘除与括号，小数运算在本机完成。");
        EditText expression=input("例如 (120+80)*0.85", "",false);
        expression.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        put(expression,0);
        TextView result=t("＝",27,true,textColor());result.setGravity(Gravity.CENTER);
        put(result,13);
        put(button("计算",()->{
            try{
                double value=new CalculatorParser(expression.getText().toString()).parse();
                if(!Double.isFinite(value))throw new ArithmeticException();
                result.setText("＝ "+java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString());
            }catch(Exception e){result.setText("表达式无效");}
        }),12);
    }
    private static final class CalculatorParser {
        private final String s;private int at;
        CalculatorParser(String s){this.s=s.replace('×','*').replace('÷','/').replace(" ","");}
        double parse(){
            double n=expr();
            if(at!=s.length())throw new IllegalArgumentException();
            return n;
        }
        double expr(){
            double a=term();while(at<s.length()){
                char c=s.charAt(at);
                if(c!='+'&&c!='-')break;
                at++;double b=term();a=c=='+'?a+b:a-b;
            }return a;
        }
        double term(){
            double a=atom();while(at<s.length()){
                char c=s.charAt(at);
                if(c!='*'&&c!='/')break;
                at++;double b=atom();
                if(c=='/'&&b==0)throw new ArithmeticException();
                a=c=='*'?a*b:a/b;
            }return a;
        }
        double atom(){
            if(at>=s.length())throw new IllegalArgumentException();
            char c=s.charAt(at);
            if(c=='+'||c=='-'){at++;double n=atom();return c=='-'?-n:n;}
            if(c=='('){at++;double n=expr();if(at>=s.length()||s.charAt(at++)!=')')throw new IllegalArgumentException();return n;}
            int start=at;while(at<s.length()&&("0123456789.".indexOf(s.charAt(at))>=0))at++;
            if(start==at)throw new IllegalArgumentException();
            return Double.parseDouble(s.substring(start,at));
        }
    }
    private void autofillSettings(){
        new AlertDialog.Builder(this).setTitle("使用系统密码自动填充")
            .setMessage("墨鱼浏览器不会自行以明文保存网页密码。可在 Android 系统设置中选择可信的自动填充服务，支持的网站由系统提供填充。")
            .setNegativeButton("取消",null).setPositiveButton("打开设置",(dg,w)->{
                try{startActivity(new Intent("android.settings.AUTOFILL_SETTINGS"));}
                catch(Exception ex){startActivity(new Intent(Settings.ACTION_SETTINGS));}
            }).show();
    }
    @Override public void onBackPressed(){
        if("tools".equals(page)){finish();return;}
        showHome();
    }
}
