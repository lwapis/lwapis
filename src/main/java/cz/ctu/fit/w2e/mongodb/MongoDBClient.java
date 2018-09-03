/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.ctu.fit.w2e.mongodb;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Milan Dojchinovski
 * <milan (at) dojchinovski (dot) mk>
 * Twitter: @m1ci
 * www: http://dojchinovski.mk
 */
public class MongoDBClient {
        
    private static MongoClient mongoClient = null;
    private static DB db = null;
            
    public static DB getInstance(){
        if(db == null){
            init();
            db = mongoClient.getDB( "linkeddb2" );
        }
        return db;
    }
        
    public static void init(){
        try {
            mongoClient = new MongoClient( "localhost" , 27017 );
        } catch (UnknownHostException ex) {
            Logger.getLogger(MongoDBClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}