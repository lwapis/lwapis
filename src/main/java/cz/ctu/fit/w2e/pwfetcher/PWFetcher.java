/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.ctu.fit.w2e.pwfetcher;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import cz.ctu.fit.w2e.mongodb.MongoDBClient;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.bson.types.ObjectId;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 *
 * @author Milan Dojchinovski
 */
public class PWFetcher {

    public static PWFetcher          client         = null;

    private       BasicDBObject      tempObj        = null;
    private       ArrayList          tempList       = new ArrayList();
    private       boolean            existsComments = false;
    public        List<List<String>> fetchList      = new ArrayList<List<String>>();
    
    Element api_desc = null;

    private int numUsers   = 0;
    private int numAPIs    = 0;
    private int numMashups = 0;
    
    public static PWFetcher getInstance(){
        if(client == null){
            client = new PWFetcher();
        }
        return client;
    }
    
    public List<List<String>> getFetchList(Object snapshotId){
//        fetchAPIList(snapshotId);
        fetchUsersList();
//        fetchMashupsList();
        
        DBObject found = MongoDBClient.getInstance().getCollection("snapshots").findOne(snapshotId);
        found.put("num_users", numUsers);
        found.put("num_apis", numAPIs);
        found.put("num_mashups", numMashups);
        MongoDBClient.getInstance().getCollection("snapshots").save(found);
        
        return fetchList;
    }
    
    public void fetchUsersList(){
        
        System.out.println("Fetching users list...");

        int counter = 1;        
        
        while(fetchMembersPage(counter)){
            counter++;
            //fetchMembersPage(counter);
        }        
    }
    
