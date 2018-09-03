/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.ctu.fit.w2e.pwfetcher;

import java.util.Timer;
import org.bson.types.ObjectId;

/**
 * @author Milan Dojchinovski
 * <milan (at) dojchinovski (dot) mk>
 * Twitter: @m1ci
 * www: http://dojchinovski.mk
 */
public class Fetcher {
    
    public static void main(String args[]){
    
        long start = System.currentTimeMillis();
        //try {
            //fetchAPI("http://www.programmableweb.com/api/google-maps");
            //fetchAPIList();
            //fetchAPIPageList(1);
        
            //fetchMashup("http://www.programmableweb.com/mashup/compare-prices.info");//
            //fetchMashup("http://www.programmableweb.com/mashup/10-camera");
            //fetchMashupsList();
            //fetchUsersList();
        
            //fetchMember("http://www.programmableweb.com/profile/m1ci");
            //fetchMember("http://www.programmableweb.com/profile/bibirmer");
            //PWFetcher.getInstance().fetchMember("http://www.programmableweb.com/profile/duvander");
            //PWFetcher.getInstance().fetchAPIComments("http://www.programmableweb.com/api/twitter/comments");
            //PWFetcher.getInstance().fetchAPIComments("http://www.programmableweb.com/api/google-maps/comments");
            //PWFetcher.getInstance().fetchAPIComments("http://www.programmableweb.com/api/box.net/comments");
            //PWFetcher.getInstance().fetchAPIComments("http://www.programmableweb.com/api/transit-and-trails/comments");
            //PWFetcher.getInstance().fetchAPI("http://www.programmableweb.com/api/twitter", new Object(), "2012-12-17");
            //PWFetcher.getInstance().fetchAPI("http://www.programmableweb.com/api/aculab-cloud", new Object(), "2012-12-17");
        
            // aditional fetch
//            PWFetcher.getInstance().fetchAPI("http://www.programmableweb.com/api/eviscape", "", "2009-03-26");
        
            Timer timer = new Timer();        
            FetchTask st = new FetchTask(timer);
            timer.schedule(st, 0, 4000);
        
            //MongoDBClient client = MongoDBClient.getInstance();
            //client.save();
        
            //long elapsedTime = System.currentTimeMillis() - start;
            //System.out.println("member fetched in: " + elapsedTime/1000F + "s ");

            /*
            String text = "The Charles Bridge is a famous historic bridge that crosses the Vltava river in Prague, Czech Republic.";
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5hash = new byte[32];
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            md5hash = md.digest();
            System.out.println(md5hash.toString());
            System.out.println(md5hash.hashCode());
            
            String hex = (new HexBinaryAdapter()).marshal(md.digest(text.getBytes()));
            System.out.println(hex);
            */

            
        //} catch (UnsupportedEncodingException ex) {
        //    Logger.getLogger(PWFetcher.class.getName()).log(Level.SEVERE, null, ex);
        //}catch (NoSuchAlgorithmException ex) {
        //    Logger.getLogger(PWFetcher.class.getName()).log(Level.SEVERE, null, ex);
        //}
    }
}
