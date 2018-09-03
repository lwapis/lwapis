/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.ctu.fit.w2e.pwfetcher;

import com.mongodb.BasicDBObject;
import cz.ctu.fit.w2e.mongodb.MongoDBClient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.bson.types.ObjectId;

/**
 * @author Milan Dojchinovski
 * <milan (at) dojchinovski (dot) mk>
 * Twitter: @m1ci
 * www: http://dojchinovski.mk
 */
public class FetchTask extends TimerTask{

    private Timer timer;
    private boolean initialized = false;
    private boolean previousTaskFinished = true;
    private BasicDBObject tempDoc = null;
    private List<List<String>> fetchList;
    private Object snapshotId = null; // current snapshot id    
    
    public FetchTask(Timer timer){
        this.timer = timer;
    }

    public void run() {
        
        if(previousTaskFinished){
            
            if(!initialized){
        
                init();
                
            } else if(fetchList.size()>0){
                
                previousTaskFinished = false;
                
                if (fetchList.get(0).get(3).toString().equals("api")) {
                    PWFetcher.getInstance().fetchAPI(fetchList.get(0).get(1), snapshotId, fetchList.get(0).get(2));
                    fetchList.remove(0);
                    
                } else if (fetchList.get(0).get(3).toString().equals("mashup")) {  
                    
                    PWFetcher.getInstance().fetchMashup(fetchList.get(0).get(1), snapshotId);
                    fetchList.remove(0);
                    
                } else if (fetchList.get(0).get(3).toString().equals("user")) {
                    
                    boolean check = PWFetcher.getInstance().fetchMember(fetchList.get(0).get(1), snapshotId);                
                    System.out.println("left: " + fetchList.size());
                    if(check){
                        fetchList.remove(0);                    
                    }else{
                    
                    }
                }
                // uncommend to fetch also comments
//                } else if (fetchList.get(0).get(3).toString().equals("comments")) {
//                    
////                    PWFetcher.getInstance().extractComments(fetchList.get(0).get(1), snapshotId, fetchList.get(0).get(2));                
//                    fetchList.remove(0);
//                }
                
                previousTaskFinished = true;

            }else{
                System.out.println("Finishing...");
                BasicDBObject newDocument = new BasicDBObject().append("$set", new BasicDBObject().append("dateFinished", new Date()));
                MongoDBClient.getInstance().getCollection("snapshots").update(new BasicDBObject().append("_id",snapshotId), newDocument);
                timer.cancel();
            }
        } else {
            System.out.println("Waiting for previos task to finish...");
        }
    }
        
    private void init(){
        System.out.println("Starting creating snapshot on " + new Date());
        
        previousTaskFinished = false;
        
        // Creating new snapshot in database.
        tempDoc = new BasicDBObject();
        tempDoc.append("dateStarted", new Date());
        tempDoc.append("completed", 0);
        MongoDBClient.getInstance().getCollection("snapshots").save(tempDoc);
        snapshotId = tempDoc.get("_id");

        System.out.println("Initializing fetcher...");
        fetchList = PWFetcher.getInstance().getFetchList(snapshotId);

        /*
        List<String> row = new ArrayList<String>();
        row.add("Google Maps");
        row.add("http://www.programmableweb.com/api/google-maps");
        row.add("2012-12-17");
        row.add("api");
        fetchList.add(row);        
        */
        
        //System.out.println("FINAL size: " + fetchList.size());
                       
        initialized = true;
        previousTaskFinished = true;
        System.out.println("Fetcher was initialized!");

    }
}