    public boolean fetchMembersPage(int num){
        System.out.println("users page " + num);
        try {
            
            Thread.sleep(20000);
            Connection conn = Jsoup.connect("http://www.programmableweb.com/members/directory/"+num+"?filter=mashups&pagesize=100")
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Encoding", "gzip,deflate,sdch")
                .header("Accept-Language", "en-US,en;q=0.8")
                .header("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3")
                .timeout(35000);
            
            Document doc = conn.get();
            
            Elements members_list = doc.select("table.listTable").select("tr");
            //System.out.println("SIZE: "+ members_list.size());
            
            if(members_list.size()>2){
                for(Element el : members_list.subList(1, members_list.size())){
                    numUsers++;
                    List<String> row = new ArrayList<String>();

                    // User title
                    row.add("");
                    // User URL
                    row.add("http://www.programmableweb.com" + el.select("td").first().select("a").attr("href"));
                    // updated
                    row.add("");
                    // type
                    row.add("user");

                    fetchList.add(row);
                    //System.out.println("Member url: " + el.select("td").first().select("a").attr("href"));
                }
                if(members_list.size()==101){
                    return true;
                }else{
                    return false;
                }
            } else {
                return false;
            }
            
        } catch (IOException ex) {
            Logger.getLogger(PWFetcher.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (InterruptedException ex) {
            Logger.getLogger(PWFetcher.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }        
    }
    
    public void fetchAPIList(Object snapshotId){
        System.out.println("Fetching APIs list...");
        try {
            Connection conn = Jsoup.connect("http://www.programmableweb.com/")
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Encoding", "gzip,deflate,sdch")
                        .header("Accept-Language", "en-US,en;q=0.8")
                        .header("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3")
                        .timeout(15000);
            Document doc = conn.get();
            Element numAPIs_el = doc.select("div.span-3.aCenter")
                    .select("a.hLink.padTB5.mL15")
                    .select("span").first();
            int numAPIs2 = Integer.parseInt(numAPIs_el.text());
            //System.out.println(numAPIs2);
            int pages = (int)numAPIs2 / 3000;
            if(((int)numAPIs2 % 3000) > 0)
                pages++;
            
            //System.out.println("Number of APIs pages: "+(int)pages);
            for(int i=1; i <=(int)pages; i++){
                fetchAPIsPage(i, snapshotId);
            }
            
        } catch (IOException ex) {
            Logger.getLogger(PWFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void fetchMashupsList(){
        
        System.out.println("Fetching Mashups list...");

        try {
            Connection conn = Jsoup.connect("http://www.programmableweb.com/")
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Encoding", "gzip,deflate,sdch")
                        .header("Accept-Language", "en-US,en;q=0.8")
                        .header("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3")
                        .timeout(15000);
            Document doc = conn.get();
            Element numMashups_el = doc.select("div.span-3.aCenter.last")
                    .select("a.hLink.padTB5")
                    .select("span").first();
            int numMashups2 = Integer.parseInt(numMashups_el.text());
            //System.out.println(numMashups2);
            int pages = (int)numMashups2 / 20;
            if(((int)numMashups2 % 20) > 0)
                pages++;
                        
            //System.out.println("Number of Mashup pages: "+(int)pages);
            //for(int i=1; i <=(int)1; i++){
            for(int i=1; i <=(int)pages; i++){
                System.out.println("fetching mashup page " + i);
                fetchMashupsPage(i);
            }
            
        } catch (IOException ex) {
            Logger.getLogger(PWFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void fetchMashupsPage(int num) {
        try {
            Connection conn = Jsoup.connect("http://www.programmableweb.com/mashups/directory/"+num)
                                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                .header("Accept-Encoding", "gzip,deflate,sdch")
                                .header("Accept-Language", "en-US,en;q=0.8")
                                .header("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3")
                                .timeout(30000);
                Document doc = conn.get();
                Elements apis_list = doc.select("table.listTable")
                        .select("tr");
                
                for(Element el : apis_list.subList(1, apis_list.size())){
                                        
                    numMashups++;
                    //System.out.println("Mashup name:" + el.select("td").get(1).select("a").first().text());
                    //System.out.println("Mashup url:" + el.select("a").first().attr("href"));
                    //System.out.println("API updated:" + el.select("td").last().text());
                    
                    List<String> row = new ArrayList<String>();
                    // Mashup title
                    row.add(el.select("td").get(1).select("a").first().text());
                    // Mashup URL
                    row.add("http://www.programmableweb.com" + el.select("a").first().attr("href"));
                    // Mashup updated
                    row.add("");                
                    // type
                    row.add("mashup");

                    fetchList.add(row);
                }
                
        } catch (IOException ex) {
            Logger.getLogger(PWFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void fetchAPIsPage(int num, Object snapshotId){
        try {
            Connection conn = Jsoup.connect("http://www.programmableweb.com/apis/directory/"+num)
                            .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .header("Accept-Encoding", "gzip,deflate,sdch")
                            .header("Accept-Language", "en-US,en;q=0.8")
                            .header("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3")
                            .timeout(30000);
            Document doc = conn.get();
            Elements apis_list = doc.select("table.listTable.mB15")
                    .select("tr");
            
            for(Element el : apis_list.subList(1, apis_list.size())){
                numAPIs++;
//                tempObj = new BasicDBObject();
//                tempObj.append("snapshot_id", snapshotId);
                
                Calendar c = new GregorianCalendar();
                String tempDate = el.select("td").last().text();
                
                /*
                System.out.println(el.select("td").first().text());
                System.out.println("year:" + Integer.parseInt(tempDate.substring(0,4)));
                System.out.println("month:" + Integer.parseInt(tempDate.substring(5,7)));
                System.out.println("day:" + Integer.parseInt(tempDate.substring(8,10)));
                */
                
                c.set(Integer.parseInt(tempDate.substring(0,4)), Integer.parseInt(tempDate.substring(5,7))-1, Integer.parseInt(tempDate.substring(8,10)));
//                System.out.println(el.select("td").first().text());
//                System.out.println(c.getTime());
//                tempObj.append("date_updated", c.getTime());

//                MongoDBClient.getInstance().getCollection("apis").insert(tempObj);
                List<String> row = new ArrayList<String>();
                // API title
                row.add(el.select("td").first().text());
                // API URL
                row.add("http://www.programmableweb.com" + el.select("td").first().select("a").attr("href"));
                // API updated
                row.add(tempDate);                
                // type
                row.add("api");
                
                fetchList.add(row);
                /*
                System.out.println("API name:" + el.select("td").first().text());
                System.out.println("API url:" + el.select("td").first().select("a").attr("href"));
                System.out.println("API updated:" + el.select("td").last().text());
                */
            }
            
            
        } catch (IOException ex) {
            Logger.getLogger(PWFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void fetchMashup(String url, Object snapshotId){
        
        tempObj = new BasicDBObject();
        tempObj.append("snapshot_id", snapshotId);
        
        System.out.println("Fetching mashup: " + url);
        try {
            Connection conn = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Encoding", "gzip,deflate,sdch")
                        .header("Accept-Language", "en-US,en;q=0.8")
                        .header("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3")
                        .timeout(15000);

            Document doc = conn.get();
            //Mashup title
            Element mashup_title = doc.select("h1").first();
            //System.out.println(mashup_title.text());
            tempObj.append("title", mashup_title.text());

            tempObj.append("pw_url", url);
            
            //Mashup description
            Element mashup_desc = doc.select("div.span-16.mT10").select("div.span-8.last").select("p").first();
            //System.out.println(mashup_desc.text());
            tempObj.append("description", mashup_desc.text());
            
            // Mashup rating
            Element mashup_rating_el = doc.select("ul.star-rating").select("li.current-rating").first();
            double rating = Double.parseDouble(mashup_rating_el.attr("style").substring(6, mashup_rating_el.attr("style").length()-3))/15;
            //System.out.println("Mashup rating: " + rating);
            tempObj.append("rating", rating);

            //Mashup tags
            Elements mashup_tags_list = doc.select("dl.inline.dt40.mB15").select("dd");
            tempList = new ArrayList();
            for(Element el: mashup_tags_list){
                //System.out.println("tag: "+el.select("a").first().text());
                tempList.add(el.select("a").first().text());
            }
            tempObj.append("tags", tempList);
            
            //Used APIs by mashup
            Elements mashup_apis_list = doc.select("div.span-16.mT10").select("div.span-8.last").select("dl.inline.dt40").first().select("dd");
            tempList = new ArrayList();
            for(Element el: mashup_apis_list){
                //System.out.println("used api: " + el.text());
                //System.out.println("used api url: " + "http://www.programmableweb.com" + el.select("a").attr("href"));
                tempList.add(new BasicDBObject().append("api_title", el.text()).append("api_url", "http://www.programmableweb.com" + el.select("a").attr("href")));
            }
            tempObj.append("used_apis", tempList);
            
            //Date added
            Elements mashup_added_list = doc.select("div.span-16.mT10").select("div.span-8.last").select("dl.inline.dt40");
            for(Element el: mashup_added_list){
                if(el.select("dt").text().equals("Added")){
                    //System.out.println("mashup was added on: " + el.select("dd").text());
                    tempObj.append("date_added", el.select("dd").text());
                }
            }
            
            //Mashup author url
            Elements mashup_author_list = doc.select("div.span-16.mT10").select("div.span-8.last").select("dl.inline.dt40");
            
            for(Element el: mashup_author_list){
                
                if(el.select("dt").text().equals("Who")){
                    
                    //System.out.println("mashup author username: " + el.select("dd").text());
                    
                    if (el.select("dd").text().endsWith("[Profile]")) {                        
                        tempObj.append("author", el.select("dd").text().substring(0, el.select("dd").text().length()-10));                                            
                    } else {
                        tempObj.append("author", el.select("dd").text());                    
                    }
                        
                    //System.out.println("author url: " + el.select("a").attr("href"));
                    
                    if(el.select("a").attr("href").startsWith("/profile/")){
                        
                        tempObj.append("author_url", "http://www.programmableweb.com" + el.select("a").attr("href"));
                        
                    } else {

                        tempObj.append("author_url", el.select("a").attr("href"));
                        
                        BasicDBObject queryObj = new BasicDBObject();
//                        queryObj.put("snapshot_id", snapshotId);
                        queryObj.put("username", el.select("a").text());
                        
                        DBCursor cursor = MongoDBClient.getInstance().getCollection("users").find(queryObj);
                        
                        if(cursor.size()==0){
                            MongoDBClient.getInstance().getCollection("users").update(new BasicDBObject().append("username",el.select("a").text()),queryObj, true, false);
//                            MongoDBClient.getInstance().getCollection("snapshots")
//                                    .update(new BasicDBObject().append("_id",snapshotId), new BasicDBObject().append("$inc", new BasicDBObject().append("num_users", 1)));
                        }
                    }
                }
            }
            
            //Mashup url
            Elements mashup_url_list = doc.select("div.span-16.mT10").select("div.span-8.last").select("dl.inline.dt40");
            for(Element el: mashup_author_list){
                if(el.select("dt").text().equals("URL")){
                    //System.out.println("mashup url: " + el.select("dd").select("a").attr("href"));
                    tempObj.append("url", el.select("dd").select("a").attr("href"));
                }
            }
            
            //Mashup availability
            Elements mashup_availability_list = doc.select("div.box5.bgRed.mB15.aCenter");
            tempObj.append("available", 1);
            for(Element el: mashup_availability_list){
                if(el.text().indexOf("no longer available") != -1){
                    //System.out.println("mashup is not available");
                    tempObj.append("available", 0);
                }
            }
            
//            // Mashup comments
//            Elements navBar = doc.select(".inPnav li");
//            for(Element el: navBar){
//                if(el.select("a").first().text().startsWith("Comments")){
//                    if(el.select("a").first().text().equals("Comments")){
//                        //System.out.println("No comments");
//                        tempObj.append("exists_comments", 0);
//                    }else{
//                        //System.out.println("Comments exist");
//                        tempObj.append("exists_comments", 1);
//                        existsComments = true;
//                    }
//                }
//            }
            
            MongoDBClient.getInstance().getCollection("mashups").update(new BasicDBObject().append("title",mashup_title.text()), tempObj, true, false);
            
//            if(existsComments){
//                
//                List<String> row = new ArrayList<String>();
//                
//                row.add("");
//                // Comments URL
//                row.add(url + "/comments");
//                // Mashup id
//                row.add(tempObj.get("_id").toString());                
//                // type
//                row.add("comments");
//
//                fetchList.add(row);
//                // extractComments(url + "/comments", snapshotId, tempObj.get("_id")); // extract comments, if there exist
//            }
//            existsComments = false;
            
        } catch (IOException ex) {
            Logger.getLogger(PWFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    
    }
    
    public void extractComments(String url, Object snapshotId, Object targetId){
        
        System.out.println("Fetching comments from URL: " + url);
        
        try {
            Connection conn = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Encoding", "gzip,deflate,sdch")
                        .header("Accept-Language", "en-US,en;q=0.8")
                        .header("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3")
                        .timeout(15000);

            Document doc = conn.get();
            
            Elements commentsList = doc.select(".listText").first().select("tr");
            //System.out.println(commentsList.size());
            
            for(Element el: commentsList.subList(1, commentsList.size())){
                tempObj = new BasicDBObject();
                tempObj.append("snapshot_id", snapshotId);
                tempObj.append("target_obj_id", targetId);
                //System.out.println("date: "+el.select("td").first().text());
                String month = el.select("td").first().text().substring(3, 6);
                int monthString;
                switch (month) {
                   case "Jan":  monthString = 0;
                            break;
                   case "Feb":  monthString = 1;
                            break;
                   case "Mar":  monthString = 2;
                            break;
                   case "Apr":  monthString = 3;
                            break;
                   case "May":  monthString = 4;
                            break;
                   case "Jun":  monthString = 5;
                            break;
                   case "Jul":  monthString = 6;
                            break;
                   case "Aug":  monthString = 7;
                            break;
                   case "Sep":  monthString = 8;
                            break;
                   case "Oct":  monthString = 9;
                            break;
                   case "Nov":  monthString = 10;
                            break;
                   case "Dec":  monthString = 11;
                            break;
                   default: monthString = 0;
                            break;
                }
                
                Calendar c = new GregorianCalendar();
                c.set(Integer.parseInt(el.select("td").first().text().substring(7,11)), monthString, Integer.parseInt(el.select("td").first().text().substring(0, 2)), 0, 0, 0);
                tempObj.append("date",c.getTime());
                
                Element el2 = el.select("td").get(1).select("h3").first();
                String author = "";
                
                for (Node child : el2.childNodes()) {
                    if (child instanceof TextNode) {
                        author = ((TextNode) child).text().trim();                 
                    }
                    break;
                }
                String author_url="";
                boolean insertAuthorInDB = true;
                
                // Author with an URL
                if(author.equals("")){
                    //System.out.println("tobe" + el.select("td").get(1).select("h3 a").first().text());
                    author = el.select("td").get(1).select("h3 a").first().text();
                    author_url = el.select("td").get(1).select("h3 a").first().attr("href");
                    //System.out.println("author url: " + author_url);
                    
                    if(el.select("td").get(1).select("h3 a").first().attr("href").startsWith("/profile/")){
                        author_url = "http://www.programmableweb.com" + el.select("td").get(1).select("h3 a").first().attr("href");
                        tempObj.append("author_url", "http://www.programmableweb.com" + el.select("td").get(1).select("h3 a").first().attr("href"));                    
                        insertAuthorInDB = false;
                    }else{
                        tempObj.append("author_url", el.select("td").get(1).select("h3 a").first().attr("href")); 
                        author_url = el.select("td").get(1).select("h3 a").first().attr("href");
                        insertAuthorInDB = true;
                    }
                }
                
                //System.out.println("author: " + author);
                tempObj.append("author", author);
                
                if(insertAuthorInDB){
                    BasicDBObject queryObj = new BasicDBObject();
                    queryObj.put("snapshot_id", snapshotId);
                    queryObj.put("username", author);
                    queryObj.put("author_url", author_url);

                    DBCursor cursor = MongoDBClient.getInstance().getCollection("users").find(queryObj);

                    if(cursor.size()==0){
                        MongoDBClient.getInstance().getCollection("users").save(queryObj);
                        MongoDBClient.getInstance().getCollection("snapshots")
                            .update(new BasicDBObject().append("_id",snapshotId), new BasicDBObject().append("$inc", new BasicDBObject().append("num_users", 1)));
                    }
                } else {
                    // User is added in the queue.
                    List<String> row = new ArrayList<String>();
                
                    // User title
                    row.add("");
                    // User URL
                    row.add(author_url);
                    // updated
                    row.add("");
                    // type
                    row.add("user");

                    fetchList.add(row);
                    MongoDBClient.getInstance().getCollection("snapshots")
                            .update(new BasicDBObject().append("_id",snapshotId), new BasicDBObject().append("$inc", new BasicDBObject().append("num_users", 1)));

                }
                
                // System.out.println("comment: " + el.select("td").get(1).childNodes().get(el.select("td").get(1).childNodes().size()-1));
                String comment = "";
                //System.out.println("comment: " + el.select("td").get(1).childNodes().get(el.select("td").get(1).childNodes().size()-1));
                Element td = el.select("td").get(1);
                //System.out.println(td.childNodes().size());
                for (Node child : td.childNodes()) {
                    if (child instanceof TextNode) {
                        comment += ((TextNode) child).text();                 
                        //System.out.println("comment: "+((TextNode) child).text());
                    }
                }
                
                //System.out.println("comment: " + comment);
                tempObj.append("text", comment);
                
                MongoDBClient.getInstance().getCollection("comments").insert(tempObj);
            }
                
        } catch (IOException ex) {
            Logger.getLogger(PWFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void fetchAPI(String url, Object snapshotId, String tempDate){
        tempObj = new BasicDBObject();
        tempObj.append("snapshot_id", snapshotId);
        
        System.out.println("Fetching API: " + url);
        
        try {
            
            Connection conn = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip,deflate,sdch")
                    .header("Accept-Language", "en-US,en;q=0.8")
                    .header("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3")
                    .timeout(30000);

            Document doc = conn.get();
            
            //API title
            Element api_title = doc.select("h1").first();
            //System.out.println(api_title.text());
            tempObj.append("title", api_title.text());
            
            tempObj.append("pw_url", url);
            
            //API description
            String api_desc_text = "";
            try{
                api_desc = doc.select("div.span-16.mT5").select("div.span-3p5.padT10").first().siblingElements().first();            
                api_desc_text = api_desc.text();
            }catch(NullPointerException ex){
                api_desc = doc.select("div.span-11").select("p").first();            
                for (Node child : api_desc.childNodes()) {
                    if (child instanceof TextNode) {
                        //System.out.println(((TextNode) child).text().trim());
                        api_desc_text += ((TextNode) child).text().trim();
                    }
                }                        
            }
            
            //System.out.println(api_desc_text);
            
            tempObj.append("description", api_desc_text);
            
            // API rating
            Element api_rating_el = doc.select("ul.star-rating").select("li.current-rating").first();
            double rating = Double.parseDouble(api_rating_el.attr("style").substring(6, api_rating_el.attr("style").length()-3))/15;
            //System.out.println("API rating: " + rating);            
            tempObj.append("rating", rating);
            
            //API summary
            Element api_summary;
            String api_summary_text="";
            try{
                api_summary = doc.select("dl.inline.dt90").select("dt").first().siblingElements().first();
                api_summary_text = api_summary.text();
                    //System.out.println(api_summary.text());
            }catch(NullPointerException ex){
                api_summary_text = doc.select("div.span-8.last.padT15").select("dd").first().text();                
            }
            //System.out.println("Summary: "+api_summary_text);
            tempObj.append("summary", api_summary_text);
            
            //API category
            Elements api_category_list = doc.select("dl.inline.dt90").select("dt");
            for(Element el: api_category_list){
                if(el.text().equals("Category")){
                    String api_category = el.siblingElements().first().text();
                    //System.out.println("Category: " + api_category);
                    tempObj.append("category", api_category);
                }
            }
            
            //API tags            
            Elements api_tags_list = doc.select("dl.inline.dt90").select("dt");
            for(Element el: api_tags_list){
               if(el.text().equals("Tags")){
                    tempList = new ArrayList();
                    Elements tags = el.parent().select("a");
                    for(Element tag: tags){
                        //System.out.println("tag: " + tag.text());
                        tempList.add(tag.text());
                    }
                    tempObj.append("tags", tempList);
                }             
            }
            
            //API Protocols            
            Elements api_protocols_list = doc.select("dl.inline.dt90").select("dt");
            for(Element el: api_protocols_list){
                if(el.text().equals("Protocols")){
                    Elements protocols = el.parent().select("a");
                    tempList = new ArrayList();
                    for(Element protocol: protocols){
                        if(!protocol.equals("")){
                            //System.out.println("protocol: " + protocol.text());
                            tempList.add(protocol.text());
                        }
                    }
                    tempObj.append("protocols", tempList);
                }             
            }
            
            //API data formats            
            Elements api_data_formats_list = doc.select("dl.inline.dt90").select("dt");
            for(Element el: api_data_formats_list){
                if(el.text().equals("Data Formats")){
                    tempList = new ArrayList();
                    Elements formats = el.parent().select("a");
                    for(Element format: formats){
                        if(!format.equals("")){
                            //System.out.println("format: " + format.text());
                            tempList.add(format.text());
                        }
                    }
                    tempObj.append("formats", tempList);
                }             
            }
            
            // API home URL            
            Elements api_home_url_list = doc.select("dl.inline.dt90").select("dt");
            for(Element el: api_home_url_list){
                if(el.text().equals("API home")){
                    String api_home_url = el.parent().select("a").attr("href");
                    //System.out.println("API home url: " + api_home_url);                
                    tempObj.append("home_url", api_home_url);
                }
            }
            
            // API availability
            Elements api_availability_list = doc.select("div.box5.bgRed.aCenter.mTB5");
            tempObj.append("available", 1);
            for(Element el: api_availability_list){
                if(el.text().indexOf("no longer available") != -1){
                    //System.out.println("api is NOT AVAILABLE");
                    tempObj.append("available", 0);
                }
            }
            
            // API comments
//            Elements navBar = doc.select(".inPnav li");
//            for(Element el: navBar){
//                if(el.select("a").first().text().startsWith("Comments")){
//                    if(el.select("a").first().text().equals("Comments")){
//                        //System.out.println("No comments");
//                        tempObj.append("exists_comments", 0);
//                    }else{
//                        //System.out.println("Comments exist");
//                        tempObj.append("exists_comments", 1);
//                        existsComments = true;
//                    }
//                }
//            }
            
            // Other INFO
            Elements otherEl = doc.select("dl.tabular.dt145");
            for(Element el: otherEl){
                
                if(el.select("dt").text().equals("Client Install Required")){
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("client_install_required", "");
                    }else{
                        //System.out.println("Client Install Required: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("client_install_required", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Service Endpoint")){
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("service_endpoint", "");
                    }else{
                        //System.out.println("Service Endpoint: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("service_endpoint", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Signup Requirements")){
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("signup_requirements", "");
                    }else{
                        //System.out.println("Signup Requirements: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("signup_requirements", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Developer Key Required")){
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("developer_key_required", "");
                    }else{
                        //System.out.println("Developer Key Required: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("developer_key_required", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                if(el.select("dt").text().equals("Account Required")){
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("account_required", "");
                    }else{
                        //System.out.println("Account Required: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("account_required", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                if(el.select("dt").text().equals("Commercial Licensing")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("commercial_licensing", "");
                    }else{
                        //System.out.println("Commercial Licensing: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("commercial_licensing", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                if(el.select("dt").text().equals("Provider")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("provider", "");
                    }else{
                        //System.out.println("Provider: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("provider", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                if(el.select("dt").text().equals("Company")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("company", "");
                    }else{
                        //System.out.println("Company: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("company", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Non-Commercial Lic.")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("non_commercial_lic", "");
                    }else{
                        //System.out.println("Non-Commercial Lic.: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("non_commercial_lic", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                if(el.select("dt").text().equals("Data Licensing")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("data_licensing", "");
                    }else{
                        //System.out.println("Data Licensing: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("data_licensing", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                if(el.select("dt").text().equals("Usage Fees")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("usage_fees", "");
                    }else{
                        //System.out.println("Usage Fees: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("usage_fees", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                if(el.select("dt").text().equals("Program Fees")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("program_fees", "");
                    }else{
                        //System.out.println("Program Fees: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("program_fees", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                if(el.select("dt").text().equals("Certification Program")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("certification_program", "");
                    }else{
                        //System.out.println("Certification Program: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("certification_program", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Usage Limits")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("usage_limits", "");
                    }else{
                        //System.out.println("Usage Limits: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("usage_limits", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Terms of Service")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("terms_of_service", "");
                    }else{
                        //System.out.println("Terms of Service: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("terms_of_service", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Authentication Model")){
                    tempList = new ArrayList();
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("authentication_model", tempList);
                    }else{
                        //System.out.println("Authentication Model: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        for(Element el2: el.select("dd a")){
                            tempList.add(el2.text());
                        }
                        tempObj.append("authentication_model", tempList);
                        //tempObj.append("authentication_model", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("SSL Support")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("ssl_support", "");
                    }else{
                        //System.out.println("SSL Support: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("ssl_support", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Read-only Without Login")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("read_only_without_login", "");
                    }else{
                        //System.out.println("Read-only Without Login: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("read_only_without_login", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Vendor API Kits")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("vendor_api_kits", "");
                    }else{
                        //System.out.println("Vendor API Kits: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("vendor_api_kits", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Community API Kits")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("community_api_kits", "");
                    }else{
                        //System.out.println("Community API Kits: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("community_api_kits", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("API Blog")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("api_blog", "");
                    }else{
                        //System.out.println("API Blog: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("api_blog", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Site Blog")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("site_blog", "");
                    }else{
                        //System.out.println("Site Blog: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("site_blog", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("API Forum")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("api_forum", "");
                    }else{
                        //System.out.println("API Forum: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("api_forum", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Developer Support")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("developer_support", "");
                    }else{
                        //System.out.println("Developer Support: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("developer_support", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
                
                if(el.select("dt").text().equals("Console URL")){
                    
                    if(el.select("dd").text().replace(String.valueOf((char) 160), " ").trim().isEmpty()){
                        tempObj.append("console_url", "");
                    }else{
                        //System.out.println("Console URL: " + el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                        tempObj.append("console_url", el.select("dd").text().replace(String.valueOf((char) 160), " ").trim());
                    }
                }
            }            
            
            Calendar c = new GregorianCalendar();
            c.set(Integer.parseInt(tempDate.substring(0,4)), Integer.parseInt(tempDate.substring(5,7))-1, Integer.parseInt(tempDate.substring(8,10)));
            
            tempObj.append("date_updated", c.getTime());

            // upsert
            MongoDBClient.getInstance().getCollection("apis").update(new BasicDBObject().append("title", api_title.text()), tempObj, true, false);
            
//            if(existsComments){
//                List<String> row = new ArrayList<String>();
//                
//                row.add("");
//                // Comments URL
//                row.add(url + "/comments");
//                // Mashup id
//                row.add(tempObj.get("_id").toString());                
//                // type
//                row.add("comments");
//
////                fetchList.add(row);
//                // extractComments(url + "/comments", snapshotId, tempObj.get("_id")); // extract comments, if there exist
//            }
//            existsComments = false;
            
        } catch (IOException ex) {
            Logger.getLogger(PWFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }    
    }

    public boolean fetchMember(String url, Object snapshotId){
        tempObj = new BasicDBObject();
        tempObj.append("snapshot_id", snapshotId);
        tempObj.append("pw_url", url);
        System.out.println("Fetching member: " + url);
        
        try {
            Connection conn = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip,deflate,sdch")
                    .header("Accept-Language", "en-US,en;q=0.8")
                    .header("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3")
                    .timeout(30000);

            Document doc = conn.get();
            
            //Member title
            Elements member_username = doc.select("table.refGrid").select("tr");
            String username = "";
            for(Element el: member_username){                
                
                if(el.select("td").first() != null){
                    
                    // username
                    if(el.select("td").first().text().equals("Username")){
                        //System.out.println("Username: " + el.select("td").last().text());
                        username = el.select("td").last().text().replace(String.valueOf((char) 160), " ").trim();
                        tempObj.append("username", username);
                    }

                    // Users realname - given and family name
                    if(el.select("td").first().text().equals("Real Name")){
                        //System.out.println("Given name: " + el.select("td").last().select("span.given-name").text());                    
                        //System.out.println("Family name: " + el.select("td").last().select("span.family-name").text());                    
                        tempObj.append("given_name", el.select("td").last().select("span.given-name").text());
                        tempObj.append("family_name", el.select("td").last().select("span.family-name").text());
                    }
                    
                    // Location - usually city
                    if(el.select("td").first().text().equals("Location")){
                        //System.out.println("Location: " + el.select("td").last().text());
                        tempObj.append("city", el.select("td").last().text());
                    }
                    
                    // Users country
                    if(el.select("td").first().text().equals("Country")){
                        //System.out.println("Country: " + el.select("td").last().text());
                        tempObj.append("country", el.select("td").last().text());
                    }
                    
                    // Users gender
                    if(el.select("td").first().text().equals("Gender")){
                        //System.out.println("Gender: " + el.select("td").last().text());
                        tempObj.append("gender", el.select("td").last().text());
                    }
                    
                    // Users web site
                    if(el.select("td").first().text().equals("Web Site")){
                        //System.out.println("Web Site: " + el.select("td").last().select("a").attr("href"));
                        tempObj.append("web_site", el.select("td").last().select("a").attr("href"));
                    }
                    
                    // About me
                    if(el.select("td").first().text().equals("About Me")){
                        //System.out.println("About Me: " + el.select("td").last().text());
                        tempObj.append("about", el.select("td").last().text());
                    }
                }
            }
            
            // Created mashups
            Elements mash_elems = doc.select("table.refGrid > tbody > tr > td > a");
            tempList = new ArrayList();
            for(Element el : mash_elems){
                if(el.attr("href").substring(1,7).equals("mashup")){
                    tempList.add("http://www.programmableweb.com" + el.attr("href"));
                    //System.out.println("mashup title: "+el.text());
                    //System.out.println("mashup url: "+el.attr("href"));
                }
            }
            tempObj.append("mashups", tempList);
            
            // Friends
//            tempList = new ArrayList();
//            for(Element el : member_username){
//                if(el.select("th").text().equals("Friends")){
//                    for(Element el2: el.nextElementSibling().select("a")){
//                        if(!el2.text().equals("")){
//                            tempList.add("http://www.programmableweb.com" + el2.attr("href"));
//                            //System.out.println("friend username: " + el2.text());
//                            //System.out.println("friend url: " + el2.attr("href"));
//                            
//                            BasicDBObject queryObj = new BasicDBObject();
//                            queryObj.put("snapshot_id", snapshotId);
//                            queryObj.put("username", el.select("a").text());
//
//                            DBCursor cursor = MongoDBClient.getInstance().getCollection("users").find(queryObj);
//
//                            // If the friend is not in DB then add it to the queue
//                            if(cursor.size()==0){
//                                // User is added in the queue.
//                                List<String> row = new ArrayList<String>();
//
//                                // User title
//                                row.add("");
//                                // User URL
//                                row.add("http://www.programmableweb.com" + el2.attr("href"));
//                                // updated
//                                row.add("");
//                                // type
//                                row.add("user");
//
//                                fetchList.add(row);
//                            }
//                        }
//                    }
//                }
//            }
//            tempObj.append("friends", tempList);
            
            // Date of registration
            for(Element el : member_username.select("tr")){
                if(el.select("td").first()!=null){
                    if(el.select("td").first().text().equals("Registered")){
                        //System.out.println("date registerd: " + el.select("td").last().text());
                        String tempDate = el.select("td").last().text();
                        Calendar c = new GregorianCalendar();
                        c.set(Integer.parseInt(tempDate.substring(0,4)), Integer.parseInt(tempDate.substring(5,7)), Integer.parseInt(tempDate.substring(8,10)), 0, 0, 0);
                        tempObj.append("registration_date", c.getTime());
                    }
                }
            }
            
            MongoDBClient.getInstance().getCollection("users").update(new BasicDBObject().append("username",username),tempObj, true, false);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(PWFetcher.class.getName()).log(Level.SEVERE, "problem", ex);
            return false;
        }
    }
}