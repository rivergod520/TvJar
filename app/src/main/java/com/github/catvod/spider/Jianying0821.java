package com.github.catvod.spider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.AES;
import com.github.catvod.utils.CBC;
import com.github.catvod.utils.Hex;
import com.github.catvod.utils.okhttp.OkHttpUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Jianying extends Spider {

    private String token;
    private JSONObject loginInfo;

    @Override
    public void init(Context context) {
        super.init(context);
    }

    @Override
    public void init(Context context, String extend) {
        try {
            if(extend.startsWith("http")){
                token = OkHttpUtil.string(extend,headers());
            }else {
                byte[] decode = Base64.decode(extend, Base64.DEFAULT);
                String info = new String(decode);
                loginInfo = new JSONObject(info);
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
    }

    public HashMap<String, String> headers() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
        if (!TextUtils.isEmpty(token)) {
            hashMap.put("Authorization", token);
        }
        return hashMap;
    }

    @Override
    public String homeContent(boolean filter) {
        JSONObject result = new JSONObject();
        try {
            Map<String, String> classes = new HashMap<>();
            classes.put("电影", "电影");
            classes.put("美剧", "美剧");
            classes.put("韩剧", "韩剧");
            classes.put("日剧", "日剧");
            classes.put("动漫", "动漫");
            classes.put("纪录", "纪录");
            classes.put("动作", "动作");
            classes.put("科幻", "科幻");
            classes.put("爱情", "爱情");
            classes.put("动画", "动画");
            classes.put("喜剧", "喜剧");
            classes.put("犯罪", "犯罪");

            JSONArray cls = new JSONArray();
            for (Map.Entry<String, String> entry : classes.entrySet()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type_id", entry.getKey());
                jsonObject.put("type_name", entry.getValue());
                cls.put(jsonObject);
            }
            result.put("class", cls);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return result.toString();
    }

    @Override
    public String homeVideoContent() {
        JSONObject result = new JSONObject();
        try {
            checkAccount();
            String json = OkHttpUtil.postJson("https://admin.syrme.top/v1/api/video/index", null, headers());
            JSONArray data = new JSONObject(json).optJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item.optString("title", "").equals("最新")) {
                    JSONArray videoList = item.optJSONArray("video_list");
                    JSONArray vods = new JSONArray();
                    for (int j = 0; j < videoList.length(); j++) {
                        JSONObject vodItem = videoList.optJSONObject(j);
                        JSONObject vod = new JSONObject();
                        vod.put("vod_id", vodItem.optInt("ID"));
                        vod.put("vod_name", vodItem.optString("title"));
                        vod.put("vod_pic", vodItem.optString("image"));
                        vod.put("vod_remarks", vodItem.optString("score"));
                        vods.put(vod);
                    }
                    result.put("list", vods);
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return result.toString();
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        JSONObject result = new JSONObject();
        try {
            checkAccount();
            String url = "https://admin.syrme.top/v1/api/video/search?q=" + URLEncoder.encode(tid) + "&page=" + pg + "&size=24";
            String json = OkHttpUtil.postJson(url, null, headers());
            JSONObject resp = new JSONObject(json);
            JSONArray data = resp.optJSONArray("data");
            JSONArray vods = new JSONArray();
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                JSONObject vod = new JSONObject();
                vod.put("vod_id", item.optInt("ID"));
                vod.put("vod_name", item.optString("title"));
                vod.put("vod_pic", item.optString("image"));
                vod.put("vod_remarks", item.optString("score"));
                vods.put(vod);
            }
            int parseInt = Integer.parseInt(pg);
            result.put("page", parseInt);
            if (vods.length() == 24) {
                parseInt++;
            }
            result.put("pagecount", parseInt);
            result.put("limit", 24);
            result.put("total", resp.optInt("num"));
            result.put("list", vods);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return result.toString();
    }

    @Override
    public String detailContent(List<String> ids) {
        JSONObject result = new JSONObject();
        try {
            checkAccount();
            String url = "https://admin.syrme.top/v1/api/video/id?id=" + ids.get(0);
            String json = OkHttpUtil.postJson(url, null, headers());
            JSONObject data = new JSONObject(json).optJSONObject("data");
            JSONObject vod = new JSONObject();
            vod.put("vod_id", data.optString("ID"));
            vod.put("vod_name", data.getString("title"));
            vod.put("vod_pic", data.getString("image"));
            vod.put("type_name", data.optString("video_tags"));
            vod.put("vod_year", data.optString("year"));
            vod.put("vod_area", data.optString("vod_area"));
            vod.put("vod_remarks", data.optString("score"));
            vod.put("vod_director", data.optString("director"));
            vod.put("vod_actor", data.optString("authors"));
            vod.put("vod_content", data.optString("content").trim());
            vod.put("vod_play_from", "简影");
            String urlContent = data.optString("url_content").replaceAll("\\n", "#");
            vod.put("vod_play_url", urlContent);

            JSONArray vods = new JSONArray();
            vods.put(vod);
            result.put("list", vods);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return result.toString();
    }

    @Override
    public String searchContent(String key, boolean quick) {
        JSONObject result = new JSONObject();
        try {
            checkAccount();
            String url = "https://admin.syrme.top/v1/api/video/search?q=" + URLEncoder.encode(key) + "&page=1&size=24";
            String json = OkHttpUtil.postJson(url, null, headers());
            JSONObject resp = new JSONObject(json);
            JSONArray data = resp.optJSONArray("data");
            JSONArray vods = new JSONArray();
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                JSONObject vod = new JSONObject();
                vod.put("vod_id", item.optInt("ID"));
                vod.put("vod_name", item.optString("title"));
                vod.put("vod_pic", item.optString("image"));
                vod.put("vod_remarks", item.optString("score"));
                vods.put(vod);
            }
            result.put("list", vods);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return result.toString();
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressLint("SimpleDateFormat")
    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        JSONObject result = new JSONObject();
        try {
            checkAccount();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy丨MM-dd HH:mm");
            String date = sdf.format(new Date());
            String key = "ba0-2401-45eb-cs";
            String aesString = Hex.toHex(AES.encrypt(date.getBytes(), key.getBytes(), key.getBytes(), AES.AES_CBC_PKCS7Padding));
            String sid = "";
            int i = 0;
            while (i < aesString.length()) {
                int j = i + 8;
                sid = sid + aesString.substring(i, j) + "-";
                i = j;
            }
            sid = sid.substring(0, sid.length() - 1);
            result.put("parse", 0);
            result.put("url", id + "?sid=" + sid);
            result.put("playUrl", "");
            result.put("header", "");
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return result.toString();
    }

    @Override
    public boolean isVideoFormat(String url) {
        return super.isVideoFormat(url);
    }

    @Override
    public boolean manualVideoCheck() {
        return super.manualVideoCheck();
    }

    private void checkAccount() {
        if (TextUtils.isEmpty(token)) {
            // 登陆
            login();
        } else {
            try {
                String[] split = token.split("\\.");
                byte[] decode = Base64.decode(split[1], Base64.DEFAULT);
                String s = new String(decode);
                JSONObject info = new JSONObject(s);
                long exp = info.optLong("exp");
                if ((exp * 1000) - new Date().getTime() <= 10 * 60 * 1000) {
                    // 重新登陆
                    login();
                }
            } catch (Exception e) {
                SpiderDebug.log(e);
            }
        }
    }

    private void login() {
        try {
            if (!TextUtils.isEmpty(loginInfo.optString("token"))) {
                token = loginInfo.optString("token");
            } else if (!TextUtils.isEmpty(loginInfo.optString("username"))) {
                JSONObject body = new JSONObject();
                body.put("user_email", loginInfo.optString("username"));
                body.put("user_pwd", loginInfo.optString("password"));
                String json = OkHttpUtil.postJson("https://admin.syrme.top/v1/api/user/login", body.toString(), headers());
                JSONObject resp = new JSONObject(json);
                token = resp.optString("data");
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
    }
}
