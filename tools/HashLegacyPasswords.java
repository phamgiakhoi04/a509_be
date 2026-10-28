import java.sql.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Explicit, local-only data migration. Never runs as part of application startup. */
public class HashLegacyPasswords {
 public static void main(String[] args)throws Exception{
  String url=System.getenv("SPRING_DATASOURCE_URL");
  if(url==null||!url.matches("jdbc:mysql://localhost:3306/a509_db(?:\\?.*)?"))throw new IllegalStateException("Local a509_db only");
  boolean apply=args.length==2&&args[0].equals("--apply");
  if(apply&&(!Files.isRegularFile(Path.of(args[1]))||Files.size(Path.of(args[1]))<1000))throw new IllegalStateException("A fresh mysqldump backup is required");
  try(var db=DriverManager.getConnection(url,System.getenv("SPRING_DATASOURCE_USERNAME"),System.getenv("SPRING_DATASOURCE_PASSWORD"))){
   db.setAutoCommit(false);
   try{
    var ids=new ArrayList<Long>();var passwords=new ArrayList<String>();
    try(var s=db.createStatement();var r=s.executeQuery("select user_id,password_hash from users where password_hash not like '$2a$%' and password_hash not like '$2b$%' and password_hash not like '$2y$%' for update")){
     while(r.next()){ids.add(r.getLong(1));passwords.add(r.getString(2));}
    }
    System.out.println("Legacy credentials found: "+ids.size());
    if(!apply){db.rollback();System.out.println("Dry run only. No data changed.");return;}
    var encoder=new BCryptPasswordEncoder();
    for(int i=0;i<ids.size();i++){
     String original=passwords.get(i);
     if(original.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new IllegalStateException("Password exceeds BCrypt's byte limit; use password reset instead");
     String encoded=encoder.encode(original);if(!encoder.matches(original,encoded))throw new IllegalStateException("Hash verification failed");
     try(var update=db.prepareStatement("update users set password_hash=? where user_id=? and password_hash=?")){
      update.setString(1,encoded);update.setLong(2,ids.get(i));update.setString(3,original);if(update.executeUpdate()!=1)throw new IllegalStateException("Concurrent credential change");
     }
    }
    db.commit();System.out.println("Committed "+ids.size()+" BCrypt upgrades. Password text and active session versions are unchanged; no credentials printed.");
   }catch(Exception ex){db.rollback();throw ex;}
  }
 }
}
